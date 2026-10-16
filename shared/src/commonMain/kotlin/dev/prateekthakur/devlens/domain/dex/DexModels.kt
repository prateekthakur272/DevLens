package dev.prateekthakur.devlens.domain.dex

/** PRD §16/§40-adjacent — a class found in a DEX file, read as untrusted binary input. */
data class DexClassInfo(
    val descriptor: String,
    val packageName: String,
    val simpleName: String,
    val superclass: String?,
    val interfaces: List<String>,
    val isInterface: Boolean,
    val isAbstract: Boolean,
    val isEnum: Boolean,
    val visibility: String,
    val fieldCount: Int,
    val methodCount: Int,
    val dexFile: String,
)

/** Browses the classes embedded in an APK's DEX file(s) — no decompilation, just structure. */
interface DexClassAnalyzer {
    suspend fun analyze(apkPath: String): Result<List<DexClassInfo>>
}
