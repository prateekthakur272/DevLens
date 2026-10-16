package dev.prateekthakur.devlens.infrastructure.apk

/** Extracts printable ASCII runs from arbitrary binary data — the `strings` utility, in Kotlin. */
object BinaryStringExtractor {

    fun extract(bytes: ByteArray, minLength: Int = 6): List<String> {
        val results = mutableListOf<String>()
        val current = StringBuilder()
        for (byte in bytes) {
            val c = byte.toInt() and 0xFF
            if (c in 0x20..0x7E) {
                current.append(c.toChar())
            } else {
                if (current.length >= minLength) results += current.toString()
                current.setLength(0)
            }
        }
        if (current.length >= minLength) results += current.toString()
        return results
    }
}
