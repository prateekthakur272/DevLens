package dev.prateekthakur.devlens.domain.environment

import dev.prateekthakur.devlens.core.model.ToolCheck
import dev.prateekthakur.devlens.core.model.ToolStatus
import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator

/** PRD §11 — Environment Doctor: detect ADB, Android SDK, Java, Gradle, Flutter, Dart, Git. */
interface EnvironmentDoctor {
    suspend fun checkAll(): List<ToolCheck>
}

class DefaultEnvironmentDoctor(
    private val executor: CommandExecutor = CommandExecutor(),
    private val sdkLocator: AndroidSdkLocator = AndroidSdkLocator(executor = executor),
) : EnvironmentDoctor {

    override suspend fun checkAll(): List<ToolCheck> = listOf(
        checkJava(),
        checkAdb(),
        checkAndroidSdk(),
        checkGradle(),
        checkFlutter(),
        checkDart(),
        checkGit(),
    )

    private suspend fun checkAndroidSdk(): ToolCheck {
        val root = sdkLocator.sdkRoot()
            ?: return ToolCheck(
                name = "Android SDK",
                status = ToolStatus.NOT_FOUND,
                message = "Android SDK not found. Set ANDROID_HOME or configure the SDK path in Settings.",
            )
        val aapt2 = sdkLocator.findAapt2()
        return ToolCheck(
            name = "Android SDK",
            status = if (aapt2 != null) ToolStatus.READY else ToolStatus.WARNING,
            path = root,
            message = if (aapt2 == null) "No build-tools found under $root/build-tools — APK manifest analysis will be unavailable." else null,
        )
    }

    private suspend fun checkJava(): ToolCheck {
        val path = executor.which("java")
        val result = executor.run("java", listOf("-version"))
        // java -version writes to stderr
        val versionLine = result.stderr.lineSequence().firstOrNull() ?: result.stdout.lineSequence().firstOrNull()
        return if (path != null && result.exitCode == 0) {
            ToolCheck(
                name = "Java",
                status = ToolStatus.READY,
                version = versionLine?.substringAfter('"')?.substringBefore('"'),
                path = path,
                minSupportedVersion = "17",
                recommendedVersion = "21",
            )
        } else {
            ToolCheck(
                name = "Java",
                status = ToolStatus.NOT_FOUND,
                message = "Java not found on PATH. Install a JDK or configure JAVA_HOME.",
                minSupportedVersion = "17",
                recommendedVersion = "21",
            )
        }
    }

    private suspend fun checkAdb(): ToolCheck {
        val path = sdkLocator.findAdb()
        if (path == null) {
            return ToolCheck(
                name = "ADB",
                status = ToolStatus.NOT_FOUND,
                message = "ADB not found. Install Android SDK Platform Tools or configure the SDK path in Settings.",
            )
        }
        val result = executor.run(path, listOf("version"))
        val version = result.stdout.lineSequence().firstOrNull { it.contains("version", ignoreCase = true) }
            ?.substringAfterLast(' ')
        return ToolCheck(
            name = "ADB",
            status = if (result.isSuccess) ToolStatus.READY else ToolStatus.WARNING,
            version = version,
            path = path,
        )
    }

    private suspend fun checkGradle(): ToolCheck {
        val path = executor.which("gradle")
        if (path == null) {
            return ToolCheck(
                name = "Gradle",
                status = ToolStatus.WARNING,
                message = "Gradle not found on PATH. Projects using the Gradle wrapper are unaffected.",
            )
        }
        val result = executor.run("gradle", listOf("--version"))
        val version = result.stdout.lineSequence().firstOrNull { it.startsWith("Gradle ") }?.removePrefix("Gradle ")
        return ToolCheck(
            name = "Gradle",
            status = if (result.isSuccess) ToolStatus.READY else ToolStatus.WARNING,
            version = version,
            path = path,
        )
    }

    private suspend fun checkFlutter(): ToolCheck {
        val path = executor.which("flutter")
        if (path == null) {
            return ToolCheck(
                name = "Flutter",
                status = ToolStatus.NOT_FOUND,
                message = "Flutter not found. Install Flutter or configure the Flutter SDK path in Settings.",
            )
        }
        val result = executor.run("flutter", listOf("--version"))
        val version = result.stdout.lineSequence().firstOrNull { it.startsWith("Flutter ") }
            ?.removePrefix("Flutter ")?.substringBefore(' ')
        return ToolCheck(
            name = "Flutter",
            status = if (result.isSuccess) ToolStatus.READY else ToolStatus.WARNING,
            version = version,
            path = path,
        )
    }

    private suspend fun checkDart(): ToolCheck {
        val path = executor.which("dart")
        if (path == null) {
            return ToolCheck(
                name = "Dart",
                status = ToolStatus.NOT_FOUND,
                message = "Dart not found. It is normally bundled with the Flutter SDK.",
            )
        }
        val result = executor.run("dart", listOf("--version"))
        val version = (if (result.isSuccess) result.stdout else result.stderr)
            .substringAfter("version ", "").substringBefore(" ")
            .ifBlank { null }
        return ToolCheck(
            name = "Dart",
            status = if (result.isSuccess) ToolStatus.READY else ToolStatus.WARNING,
            version = version,
            path = path,
        )
    }

    private suspend fun checkGit(): ToolCheck {
        val path = executor.which("git")
        if (path == null) {
            return ToolCheck(
                name = "Git",
                status = ToolStatus.WARNING,
                message = "Git not found. Project version-control features will be unavailable.",
            )
        }
        val result = executor.run("git", listOf("--version"))
        val version = result.stdout.trim().removePrefix("git version ")
        return ToolCheck(
            name = "Git",
            status = if (result.isSuccess) ToolStatus.READY else ToolStatus.WARNING,
            version = version.ifBlank { null },
            path = path,
        )
    }
}
