import org.redisson.api.RateIntervalUnit
import org.redisson.api.RateType
import org.redisson.api.RedissonClient

class RateLimiterService(private val redisson: RedissonClient) {

    fun canUserChat(userId: String): Boolean {
        //bucket name
        val bucketName = "rate_limit:groq:$userId"
        val rateLimiter = redisson.getRateLimiter(bucketName)

        // 2. Initialize the Token Bucket rules (only runs once per user)
        // RateType.OVERALL = Limit applies across all servers for multiple Ktor instances
        // 5 = Burst capacity (Maximum tokens the bucket can hold)
        // 3 = Refill rate (Adds 3 tokens...)
        // 1 MINUTES = ...every 1 minute
        rateLimiter.trySetRate(RateType.OVERALL, 5, 3, RateIntervalUnit.MINUTES)

        // 3. Try to consume 1 token. Returns true if successful, false if the bucket is empty!
        return rateLimiter.tryAcquire(1)
    }
}