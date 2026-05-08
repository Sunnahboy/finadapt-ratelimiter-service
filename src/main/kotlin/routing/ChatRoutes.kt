package routing

import service.Prompts
import service.AppFeature
import service.RateLimiterService
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import service.LlmClientService

@Serializable
data class ChatRequest(val prompt: String)

@Serializable
data class ReceiptRequest(@SerialName("raw_text") val rawText: String)

fun Application.chatRoutes(rateLimiterService: RateLimiterService, llmClientService: LlmClientService) {
    routing {
        //Chat Domain
        withRateLimit("/api/chat", AppFeature.CHAT, rateLimiterService) {
            post {
                val request = try { call.receive<ChatRequest>() }
                catch(_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid format."))
                    return@post
                }

                val result = llmClientService.askChatAi(Prompts.CHAT_SYSTEM_PROMPT, request.prompt)

                result.fold(
                    onSuccess = { call.respond(HttpStatusCode.OK, mapOf("message" to it)) },
                    onFailure = { call.respond(HttpStatusCode.BadGateway, mapOf("error" to (it.message ?: "Failed"))) }
                )
            }
        }

        //Receipt Domain
        withRateLimit("/api/receipt", AppFeature.RECEIPT, rateLimiterService) {
            post {
                val request = try { call.receive<ReceiptRequest>() }
                catch(_: Exception) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "Invalid format."))
                    return@post
                }

                val result = llmClientService.parseReceiptAi(Prompts.RECEIPT_SYSTEM_PROMPT, "RECEIPT TEXT:\n${request.rawText}")

                result.fold(
                    onSuccess = { call.respondText(it, io.ktor.http.ContentType.Application.Json, HttpStatusCode.OK) },
                    onFailure = { call.respond(HttpStatusCode.BadGateway, mapOf("error" to (it.message ?: "Failed"))) }
                )
            }
        }
    }
}