package routing

import service.AppFeature
import service.RateLimiterService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import service.LlmClientService


fun Application.chatRoutes(rateLimiterService: RateLimiterService, llmClientService: LlmClientService) {

    routing {

        //The Chat Domain
        withRateLimit("/api/chat", AppFeature.CHAT, rateLimiterService) {
            post {
                //safe to hit the API
                call.respond(HttpStatusCode.OK, mapOf("message" to "LLM  response goes here!"))
            }
            //related endpoints here
        }
        //The Receipt Domain
        withRateLimit("/api/receipt", AppFeature.RECEIPT, rateLimiterService) {
            post {
                val userMessage = "Explain compound interest simply." //just test the connection

                //Ask safely from the server
                val result = llmClientService.askAi(userMessage)
                //safely unwarp
                result.fold(
                    onSuccess = {aiResponse->
                        call.respond(HttpStatusCode.OK, mapOf("message" to aiResponse))
                    },
                    onFailure = { error ->
                        call.respond(
                            HttpStatusCode.BadGateway,
                            mapOf("error" to (error.message ?: "Failed to contact AI service."))
                        )
                    }
                )
            }

        }

    }
}