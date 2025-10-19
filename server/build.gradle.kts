plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.darkmat13r"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(libs.kotlin.asyncapi.ktor)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.sse)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.koin.ktor)
    implementation(libs.koin.logger.slf4j)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    implementation(libs.mcp.kotlin.sdk)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.server.config.yaml)
    implementation("io.ktor:ktor-server-cors:3.3.0")
    implementation("com.squareup.okio:okio:3.12.0")
    implementation(libs.ktor.server.cio)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}


tasks.register("killPort8080") {
    group = "devops"
    description = "Kills any process listening on TCP port 8080 (macOS/Linux/Windows)"
    doLast {
        val os = System.getProperty("os.name").lowercase()
        if (os.contains("win")) {
            // Use PowerShell for Windows
            exec {
                commandLine(
                    "powershell",
                    "-NoProfile",
                    "-Command",
                    "${'$'}p=(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue).OwningProcess; if(${'$'}p){ Stop-Process -Id ${'$'}p -Force; Write-Host 'Killed PID ' ${'$'}p } else { Write-Host 'No listener on 8080' }"
                )
                isIgnoreExitValue = true
            }
        } else {
            // macOS/Linux: try lsof, fallback to fuser
            exec {
                commandLine(
                    "bash", "-lc",
                    "pids=$(lsof -ti tcp:8080 2>/dev/null || true); if [ -z \"${'$'}pids\" ]; then pids=$(fuser -n tcp 8080 2>/dev/null | tr ' ' '\n' || true); fi; if [ -n \"${'$'}pids\" ]; then kill -9 ${'$'}pids && echo Killed: ${'$'}pids; else echo 'No listener on 8080'; fi"
                )
                isIgnoreExitValue = true
            }
        }
    }
}
