# Adaptive Finance AI Backend & Rate Limiter

This project is the secure, high-performance middleware for the Adaptive Finance Android application. Built with Ktor, it acts as a secure proxy and traffic controller to protect third-party API quotas (like Groq) using a Redis-backed Token Bucket algorithm.

Here are some useful links for the core technologies used:
* [Ktor Documentation](https://ktor.io/docs/welcome.html)
* [Redisson (Redis Client) Documentation](https://redisson.pro/docs/)

## Features

Here's a list of features included in this project:

| Name | Description |
| --- | --- |
| **Routing** | Structured routing DSL for endpoints (e.g., `/api/chat`, `/api/scan`). |
| **Token-Bucket Rate Limiter** | Redis-powered limits via `Redisson` to prevent API spam and DDoS attacks. Features TTL cleanup and dynamic limits per feature. |
| **Content Negotiation** | Automatic content conversion according to `Content-Type` and `Accept` headers. |
| **kotlinx.serialization** | High-performance JSON serialization using the native Kotlin library. |

## Building & Running

To build or run the project locally, ensure you have a local Redis instance running (`redis://127.0.0.1:6379`), then use one of the following tasks:

| Task | Description |
| --- | --- |
| `./gradlew test` | Run the tests |
| `./gradlew build` | Build everything |
| `./gradlew buildFatJar` | Build an executable JAR of the server with all dependencies included |
| `./gradlew buildImage` | Build the docker image to use with the fat JAR for production deployment |
| `./gradlew run` | Run the server locally |

If the server starts successfully, you'll see the following output in your terminal:

```text
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at [http://0.0.0.0:8080](http://0.0.0.0:8080)
