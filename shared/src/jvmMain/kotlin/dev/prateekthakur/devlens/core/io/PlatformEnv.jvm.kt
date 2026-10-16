package dev.prateekthakur.devlens.core.io

actual object PlatformEnv {
    actual fun get(name: String): String? = System.getenv(name)
    actual fun osName(): String = System.getProperty("os.name") ?: ""
}
