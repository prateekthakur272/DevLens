package dev.prateekthakur.devlens.infrastructure.decompile

import dev.prateekthakur.devlens.domain.decompile.ApkDecompiler
import dev.prateekthakur.devlens.domain.decompile.DecompiledFile
import dev.prateekthakur.devlens.domain.decompile.DecompiledProject
import jadx.api.JadxArgs
import jadx.api.JadxDecompiler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Decompiles with the JADX library into a temp folder: Java under `sources/`, decoded resources under `resources/`. */
class JadxApkDecompiler : ApkDecompiler {

    private val textExtensions = setOf(
        "java", "kt", "xml", "json", "txt", "properties", "html", "js", "css", "md", "yml", "yaml", "cfg", "gradle", "version", "pro", "mf", "sf",
    )

    override suspend fun decompile(apkPath: String, onProgress: (Float) -> Unit): Result<DecompiledProject> =
        withContext(Dispatchers.IO) {
            runCatching {
                val apk = File(apkPath)
                require(apk.isFile) { "File not found: $apkPath" }
                val out = File(System.getProperty("java.io.tmpdir"), "devlens-decompiled/${apk.nameWithoutExtension}-${apk.absolutePath.hashCode().toUInt()}")
                out.deleteRecursively()
                out.mkdirs()

                val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)
                val args = JadxArgs().apply {
                    setInputFile(apk)
                    setOutDir(out)
                    setThreadsCount(threads)
                    setShowInconsistentCode(true)
                    setDeobfuscationOn(false)
                }
                var lastReported = -1
                JadxDecompiler(args).use { decompiler ->
                    decompiler.load()
                    decompiler.save(threads) { done, total ->
                        val pct = if (total <= 0) 0 else (done * 100 / total).toInt()
                        if (pct != lastReported) {
                            lastReported = pct
                            onProgress(pct / 100f)
                        }
                    }
                }

                val files = out.walkTopDown()
                    .filter { it.isFile && it.extension.lowercase() in textExtensions }
                    .map { DecompiledFile(it.relativeTo(out).invariantSeparatorsPath, it.length()) }
                    .toList()
                check(files.isNotEmpty()) { "Decompilation produced no readable files." }
                DecompiledProject(out.absolutePath, files)
            }
        }
}
