package dev.prateekthakur.devlens.domain.android

/** PRD §29 — native Android/Gradle project analysis. */
data class AndroidProjectInfo(
    val gradleVersion: String?,
    val agpVersion: String?,
    val kotlinVersion: String?,
    val javaVersion: String?,
    val compileSdk: Int?,
    val targetSdk: Int?,
    val minSdk: Int?,
)

interface AndroidProjectAnalyzer {
    suspend fun analyze(projectDir: String): Result<AndroidProjectInfo>
}

/** PRD §29 — reads Gradle/AGP/Kotlin/SDK versions straight out of the project's build files. */
class GradleAndroidProjectAnalyzer(
    private val files: dev.prateekthakur.devlens.core.io.PlatformFiles = dev.prateekthakur.devlens.core.io.PlatformFiles(),
) : AndroidProjectAnalyzer {

    override suspend fun analyze(projectDir: String): Result<AndroidProjectInfo> {
        val appBuildGradle = firstExisting(
            files.join(projectDir, "android", "app", "build.gradle.kts"),
            files.join(projectDir, "android", "app", "build.gradle"),
            files.join(projectDir, "app", "build.gradle.kts"),
            files.join(projectDir, "app", "build.gradle"),
        ) ?: return Result.failure(IllegalArgumentException("No app/build.gradle(.kts) found under $projectDir"))

        val rootBuildGradle = firstExisting(
            files.join(projectDir, "android", "build.gradle.kts"),
            files.join(projectDir, "android", "build.gradle"),
            files.join(projectDir, "build.gradle.kts"),
            files.join(projectDir, "build.gradle"),
        )
        val gradleWrapperProps = firstExisting(
            files.join(projectDir, "android", "gradle", "wrapper", "gradle-wrapper.properties"),
            files.join(projectDir, "gradle", "wrapper", "gradle-wrapper.properties"),
        )
        val gradleProperties = firstExisting(
            files.join(projectDir, "android", "gradle.properties"),
            files.join(projectDir, "gradle.properties"),
        )

        val appText = files.readText(appBuildGradle).orEmpty()
        val rootText = rootBuildGradle?.let { files.readText(it) }.orEmpty()
        val wrapperText = gradleWrapperProps?.let { files.readText(it) }.orEmpty()
        val propertiesText = gradleProperties?.let { files.readText(it) }.orEmpty()
        val combined = appText + "\n" + rootText

        return Result.success(
            AndroidProjectInfo(
                gradleVersion = Regex("""gradle-([\d.]+)-\S+\.zip""").find(wrapperText)?.groupValues?.get(1),
                agpVersion = Regex("""com\.android\.(?:application|library)['"]\s*\)?\s*version\s*['"]([\d.]+)['"]""")
                    .find(combined)?.groupValues?.get(1)
                    ?: Regex("""classpath\s*['"]com\.android\.tools\.build:gradle:([\d.]+)['"]""").find(combined)?.groupValues?.get(1)
                    ?: Regex("""agp[_.]?version\s*=\s*['"]?([\d.]+)['"]?""", RegexOption.IGNORE_CASE).find(propertiesText)?.groupValues?.get(1),
                kotlinVersion = Regex("""org\.jetbrains\.kotlin\.android['"]\s*version\s*['"]([\d.]+)['"]""").find(combined)?.groupValues?.get(1)
                    ?: Regex("""kotlin_version\s*=\s*['"]?([\d.]+)['"]?""").find(combined + propertiesText)?.groupValues?.get(1),
                javaVersion = Regex("""JavaVersion\.VERSION_(\d+(?:_\d+)?)""").find(appText)?.groupValues?.get(1)?.replace("_", "."),
                compileSdk = Regex("""compileSdk(?:Version)?\s*=?\s*(\d+)""").find(appText)?.groupValues?.get(1)?.toIntOrNull(),
                targetSdk = Regex("""targetSdk(?:Version)?\s*=?\s*(\d+)""").find(appText)?.groupValues?.get(1)?.toIntOrNull(),
                minSdk = Regex("""minSdk(?:Version)?\s*=?\s*(\d+)""").find(appText)?.groupValues?.get(1)?.toIntOrNull(),
            ),
        )
    }

    private suspend fun firstExisting(vararg paths: String): String? = paths.firstOrNull { files.exists(it) }
}
