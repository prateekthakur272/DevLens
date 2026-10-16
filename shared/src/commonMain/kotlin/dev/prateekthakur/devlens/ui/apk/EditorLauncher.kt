package dev.prateekthakur.devlens.ui.apk

import androidx.compose.runtime.compositionLocalOf
import dev.prateekthakur.devlens.domain.decompile.DecompiledProject

/**
 * Set by a host that can show extra windows (the desktop app). Takes the decompiled project and a title.
 * When null (web), the code editor opens inside the main window instead.
 */
val LocalEditorLauncher = compositionLocalOf<((DecompiledProject, String) -> Unit)?> { null }
