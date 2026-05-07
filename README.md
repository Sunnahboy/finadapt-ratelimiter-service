# Adaptive Finance AI Backend & rate Limiter

This project is the secure, high-performance middleware for the **Adaptive Finance** Android application. Built with Ktor 3.x, it acts as a secure proxy and traffic controller to protect third-party API quotas (e.g., OpenAI, Groq, custom LLMs) using a Redis-backed Token Bucket algorithm.

By abstracting API keys and request logic away from the client device, this backend ensures that malicious actors cannot steal quota, exploit secrets, or overload the system.

###  Core Technologies

* [Ktor Documentation](https://ktor.io/docs/welcome.html)
* [Redisson (Redis Client) Documentation](https://redisson.org/)
* [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)

---

##  Features

This backend is built to modern industrial standards, prioritizing security, memory efficiency, and strict request pipelines.

| Feature | Description |
| :--- | :--- |
| **LLM-Agnostic Proxy** | Built to dynamically route prompts to any AI provider (OpenAI, Groq, Together AI) without vendor lock-in. |
| **Token-Bucket rate Limiter** | Redis-powered limits via Redisson to prevent API spam and DDoS attacks. Features TTL cleanup, local memory caching to prevent redundant network calls, and dynamic limits per feature (e.g., Chat vs. Receipt Scanning). |
| **Fail-Fast Configuration** | Enforces strict environment variable checks on startup. The server immediately halts if essential secrets (API keys, Redis URLs) are missing, preventing silent runtime failures. |
| **Route Scoped Plugins** | Utilizes Ktor 3.x architecture (`createRouteScopedPlugin`) for a clean, DRY routing DSL (`withRateLimit`) that halts malicious requests *before* business logic executes. |
| **Memory Optimized** | Redisson is configured to bypass Jackson entirely, using native `StringCodec` to drastically reduce RAM usage for token tracking. |
| **Security Hardened** | Gradle constraints enforce strict version bumps to patch transitive vulnerabilities (e.g., Netty zip bomb DoS and Jackson core flaws). |

---

##  Configuration (`application.yaml`)

The server uses `EngineMain` to automatically inject configurations. Because of our **Fail-Fast** setup, you *must* provide these keys, or the server will safely refuse to start. 

Replace the placeholder values in `src/main/resources/application.yaml` (or inject them via system environment variables) before running:

```yaml
ktor:
  deployment:
    port: 8080
  application:
    modules:
      -  ApplicationKt.module

redis:
  url: ${REDIS_URL}

llm:
  apiKey: ${LLM_API_KEY}
  baseUrl: ${LLM_BASE_URL}
  model: ${LLM_MODEL}
 Building & Running
To run the project locally, ensure you have filled out the application.yaml and have a local Redis instance running on port 6379. You can spin one up quickly using Docker:

Bash
docker run -p 6379:6379 -d redis
Then, use one of the following Gradle tasks:

Task	Description
./gradlew run	Run the server locally. (Hot-reloads if configured)
./gradlew test	Run the test suite.
./gradlew build	Build the entire project.
./gradlew buildFatJar	Build an executable JAR of the server with all dependencies included.
./gradlew buildImage	Build the Docker image to use with the fat JAR for production deployment.


