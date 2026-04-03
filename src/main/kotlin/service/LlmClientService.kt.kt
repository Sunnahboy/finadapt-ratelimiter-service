package service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.io.IOException
import kotlinx.serialization.Serializable

@Serializable data class LlmMessage(val role: String, val content: String)
@Serializable data class LlmRequest(val model: String, val messages: List<LlmMessage>)
@Serializable data class LlmChoice(val message: LlmMessage)
@Serializable data class LlmResponse(val choices: List<LlmChoice>)


class LlmClientService (
    private val httpClient: HttpClient,
    private val apiKey: String,
    private val baseUrl: String,
    private val model: String
){
    //Talks to the API endpoint
    suspend fun askAi(userPrompt: String): Result<String> {
        return try {
            val response: LlmResponse = httpClient.post(baseUrl){
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            contentType(ContentType.Application.Json)
            //JSON body
            setBody(
                LlmRequest(
                    model = model,
                    messages = listOf (LlmMessage(role = "user", content = userPrompt))
                )
            )
        }.body()
        val aiText = response.choices.firstOrNull()?.message?.content//extract actual text
        if (aiText != null){
            Result.success(aiText)
        } else {
            Result.failure(Exception("Groq API returned an empty response."))
        }

       } catch (e: ClientRequestException) {
            //Catches 4xx errors
            Result.failure(Exception("Client Error (${e.response.status}): Check API key or request format."))
        } catch (e: ServerResponseException) {
            //Catches 5xx errors
            Result.failure(Exception("Provider Error (${e.response.status}): The AI service is currently unavailable."))
        } catch (e: IOException) {
            //Catches network failures (timeout, DNS resolution)
            Result.failure(Exception("Network Error: Failed to connect to AI provider."))
        } catch (e: Exception) {
            //Fallback for everything
            Result.failure(Exception("An unexpected error occurred: ${e.localizedMessage}"))
        }


    }
}