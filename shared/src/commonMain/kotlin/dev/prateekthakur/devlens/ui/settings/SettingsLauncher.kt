package dev.prateekthakur.devlens.ui.settings

import androidx.compose.runtime.compositionLocalOf

/** Set by a host that can show a settings window (the desktop app). Null where there is none. */
val LocalSettingsLauncher = compositionLocalOf<(() -> Unit)?> { null }
