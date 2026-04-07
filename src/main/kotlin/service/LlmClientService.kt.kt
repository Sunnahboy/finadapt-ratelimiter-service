package service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.io.IOException
import kotlinx.serialization.Serializable
import java.util.concurrent.atomic.AtomicInteger //REQUIRED FOR ROUND ROBIN

@Serializable data class LlmMessage(val role: String, val content: String)
@Serializable data class ResponseFormat(val type: String)
@Serializable data class LlmRequest(val model: String, val responseFormat: ResponseFormat, val messages: List<LlmMessage>)
@Serializable data class LlmChoice(val message: LlmMessage)
@Serializable data class LlmResponse(val choices: List<LlmChoice>)

class LlmClientService(
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String,
    private val primaryChatModel: String
) {
    private val chatModels = listOf(primaryChatModel, "llama-3.1-8b-instant", "mixtral-8x7b-32768").distinct()

    private val receiptModels = listOf(
        "llama-3.3-70b-versatile",
        "llama-3.1-8b-instant",
        "mixtral-8x7b-32768",
        "gemma2-9b-it"
    )

    //THREAD-SAFE COUNTERS (One for each pipeline to avoid cross-contamination)
    private val chatCounter = AtomicInteger(0)
    private val receiptCounter = AtomicInteger(0)

    // ==========================================
    // CHAT: True Round Robin
    // ==========================================
    suspend fun askChatAi(systemPrompt: String, userPrompt: String): Result<String> {
        val nextIndex = chatCounter.getAndIncrement()
        return executeWithRetry(chatModels, nextIndex, systemPrompt, userPrompt)
    }

    // ==========================================
    // RECEIPT: True Round Robin
    // ==========================================
    suspend fun parseReceiptAi(systemPrompt: String, userPrompt: String): Result<String> {
        val nextIndex = receiptCounter.getAndIncrement()
        return executeWithRetry(receiptModels, nextIndex, systemPrompt, userPrompt)
    }

    // ==========================================
    //THE CORE ENGINE
    // ==========================================
    private suspend fun executeWithRetry(
        modelList: List<String>,
        startIndex: Int,
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        var lastException: Exception? = null
        val totalModels = modelList.size

        for (i in 0 until totalModels) {
            //Modulo math ensures we loop around the list safely without going out of bounds
            val modelToTry = modelList[(startIndex + i) % totalModels]

            if (i > 0) delay(500L * i)

            try {
                val response = httpClient.post(baseUrl) {
                    header(HttpHeaders.Authorization, "Bearer $apiKey")
                    contentType(ContentType.Application.Json)
                    setBody(
                        LlmRequest(
                            model = modelToTry,
                            responseFormat = ResponseFormat(type = "json_object"),
                            messages = listOf(
                                LlmMessage(role = "system", content = systemPrompt),
                                LlmMessage(role = "user", content = userPrompt)
                            )
                        )
                    )
                }

                if (response.status.isSuccess()) {
                    val aiText = response.body<LlmResponse>().choices.firstOrNull()?.message?.content
                    if (aiText != null) return Result.success(aiText)
                    throw Exception("API returned empty text.")
                } else {
                    val errorBody = response.bodyAsText()
                    if (response.status.value == 401) {
                        return Result.failure(Exception("401 Unauthorized: Check API Key."))
                    }
                    throw Exception("API Error ${response.status}: $errorBody")
                }

            } catch (e: ClientRequestException) {
                lastException = Exception("Client Error (${e.response.status}).")
            } catch (e: ServerResponseException) {
                lastException = Exception("Provider Error (${e.response.status}).")
            } catch (e: IOException) {
                lastException = Exception("Network Error connecting to Groq.")
            } catch (e: Exception) {
                lastException = e
            }
        }

        return Result.failure(Exception("All fallback models failed. Last Error: ${lastException?.message}"))
    }
}