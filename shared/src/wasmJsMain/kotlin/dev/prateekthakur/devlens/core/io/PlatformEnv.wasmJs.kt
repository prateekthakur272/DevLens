package dev.prateekthakur.devlens.core.io

actual object PlatformEnv {
    actual fun get(name: String): String? = null
    actual fun osName(): String = "web"
}
