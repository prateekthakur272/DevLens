package dev.prateekthakur.devlens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.prateekthakur.devlens.core.model.ToolStatus
import dev.prateekthakur.devlens.domain.permission.PermissionRisk
import dev.prateekthakur.devlens.domain.security.Severity

/** container/content color pair for a filled [dev.prateekthakur.devlens.ui.common.Tag]. */
data class Tone(val container: Color, val content: Color)

@Composable
fun severityTone(severity: Severity): Tone {
    val dark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    return when (severity) {
        Severity.CRITICAL, Severity.HIGH -> Tone(cs.errorContainer, cs.onErrorContainer)
        Severity.MEDIUM -> Tone(
            if (dark) DevLensExtraColors.warningContainerDark else DevLensExtraColors.warningContainerLight,
            if (dark) DevLensExtraColors.onWarningContainerDark else DevLensExtraColors.onWarningContainerLight,
        )
        Severity.LOW -> Tone(cs.tertiaryContainer, cs.onTertiaryContainer)
        Severity.INFO -> Tone(cs.secondaryContainer, cs.onSecondaryContainer)
    }
}

@Composable
fun riskTone(risk: PermissionRisk): Tone {
    val dark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    return when (risk) {
        PermissionRisk.DANGEROUS -> Tone(cs.errorContainer, cs.onErrorContainer)
        PermissionRisk.SIGNATURE -> Tone(cs.tertiaryContainer, cs.onTertiaryContainer)
        PermissionRisk.SPECIAL -> Tone(
            if (dark) DevLensExtraColors.warningContainerDark else DevLensExtraColors.warningContainerLight,
            if (dark) DevLensExtraColors.onWarningContainerDark else DevLensExtraColors.onWarningContainerLight,
        )
        PermissionRisk.NORMAL -> Tone(
            if (dark) DevLensExtraColors.successContainerDark else DevLensExtraColors.successContainerLight,
            if (dark) DevLensExtraColors.onSuccessContainerDark else DevLensExtraColors.onSuccessContainerLight,
        )
        PermissionRisk.UNKNOWN -> Tone(cs.surfaceContainerHighest, cs.onSurfaceVariant)
    }
}

@Composable
fun toolStatusTone(status: ToolStatus): Tone {
    val dark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    return when (status) {
        ToolStatus.READY -> Tone(
            if (dark) DevLensExtraColors.successContainerDark else DevLensExtraColors.successContainerLight,
            if (dark) DevLensExtraColors.onSuccessContainerDark else DevLensExtraColors.onSuccessContainerLight,
        )
        ToolStatus.WARNING -> Tone(
            if (dark) DevLensExtraColors.warningContainerDark else DevLensExtraColors.warningContainerLight,
            if (dark) DevLensExtraColors.onWarningContainerDark else DevLensExtraColors.onWarningContainerLight,
        )
        ToolStatus.NOT_FOUND -> Tone(cs.errorContainer, cs.onErrorContainer)
    }
}

@Composable
fun logLevelColor(level: dev.prateekthakur.devlens.domain.logcat.LogLevel): Color {
    val dark = LocalIsDarkTheme.current
    val cs = MaterialTheme.colorScheme
    return when (level) {
        dev.prateekthakur.devlens.domain.logcat.LogLevel.VERBOSE -> cs.onSurfaceVariant
        dev.prateekthakur.devlens.domain.logcat.LogLevel.DEBUG -> cs.primary
        dev.prateekthakur.devlens.domain.logcat.LogLevel.INFO -> if (dark) DevLensExtraColors.successDark else DevLensExtraColors.successLight
        dev.prateekthakur.devlens.domain.logcat.LogLevel.WARN -> if (dark) DevLensExtraColors.warningDark else DevLensExtraColors.warningLight
        dev.prateekthakur.devlens.domain.logcat.LogLevel.ERROR, dev.prateekthakur.devlens.domain.logcat.LogLevel.FATAL -> cs.error
    }
}
