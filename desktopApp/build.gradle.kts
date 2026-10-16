import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "dev.prateekthakur.devlens.MainKt"

        buildTypes.release.proguard {
            // JADX loads its input plugins via ServiceLoader/reflection and PDFBox, skiko and others reference optional
            // classes; shrinking/obfuscating breaks them, so the release package ships the libraries as they are.
            isEnabled.set(false)
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "DevLens"
            description = "Local-first Android APK/AAB inspector"
            vendor = "Prateek Thakur"

            // The bundled runtime is trimmed by jlink; these are needed by JADX, PDFBox and Compose at runtime.
            modules("java.sql", "java.naming", "java.management", "java.desktop", "jdk.unsupported", "jdk.security.auth")
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("icons/app.icns"))
                bundleID = "dev.prateekthakur.devlens"
            }
            windows { iconFile.set(project.file("icons/app.ico")) }
            linux { iconFile.set(project.file("icons/app.png")) }
        }
    }
}