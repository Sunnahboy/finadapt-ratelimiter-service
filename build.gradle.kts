plugins {
    kotlin("jvm") version "2.3.0"
    id("io.ktor.plugin") version "3.4.2"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.0"
}

group = "com.finadapt.backend" // Changed to match your domain
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm")
    implementation("io.ktor:ktor-server-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-server-netty-jvm")
    implementation("io.ktor:ktor-server-config-yaml")
    testImplementation("io.ktor:ktor-server-test-host")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.3.0")
    implementation("org.redisson:redisson:3.27.0")
    //Ktor Client for making requests
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-cio")
    implementation("io.ktor:ktor-client-content-negotiation")
    //ensure JSON works in the client
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("ch.qos.logback:logback-classic:1.5.25")


    //SECURITY PATCHES
    constraints {
        implementation("com.fasterxml.jackson.core:jackson-core:2.18.0") {
            because("Mend.io flagged 2.17.0 for WS-2026-0003")
        }
        implementation("io.netty:netty-codec:4.1.115.Final") {
            because("Mend.io flagged 4.1.110.Final for zip bomb DoS")
        }
    }
}