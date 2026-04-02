package routing

import service.AppFeature
import service.RateLimiterService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.chatRoutes(rateLimiterService: RateLimiterService) {

    routing {

        //The Chat Domain
        withRateLimit(AppFeature.CHAT, rateLimiterService) {
            post("/api/chat") {
                //already checked the ID and the Tokens.
                //safe to hit the API
                call.respond(HttpStatusCode.OK, mapOf("message" to "Groq AI response goes here!"))
            }

            //more chat-related endpoints here

        }

        //The Receipt Domain
        withRateLimit(AppFeature.RECEIPT, rateLimiterService) {
            post("/api/receipt") {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Receipt parsed successfully!"))
            }
        }

    }
}