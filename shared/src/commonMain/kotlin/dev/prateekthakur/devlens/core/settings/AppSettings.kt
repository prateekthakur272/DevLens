package dev.prateekthakur.devlens.core.settings

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.prateekthakur.devlens.core.io.PlatformEnv
import dev.prateekthakur.devlens.core.io.PlatformFiles
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * User preferences, observable from Compose and persisted as `key=value` lines in `~/.devlens/settings.properties`.
 * Every window reads the same instance, so a change shows up everywhere immediately.
 */
class AppSettings(private val files: PlatformFiles = PlatformFiles()) {
    var darkTheme by mutableStateOf(false)
        private set
    var sdkPathOverride by mutableStateOf<String?>(null)
        private set
    var editorFontSize by mutableStateOf(DEFAULT_EDITOR_FONT_SIZE)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun updateDarkTheme(value: Boolean) { darkTheme = value; persist() }

    fun updateSdkPathOverride(value: String?) { sdkPathOverride = value?.trim()?.takeIf { it.isNotEmpty() }; persist() }

    fun updateEditorFontSize(value: Int) { editorFontSize = value.coerceIn(MIN_EDITOR_FONT_SIZE, MAX_EDITOR_FONT_SIZE); persist() }

    suspend fun load() {
        val path = filePath() ?: return
        val text = files.readText(path) ?: return
        val map = text.lines().mapNotNull { line ->
            val i = line.indexOf('=')
            if (i <= 0) null else line.substring(0, i).trim() to line.substring(i + 1).trim()
        }.toMap()
        map["theme"]?.let { darkTheme = it == "dark" }
        sdkPathOverride = map["sdkPath"]?.takeIf { it.isNotEmpty() }
        map["editorFontSize"]?.toIntOrNull()?.let { editorFontSize = it.coerceIn(MIN_EDITOR_FONT_SIZE, MAX_EDITOR_FONT_SIZE) }
    }

    private fun persist() {
        val path = filePath() ?: return
        val content = buildString {
            appendLine("theme=${if (darkTheme) "dark" else "light"}")
            appendLine("sdkPath=${sdkPathOverride.orEmpty()}")
            appendLine("editorFontSize=$editorFontSize")
        }
        scope.launch { files.writeText(path, content) }
    }

    private fun filePath(): String? {
        val home = PlatformEnv.get("HOME") ?: PlatformEnv.get("USERPROFILE") ?: return null
        return files.join(home, ".devlens", "settings.properties")
    }

    companion object {
        const val DEFAULT_EDITOR_FONT_SIZE = 12
        const val MIN_EDITOR_FONT_SIZE = 9
        const val MAX_EDITOR_FONT_SIZE = 20
    }
}

/** Provided by every window so UI code can read preferences without threading them through. */
val LocalAppSettings = compositionLocalOf<AppSettings?> { null }
