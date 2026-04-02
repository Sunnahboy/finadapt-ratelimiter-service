import io.ktor.server.application.*
import io.ktor.server.netty.*
import org.redisson.Redisson
import org.redisson.client.codec.StringCodec
import org.redisson.config.Config
import service.RateLimiterService

//Use EngineMain instead of embeddedServer so Ktor automatically reads application.yaml
fun main(args: Array<String>): Unit = EngineMain.main(args)

fun Application.module() {

    //Pull from application.yaml (with a fallback)
    val redisUrl = environment.config.propertyOrNull("redis.url")?.getString()
        ?: "redis://127.0.0.1:6379"

    //Initialize Redis
    val config = Config()
    config.useSingleServer().address = redisUrl

    //Tell Redisson not to use Jackson. Use Strings/Bytes instead (faster and uses less RAM).
    config.codec = StringCodec.INSTANCE
    val redissonClient = Redisson.create(config)

    //Zombie Connection (Resource Leak) Hook into the shutdown event
    monitor.subscribe(ApplicationStopped) {
        log.info("Shutting down Redis connections...")
        redissonClient.shutdown()
    }

    //Create the rate limiter
    val rateLimiterService = RateLimiterService(redissonClient)

    // Dependency Availability - Inject into routing block
     chatRoutes(rateLimiterService)
}