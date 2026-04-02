package com.finadapt.backend

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.redisson.Redisson
import org.redisson.RedissonRx
import org.redisson.api.RateLimiterConfig
import org.redisson.config.Config
import org.redisson.client.codec.StringCodec

fun main(){
    embeddedServer(Netty, 8080, host = "0.0.0.0", module = Application::module
    ).start(wait = true)
}

fun Application.module() {
    //1. initialize Redis (Batch 1)
    val config = Config()
    config.useSingleServer().address = "redis://127.0.0.1:6379"
    //Tell Redisson not to use Jackson use Strings/Bytes instead
    // faster and uses less RAM.
    config.codec = StringCodec.INSTANCE
    val redissonClient = Redisson.create(config)

    //2. create the rate limiter
    val rateLimiterConfig = RateLimiterService(redissonClient)
    //3 start the routes (batch 3)

}
