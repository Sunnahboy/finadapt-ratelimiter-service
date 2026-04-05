package routing

import service.AppFeature
import service.RateLimiterService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import service.LlmClientService
import kotlin.collections.mapOf

//define what the android app will send
@Serializable
data class ChatRequest(val prompt: String)

fun Application.chatRoutes(rateLimiterService: RateLimiterService, llmClientService: LlmClientService) {

    routing {

        //The Chat Domain
        withRateLimit("/api/chat", AppFeature.CHAT, rateLimiterService) {
            post {
                //read the incoming JSON request from Android
                val request = try{
                    call.receive<ChatRequest>()
                }catch(_:Exception){
                    call.respond(
                        HttpStatusCode.BadRequest,
                        mapOf("error" to "Invalid format. Expected JSON like { \"prompt\": \"your question\" }")
                    )
                    return@post
                }
                //Ask using the dynamic prompt  grabbed
                val result = llmClientService.askAi(request.prompt)

                //safely unwrap and send back the result
                result.fold(
                    onSuccess = {aiResponse->
                        call.respond(HttpStatusCode.OK, mapOf("message" to aiResponse))
                    },
                    onFailure = {error ->
                        call.respond(
                            HttpStatusCode.BadGateway,
                            mapOf("error" to (error.message ?: "Failed to contact AI service."))
                        )
                    }
                )

            }
        }
        // Receipt domain
        withRateLimit("/chat", AppFeature.CHAT, rateLimiterService) {
            post {
                call.respond(HttpStatusCode.OK, mapOf("message" to "Receipt scanner endpoint ready!"))
            }
        }

    }
}