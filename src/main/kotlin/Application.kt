import routing.chatRoutes
import service.RateLimiterService
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import kotlinx.serialization.json.Json
import org.redisson.Redisson
import org.redisson.client.codec.StringCodec
import org.redisson.config.Config
import service.LlmClientService

//EngineMain automatically reads yaml
fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {
    configureSerialization()//formats into JSON string

    //Load Secrets from yaml file
    val redisUrl = environment.config.property("redis.url").getString()
    val llmApiKey = environment.config.property("llm.apiKey").getString()
    val llmBaseUrl = environment.config.property("llm.baseUrl").getString()
    val llmModel = environment.config.property("llm.model").getString()


    //Initialize Redis
    val config = Config()
    config.useSingleServer().address = redisUrl
    config.codec = StringCodec.INSTANCE//Use Strings/Bytes (faster and uses less RAM)
    val redissonClient = Redisson.create(config)

    //connect into the shutdown event
    monitor.subscribe(ApplicationStopped) {
        log.info("Shutting down Redis connections...")
        redissonClient.shutdown()
    }

    //Initialize the Ktor Http client
    val httpClient = HttpClient(CIO){
        install(plugin = ContentNegotiation){
            json(Json { ignoreUnknownKeys = true })
        }
    }

    //Create the Services
    val rateLimiterService = RateLimiterService(redissonClient)
    val llmClientService = LlmClientService(httpClient, llmApiKey, llmBaseUrl, llmModel)

    //start the routes
    chatRoutes(rateLimiterService, llmClientService)
}