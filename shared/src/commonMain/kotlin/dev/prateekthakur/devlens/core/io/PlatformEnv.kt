package dev.prateekthakur.devlens.core.io

/** Read-only access to process environment variables. */
expect object PlatformEnv {
    fun get(name: String): String?
    fun osName(): String
}
