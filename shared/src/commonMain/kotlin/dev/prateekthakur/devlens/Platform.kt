package dev.prateekthakur.devlens

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform