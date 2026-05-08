package service

import org.redisson.api.RateIntervalUnit
import org.redisson.api.RateType
import org.redisson.api.RedissonClient
//from RateLimitService.kt
enum class AppFeature(
    val id: String,
    val capacity: Long,
    val refillTokens: Long,
    val refillUnit: RateIntervalUnit
) {
    // Note: TTL removed from Enum since Redisson handles state-key memory natively
    CHAT(id = "chat", capacity = 15, refillTokens = 5, refillUnit = RateIntervalUnit.MINUTES),
    RECEIPT(id = "receipt", capacity = 5, refillTokens = 2, refillUnit = RateIntervalUnit.MINUTES)
}

class RateLimiterService(private val redisson: RedissonClient) {

    fun tryAcquire(userId: String, feature: AppFeature): Boolean {
        val bucketName = "rate_limit:llm:${feature.id}:$userId"
        val rateLimiter = redisson.getRateLimiter(bucketName)

        //trySetRate safely ignores the command if the rate is already set in Redis.
        //guarantees the limiter always exists.
        rateLimiter.trySetRate(
            RateType.OVERALL,
            feature.capacity,
            feature.refillTokens,
            feature.refillUnit
        )
        // Try to consume 1 token. Returns true if allowed, false if rate limited.
        return rateLimiter.tryAcquire(1)
    }
}