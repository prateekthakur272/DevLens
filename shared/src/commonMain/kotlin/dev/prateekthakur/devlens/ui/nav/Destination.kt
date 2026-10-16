package dev.prateekthakur.devlens.ui.nav

import dev.prateekthakur.devlens.ui.theme.AiIcon
import dev.prateekthakur.devlens.ui.theme.ApkIcon
import dev.prateekthakur.devlens.ui.theme.DashboardIcon
import dev.prateekthakur.devlens.ui.theme.DebugIcon
import dev.prateekthakur.devlens.ui.theme.DevicesIcon
import dev.prateekthakur.devlens.ui.theme.FlutterIcon
import dev.prateekthakur.devlens.ui.theme.NavIcon
import dev.prateekthakur.devlens.ui.theme.ProjectsIcon
import dev.prateekthakur.devlens.ui.theme.ReportsIcon
import dev.prateekthakur.devlens.ui.theme.SettingsIcon

/** PRD §9 — top-level navigation. */
enum class Destination(val label: String, val icon: NavIcon) {
    DASHBOARD("Dashboard", DashboardIcon),
    PROJECTS("Projects", ProjectsIcon),
    APK("APK / AAB", ApkIcon),
    DEVICES("Devices", DevicesIcon),
    DEBUG("Debug", DebugIcon),
    FLUTTER("Flutter", FlutterIcon),
    REPORTS("Reports", ReportsIcon),
    AI_ASSISTANT("AI Assistant", AiIcon),
    SETTINGS("Settings", SettingsIcon),
}
