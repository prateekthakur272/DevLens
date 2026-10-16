package dev.prateekthakur.devlens.domain.secrets

/** PRD §39 — Secrets Detection. Kinds are deliberately specific so the UI can explain *why* a string matched. */
enum class SecretKind(val label: String) {
    AWS_ACCESS_KEY("AWS Access Key"),
    GOOGLE_API_KEY("Google/Firebase API Key"),
    SLACK_TOKEN("Slack Token"),
    STRIPE_KEY("Stripe Key"),
    GITHUB_TOKEN("GitHub Token"),
    PRIVATE_KEY("Private Key"),
    JWT("JWT"),
    BEARER_TOKEN("Authorization Header"),
    BASIC_AUTH_URL("Credentials in URL"),
    GENERIC_API_KEY("Generic API Key/Secret Assignment"),
    FIREBASE_DATABASE_URL("Firebase Realtime Database URL"),
}

data class SecretFinding(
    val kind: SecretKind,
    val maskedValue: String,
    val sourceFile: String,
)

/** Applies deterministic regex rules over already-extracted strings — never invents matches. */
interface SecretsScanner {
    fun scan(strings: List<Pair<String, String>>): List<SecretFinding>
}

class RegexSecretsScanner : SecretsScanner {

    private data class Rule(val kind: SecretKind, val pattern: Regex)

    private val rules = listOf(
        Rule(SecretKind.AWS_ACCESS_KEY, Regex("""\bAKIA[0-9A-Z]{16}\b""")),
        Rule(SecretKind.GOOGLE_API_KEY, Regex("""\bAIza[0-9A-Za-z_\-]{35}\b""")),
        Rule(SecretKind.SLACK_TOKEN, Regex("""\bxox[baprs]-[0-9A-Za-z-]{10,48}\b""")),
        Rule(SecretKind.STRIPE_KEY, Regex("""\b(?:sk|pk|rk)_(?:live|test)_[0-9A-Za-z]{16,99}\b""")),
        Rule(SecretKind.GITHUB_TOKEN, Regex("""\bgh[pousr]_[0-9A-Za-z]{20,255}\b""")),
        Rule(SecretKind.PRIVATE_KEY, Regex("""-----BEGIN (?:RSA |EC |DSA |OPENSSH )?PRIVATE KEY-----""")),
        Rule(SecretKind.JWT, Regex("""\bey[A-Za-z0-9_-]{10,}\.ey[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{5,}\b""")),
        Rule(SecretKind.BEARER_TOKEN, Regex("""(?i)\bAuthorization\s*:\s*Bearer\s+[A-Za-z0-9._\-]{8,}""")),
        Rule(SecretKind.BASIC_AUTH_URL, Regex("""[a-zA-Z][a-zA-Z0-9+.\-]*://[^/\s:@]+:[^/\s:@]+@[^/\s]+""")),
        Rule(SecretKind.FIREBASE_DATABASE_URL, Regex("""https://[a-zA-Z0-9-]+\.firebaseio\.com""")),
        Rule(
            SecretKind.GENERIC_API_KEY,
            Regex("""(?i)\b(api[_-]?key|apikey|secret[_-]?key|access[_-]?token|client[_-]?secret)\b\s*[:=]\s*["']?[A-Za-z0-9_\-]{12,64}["']?"""),
        ),
    )

    override fun scan(strings: List<Pair<String, String>>): List<SecretFinding> {
        val findings = mutableListOf<SecretFinding>()
        val seen = mutableSetOf<Triple<SecretKind, String, String>>()
        for ((sourceFile, text) in strings) {
            for (rule in rules) {
                val match = rule.pattern.find(text) ?: continue
                val value = match.value
                val key = Triple(rule.kind, value, sourceFile)
                if (!seen.add(key)) continue
                findings += SecretFinding(kind = rule.kind, maskedValue = mask(value), sourceFile = sourceFile)
            }
        }
        return findings
    }

    private fun mask(value: String): String {
        if (value.length <= 10) return value.take(2) + "*".repeat((value.length - 2).coerceAtLeast(0))
        return value.take(6) + "*".repeat(value.length - 10) + value.takeLast(4)
    }
}

/** Extracts human-readable strings from an APK's dex/native/resource files and flags likely secrets. */
interface ApkSecretsAnalyzer {
    suspend fun analyze(apkPath: String): Result<List<SecretFinding>>
}
