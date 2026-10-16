package dev.prateekthakur.devlens.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf

/** Lets any screen surface a transient confirmation/error without threading a host through every call site. */
val LocalSnackbarHostState = compositionLocalOf<SnackbarHostState> {
    error("LocalSnackbarHostState not provided — wrap content in App's Scaffold")
}
