package service

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.*
import io.ktor.client.statement.*
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
            val response = httpClient.post(baseUrl){
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                //JSON body
                setBody(
                    LlmRequest(
                        model = model,
                        messages = listOf (LlmMessage(role = "user", content = userPrompt))
                    )
                )
            }
            //SAFELY CHECK THE STATUS FIRST
            if (response.status.isSuccess()) {
                val llmResponse: LlmResponse = response.body()
                val aiText = llmResponse.choices.firstOrNull()?.message?.content

                if (aiText != null){
                    Result.success(aiText)
                } else {
                    Result.failure(Exception("API returned an empty response."))
                }
            } else {
                //If  returns 401, 404, etc., we grab the exact error message!
                val errorBody = response.bodyAsText()
                Result.failure(Exception("API Rejected Request (${response.status}): $errorBody"))
            }

       } catch (e: ClientRequestException) {
            Result.failure(Exception("Client Error (${e.response.status}): Check API key or request format."))
        } catch (e: ServerResponseException) {
            Result.failure(Exception("Provider Error (${e.response.status}): The AI service is currently unavailable."))
        } catch (_: IOException) {
            Result.failure(Exception("Network Error: Failed to connect to AI provider."))
        } catch (e: Exception) {
            Result.failure(Exception("An unexpected error occurred: ${e.localizedMessage}"))
        }
    }
}