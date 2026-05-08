package routing

import service.AppFeature
import service.RateLimiterService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

//Define the Plugin Configuration
class RateLimitConfig {
    lateinit var feature: AppFeature
    lateinit var rateLimiter: RateLimiterService
}

//Create the Route Scoped Plugin
val RateLimitPlugin = createRouteScopedPlugin(
    name = "RateLimitPlugin",
    createConfiguration = ::RateLimitConfig
) {
    //Read the config passed in during installation
    val feature = pluginConfig.feature
    val rateLimiter = pluginConfig.rateLimiter

    //onCall runs automatically before your route's business logic
    onCall { call ->
        val userId = call.request.headers["X-User-Id"]

        //Check Auth
        if (userId.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Missing X-User-Id header"))
            return@onCall //Halts execution.
        }

        //Check Tokens
        if (!rateLimiter.tryAcquire(userId, feature)) {
            call.respond(
                HttpStatusCode.TooManyRequests,
                mapOf("error" to "Rate limit exceeded for ${feature.id}. Please wait.")
            )
            return@onCall //Halts execution.
        }
    }
}

//DSL wrapper so ChatRoutes.kt doesn't have to change
fun Route.withRateLimit(
    path: String,
    feature: AppFeature,
    rateLimiter: RateLimiterService,
    build: Route.() -> Unit
) {
    route(path) {
        install(RateLimitPlugin) {
            this.feature = feature
            this.rateLimiter = rateLimiter
        }
        build()
    }
}