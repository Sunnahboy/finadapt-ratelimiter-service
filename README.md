# Adaptive Finance AI Backend & Rate Limiter

This project is the secure, high-performance middleware for the **Adaptive Finance** Android application. Built with Ktor 3.x, it acts as a secure proxy and traffic controller to protect third-party API quotas (like Groq) using a Redis-backed Token Bucket algorithm.

By abstracting API keys and request logic away from the client device, this backend ensures that malicious actors cannot steal quota or overload the system.

### Core Technologies

* [Ktor Documentation](https://ktor.io/docs/welcome.html)
* [Redisson (Redis Client) Documentation](https://redisson.org/)
* [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)

---

##  Features

This backend is built to modern industrial standards, prioritizing security, memory efficiency, and strict request pipelines.

| Feature | Description |
| :--- | :--- |
| **Token-Bucket Rate Limiter** | Redis-powered limits via Redisson to prevent API spam and DDoS attacks. Features TTL cleanup, local memory caching to prevent redundant network calls, and dynamic limits per feature (e.g., Chat vs. Receipt Scanning). |
| **Route Scoped Plugins** | Utilizes Ktor 3.x architecture (`createRouteScopedPlugin`) for a clean, DRY routing DSL (`withRateLimit`) that halts malicious requests *before* business logic executes. |
| **Memory Optimized** | Redisson is configured to bypass Jackson entirely, using native `StringCodec` to drastically reduce RAM usage for token tracking. |
| **Security Hardened** | Gradle constraints enforce strict version bumps to patch transitive vulnerabilities (e.g., Netty zip bomb DoS and Jackson core flaws). |
| **Content Negotiation** | Automatic content conversion according to `Content-Type` and `Accept` headers, easily feeding structured JSON back to the Android client. |
| **kotlinx.serialization** | High-performance JSON serialization using the native Kotlin library. |

---

##  Configuration (`application.yaml`)

The server uses `EngineMain` to automatically inject configurations. By default, it expects a local Redis instance. For production, inject your managed Redis URL (e.g., AWS ElastiCache, Upstash) into the environment.

```yaml
ktor:
  deployment:
    port: 8080
  application:
    modules:
      - com.finadapt.backend.ApplicationKt.module

redis:
  url: "redis://127.0.0.1:6379" # Swap this in production
```

---

##  Building & Running

To build or run the project locally, ensure you have a local Redis instance running on port `6379`. You can spin one up quickly using Docker: `docker run -p 6379:6379 -d redis`.

Then, use one of the following Gradle tasks:

| Task | Description |
| :--- | :--- |
| `./gradlew run` | **Run the server locally.** (Hot-reloads if configured) |
| `./gradlew test` | Run the test suite. |
| `./gradlew build` | Build the entire project. |
| `./gradlew buildFatJar` | Build an executable JAR of the server with all dependencies included. |
| `./gradlew buildImage` | Build the Docker image to use with the fat JAR for production deployment. |

If the server starts successfully and connects to Redis, you'll see the following output in your terminal:

```text
2026-04-03 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2026-04-03 14:32:45.682 [main] INFO  Application - Responding at [http://0.0.0.0:8080](http://0.0.0.0:8080)
```
