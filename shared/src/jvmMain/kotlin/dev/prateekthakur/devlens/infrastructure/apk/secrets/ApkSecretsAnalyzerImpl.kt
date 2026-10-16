package dev.prateekthakur.devlens.infrastructure.apk.secrets

import dev.prateekthakur.devlens.domain.secrets.ApkSecretsAnalyzer
import dev.prateekthakur.devlens.domain.secrets.RegexSecretsScanner
import dev.prateekthakur.devlens.domain.secrets.SecretFinding
import dev.prateekthakur.devlens.infrastructure.apk.BinaryStringExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipFile

/**
 * PRD §39 — extracts printable strings from DEX/native/resource/asset files (no external
 * `strings` binary required) and runs [RegexSecretsScanner] over them. Treats every entry as
 * untrusted binary input (PRD §40): every file is read up to a bounded size only.
 */
class ApkSecretsAnalyzerImpl : ApkSecretsAnalyzer {

    private val scanner = RegexSecretsScanner()
    private val minStringLength = 6
    private val maxEntryBytes = 20L * 1024 * 1024
    private val includeExtensions = setOf("dex", "so", "json", "txt", "js", "xml", "properties", "yaml", "yml", "cfg", "conf", "pem", "key", "arsc")

    override suspend fun analyze(apkPath: String): Result<List<SecretFinding>> = withContext(Dispatchers.IO) {
        runCatching {
            val strings = mutableListOf<Pair<String, String>>()
            ZipFile(File(apkPath)).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue
                    val extension = entry.name.substringAfterLast('.', "").lowercase()
                    if (extension !in includeExtensions) continue
                    if (entry.size !in 0..maxEntryBytes) continue

                    val bytes = zip.getInputStream(entry).use { it.readBytes() }
                    BinaryStringExtractor.extract(bytes, minStringLength).forEach { strings += entry.name to it }
                }
            }
            scanner.scan(strings)
        }
    }
}
