package dev.prateekthakur.devlens.di

import dev.prateekthakur.devlens.core.io.PlatformFiles
import dev.prateekthakur.devlens.core.process.CommandExecutor
import dev.prateekthakur.devlens.core.AppCommand
import dev.prateekthakur.devlens.core.AppSession
import dev.prateekthakur.devlens.core.settings.AppSettings
import kotlinx.coroutines.flow.MutableSharedFlow
import dev.prateekthakur.devlens.domain.ai.AiService
import dev.prateekthakur.devlens.domain.ai.MockAiService
import dev.prateekthakur.devlens.domain.android.AndroidProjectAnalyzer
import dev.prateekthakur.devlens.domain.android.GradleAndroidProjectAnalyzer
import dev.prateekthakur.devlens.domain.apk.ApkAnalyzer
import dev.prateekthakur.devlens.domain.apk.ApkIdentityReader
import dev.prateekthakur.devlens.domain.build.BuildDoctor
import dev.prateekthakur.devlens.domain.build.RuleBasedBuildDoctor
import dev.prateekthakur.devlens.domain.crash.CrashAnalyzer
import dev.prateekthakur.devlens.domain.crash.RegexCrashAnalyzer
import dev.prateekthakur.devlens.domain.deeplink.AdbDeepLinkTester
import dev.prateekthakur.devlens.domain.deeplink.DeepLinkTester
import dev.prateekthakur.devlens.domain.dependency.DependencyAnalyzer
import dev.prateekthakur.devlens.domain.dependency.FlutterPubDependencyAnalyzer
import dev.prateekthakur.devlens.domain.device.DeviceManager
import dev.prateekthakur.devlens.domain.decompile.ApkDecompiler
import dev.prateekthakur.devlens.domain.dex.DexClassAnalyzer
import dev.prateekthakur.devlens.domain.environment.DefaultEnvironmentDoctor
import dev.prateekthakur.devlens.domain.environment.EnvironmentDoctor
import dev.prateekthakur.devlens.domain.flutter.FlutterProjectAnalyzer
import dev.prateekthakur.devlens.domain.flutter.PubspecFlutterProjectAnalyzer
import dev.prateekthakur.devlens.domain.intent.AdbIntentTester
import dev.prateekthakur.devlens.domain.intent.IntentTester
import dev.prateekthakur.devlens.domain.logcat.AdbLogcatSource
import dev.prateekthakur.devlens.domain.logcat.LogcatSource
import dev.prateekthakur.devlens.domain.manifest.ManifestAnalyzer
import dev.prateekthakur.devlens.domain.permission.PermissionAnalyzer
import dev.prateekthakur.devlens.domain.permission.PermissionClassifier
import dev.prateekthakur.devlens.domain.report.DefaultReportGenerator
import dev.prateekthakur.devlens.domain.report.PdfReportWriter
import dev.prateekthakur.devlens.domain.report.ReportGenerator
import dev.prateekthakur.devlens.domain.secrets.ApkSecretsAnalyzer
import dev.prateekthakur.devlens.domain.security.SecurityAnalyzer
import dev.prateekthakur.devlens.domain.security.SecurityRuleEngine
import dev.prateekthakur.devlens.domain.signing.ApksignerSigningAnalyzer
import dev.prateekthakur.devlens.domain.signing.SigningAnalyzer
import dev.prateekthakur.devlens.domain.size.SizeAnalyzer
import dev.prateekthakur.devlens.infrastructure.adb.AdbDeviceManager
import dev.prateekthakur.devlens.infrastructure.android.AndroidSdkLocator
import dev.prateekthakur.devlens.infrastructure.apk.AaptManifestAnalyzer
import dev.prateekthakur.devlens.infrastructure.apk.createApkIdentityReader
import dev.prateekthakur.devlens.infrastructure.apk.createManifestAnalyzer
import dev.prateekthakur.devlens.infrastructure.apk.createSigningAnalyzer
import dev.prateekthakur.devlens.infrastructure.apk.createApkSecretsAnalyzer
import dev.prateekthakur.devlens.infrastructure.apk.createDexClassAnalyzer
import dev.prateekthakur.devlens.infrastructure.apk.createZipBasedApkAnalyzer
import dev.prateekthakur.devlens.infrastructure.apk.createZipBasedSizeAnalyzer
import dev.prateekthakur.devlens.infrastructure.decompile.createApkDecompiler
import dev.prateekthakur.devlens.infrastructure.report.createPdfReportWriter

/** Composition root — wires every domain service to its real implementation. */
class AppServices {
    private val executor = CommandExecutor()
    private val files = PlatformFiles()
    val settings = AppSettings(files)

    /** Menu-bar actions for the analysis screen, and the state the menu needs to enable/disable items. */
    val commands = MutableSharedFlow<AppCommand>(extraBufferCapacity = 8)
    val session = AppSession()

    /** Paths of files dropped onto the window by the host (desktop drag and drop). */
    val fileDrops = MutableSharedFlow<String>(extraBufferCapacity = 4)

    val sdkLocator = AndroidSdkLocator(files, executor) { settings.sdkPathOverride }

    val environmentDoctor: EnvironmentDoctor = DefaultEnvironmentDoctor(executor, sdkLocator)
    val deviceManager: DeviceManager = AdbDeviceManager(executor, sdkLocator)

    val manifestAnalyzer: ManifestAnalyzer = createManifestAnalyzer(executor, sdkLocator)
    val permissionAnalyzer: PermissionAnalyzer = PermissionClassifier(manifestAnalyzer)
    val signingAnalyzer: SigningAnalyzer = createSigningAnalyzer(executor, sdkLocator)
    val apkAnalyzer: ApkAnalyzer = createZipBasedApkAnalyzer(manifestAnalyzer)
    val identityReader: ApkIdentityReader = createApkIdentityReader(executor, sdkLocator)
    val sizeAnalyzer: SizeAnalyzer = createZipBasedSizeAnalyzer()
    val dexClassAnalyzer: DexClassAnalyzer = createDexClassAnalyzer()
    val secretsAnalyzer: ApkSecretsAnalyzer = createApkSecretsAnalyzer()
    val securityAnalyzer: SecurityAnalyzer = SecurityRuleEngine(
        manifestAnalyzer, permissionAnalyzer, signingAnalyzer, apkAnalyzer, dexClassAnalyzer, secretsAnalyzer,
    )

    val logcatSource: LogcatSource = AdbLogcatSource(executor, sdkLocator)
    val crashAnalyzer: CrashAnalyzer = RegexCrashAnalyzer()

    val intentTester: IntentTester = AdbIntentTester(executor, sdkLocator)
    val deepLinkTester: DeepLinkTester = AdbDeepLinkTester(intentTester)

    val flutterProjectAnalyzer: FlutterProjectAnalyzer = PubspecFlutterProjectAnalyzer(files, executor)
    val androidProjectAnalyzer: AndroidProjectAnalyzer = GradleAndroidProjectAnalyzer(files)
    val dependencyAnalyzer: DependencyAnalyzer = FlutterPubDependencyAnalyzer(executor)
    val buildDoctor: BuildDoctor = RuleBasedBuildDoctor()

    val reportGenerator: ReportGenerator = DefaultReportGenerator()
    val pdfReportWriter: PdfReportWriter = createPdfReportWriter()
    val apkDecompiler: ApkDecompiler = createApkDecompiler()
    val aiService: AiService = MockAiService()
}
