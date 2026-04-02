package service
import org.redisson.api.RateIntervalUnit
import org.redisson.api.RateType
import org.redisson.api.RedissonClient
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap


enum class AppFeature(
    val id: String,
    val capacity: Long,
    val refillTokens: Long,
    val refillUnit: RateIntervalUnit,
    val ttlMinutes: Long
) {
    CHAT(id = "chat", capacity = 15, refillTokens = 5, refillUnit = RateIntervalUnit.MINUTES, ttlMinutes = 60),
    RECEIPT(id = "receipt", capacity = 5, refillTokens = 2, refillUnit = RateIntervalUnit.MINUTES, ttlMinutes = 60)
}

class RateLimiterService(private val redisson: RedissonClient) {

    // 2. LOCAL CACHE: Remembers which buckets this server has already initialized.
    //remove "Redundant Network Calls" issue.
    private val initializedBuckets = ConcurrentHashMap.newKeySet<String>()

    fun tryAcquire(userId: String, feature: AppFeature): Boolean {
        // Creates unique keys like
        val bucketName = "rate_limit:groq:${feature.id}:$userId"
        val rateLimiter = redisson.getRateLimiter(bucketName)

        // 3. CACHE CHECK: .add() returns true ONLY if the item wasn't already in the set.
        if (initializedBuckets.add(bucketName)) {
            //double check redis in case restarted but ktor didn't
            if (!rateLimiter.isExists) {
                rateLimiter.trySetRate(
                    RateType.OVERALL,
                    feature.capacity,
                    feature.refillTokens,
                    feature.refillUnit
                )

                //if the user doesn't use this feature for 60 minutes,
                //Redis will automatically delete the bucket to free up RAM.
                rateLimiter.expire(Duration.ofMinutes(feature.ttlMinutes))
            }
        }

        // Try to consume 1 token. Returns true if allowed, false if empty.
        return rateLimiter.tryAcquire(1)
    }
}