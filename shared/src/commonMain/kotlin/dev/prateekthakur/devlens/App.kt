package dev.prateekthakur.devlens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.prateekthakur.devlens.core.settings.LocalAppSettings
import dev.prateekthakur.devlens.di.AppServices
import dev.prateekthakur.devlens.ui.apk.ApkScreen
import dev.prateekthakur.devlens.ui.common.LocalSnackbarHostState
import dev.prateekthakur.devlens.ui.theme.DevLensTheme

@Composable
@Preview
fun App(services: AppServices = remember { AppServices() }) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(services) { services.settings.load() }

    CompositionLocalProvider(LocalAppSettings provides services.settings) {
    DevLensTheme(darkTheme = services.settings.darkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    ApkScreen(
                        services.apkAnalyzer,
                        services.manifestAnalyzer,
                        services.permissionAnalyzer,
                        services.securityAnalyzer,
                        services.sizeAnalyzer,
                        services.dexClassAnalyzer,
                        services.secretsAnalyzer,
                        services.signingAnalyzer,
                        services.reportGenerator,
                        services.pdfReportWriter,
                        services.apkDecompiler,
                        services.identityReader,
                        services.fileDrops,
                        services.commands,
                        services.session,
                    )
                }
            }
        }
    }
    }
}
