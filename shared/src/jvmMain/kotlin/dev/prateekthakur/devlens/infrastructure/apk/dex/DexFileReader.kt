package dev.prateekthakur.devlens.infrastructure.apk.dex

import dev.prateekthakur.devlens.domain.dex.DexClassInfo

/**
 * Minimal, read-only parser for the class_def_item table of a DEX file
 * (https://source.android.com/docs/core/runtime/dex-format). Treats the file as untrusted
 * binary input (PRD §40): every offset is bounds-checked before use.
 *
 * Only class-level metadata is read (name, superclass, interfaces, field/method *counts*) —
 * full bytecode disassembly is deliberately out of scope; this is a structure browser, not a
 * decompiler.
 */
object DexFileReader {

    private const val ACC_PUBLIC = 0x1
    private const val ACC_PRIVATE = 0x2
    private const val ACC_PROTECTED = 0x4
    private const val ACC_INTERFACE = 0x200
    private const val ACC_ABSTRACT = 0x400
    private const val ACC_ENUM = 0x4000
    private const val NO_INDEX = -1 // 0xffffffff read into a signed Int

    fun readClasses(bytes: ByteArray, dexFileName: String): List<DexClassInfo> {
        if (bytes.size < 0x70 || !isDexMagic(bytes)) return emptyList()

        val stringIdsSize = u32(bytes, 0x38)
        val stringIdsOff = u32(bytes, 0x3C)
        val typeIdsSize = u32(bytes, 0x40)
        val typeIdsOff = u32(bytes, 0x44)
        val classDefsSize = u32(bytes, 0x60)
        val classDefsOff = u32(bytes, 0x64)

        if (!inBounds(bytes, classDefsOff, classDefsSize * 32L)) return emptyList()

        fun stringAt(index: Int): String? {
            if (index < 0 || index >= stringIdsSize) return null
            val idOff = stringIdsOff + index * 4
            if (!inBounds(bytes, idOff, 4)) return null
            val dataOff = u32(bytes, idOff)
            return readMutf8(bytes, dataOff)
        }

        fun typeDescriptorAt(index: Int): String? {
            if (index == NO_INDEX || index < 0 || index >= typeIdsSize) return null
            val idOff = typeIdsOff + index * 4
            if (!inBounds(bytes, idOff, 4)) return null
            val descriptorIdx = u32(bytes, idOff)
            return stringAt(descriptorIdx)
        }

        fun typeList(offset: Int): List<String> {
            if (offset == 0 || !inBounds(bytes, offset, 4)) return emptyList()
            val size = u32(bytes, offset)
            val result = ArrayList<String>(size)
            for (i in 0 until size) {
                val entryOff = offset + 4 + i * 2
                if (!inBounds(bytes, entryOff, 2)) break
                val typeIdx = u16(bytes, entryOff)
                typeDescriptorAt(typeIdx)?.let { result += humanReadable(it) }
            }
            return result
        }

        val classes = ArrayList<DexClassInfo>(classDefsSize)
        for (i in 0 until classDefsSize) {
            val base = classDefsOff + i * 32
            if (!inBounds(bytes, base, 32)) break

            val classIdx = u32(bytes, base)
            val accessFlags = u32(bytes, base + 4)
            val superclassIdx = u32Signed(bytes, base + 8)
            val interfacesOff = u32(bytes, base + 12)
            val classDataOff = u32(bytes, base + 24)

            val descriptor = typeDescriptorAt(classIdx) ?: continue
            val readableDescriptor = humanReadable(descriptor)
            val superclass = if (superclassIdx == NO_INDEX) null else typeDescriptorAt(superclassIdx)?.let { humanReadable(it) }
            val interfaces = typeList(interfacesOff)

            var staticFields = 0
            var instanceFields = 0
            var directMethods = 0
            var virtualMethods = 0
            if (classDataOff != 0 && inBounds(bytes, classDataOff, 1)) {
                var pos = classDataOff
                val a = readUleb128(bytes, pos); staticFields = a.first; pos = a.second
                val b = readUleb128(bytes, pos); instanceFields = b.first; pos = b.second
                val c = readUleb128(bytes, pos); directMethods = c.first; pos = c.second
                val d = readUleb128(bytes, pos); virtualMethods = d.first
            }

            val dotName = readableDescriptor
            val lastDot = dotName.lastIndexOf('.')
            val packageName = if (lastDot >= 0) dotName.substring(0, lastDot) else ""
            val simpleName = if (lastDot >= 0) dotName.substring(lastDot + 1) else dotName

            classes += DexClassInfo(
                descriptor = dotName,
                packageName = packageName,
                simpleName = simpleName,
                superclass = superclass,
                interfaces = interfaces,
                isInterface = accessFlags and ACC_INTERFACE != 0,
                isAbstract = accessFlags and ACC_ABSTRACT != 0,
                isEnum = accessFlags and ACC_ENUM != 0,
                visibility = when {
                    accessFlags and ACC_PUBLIC != 0 -> "public"
                    accessFlags and ACC_PRIVATE != 0 -> "private"
                    accessFlags and ACC_PROTECTED != 0 -> "protected"
                    else -> "package-private"
                },
                fieldCount = staticFields + instanceFields,
                methodCount = directMethods + virtualMethods,
                dexFile = dexFileName,
            )
        }
        return classes
    }

    private fun isDexMagic(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 'd'.code.toByte() && bytes[1] == 'e'.code.toByte() && bytes[2] == 'x'.code.toByte()

    private fun inBounds(bytes: ByteArray, offset: Int, length: Long): Boolean =
        offset >= 0 && length >= 0 && offset.toLong() + length <= bytes.size

    private fun u16(bytes: ByteArray, offset: Int): Int =
        (bytes[offset].toInt() and 0xFF) or ((bytes[offset + 1].toInt() and 0xFF) shl 8)

    private fun u32(bytes: ByteArray, offset: Int): Int {
        val v = (bytes[offset].toLong() and 0xFF) or
            ((bytes[offset + 1].toLong() and 0xFF) shl 8) or
            ((bytes[offset + 2].toLong() and 0xFF) shl 16) or
            ((bytes[offset + 3].toLong() and 0xFF) shl 24)
        return v.toInt()
    }

    /** Same bit pattern as [u32] but returns -1 for the DEX "NO_INDEX" sentinel (0xffffffff). */
    private fun u32Signed(bytes: ByteArray, offset: Int): Int = u32(bytes, offset)

    private fun readUleb128(bytes: ByteArray, offset: Int): Pair<Int, Int> {
        var result = 0
        var shift = 0
        var pos = offset
        while (pos < bytes.size) {
            val b = bytes[pos].toInt() and 0xFF
            result = result or ((b and 0x7F) shl shift)
            pos++
            if (b and 0x80 == 0) break
            shift += 7
            if (shift > 35) break
        }
        return result to pos
    }

    private fun readMutf8(bytes: ByteArray, dataOff: Int): String? {
        if (!inBounds(bytes, dataOff, 1)) return null
        val (_, stringStart) = readUleb128(bytes, dataOff) // utf16_size, unused for decoding
        var pos = stringStart
        val out = StringBuilder()
        while (pos < bytes.size) {
            val b0 = bytes[pos].toInt() and 0xFF
            if (b0 == 0) break
            when {
                b0 and 0x80 == 0 -> {
                    out.append(b0.toChar())
                    pos += 1
                }
                b0 and 0xE0 == 0xC0 && pos + 1 < bytes.size -> {
                    val b1 = bytes[pos + 1].toInt() and 0xFF
                    out.append((((b0 and 0x1F) shl 6) or (b1 and 0x3F)).toChar())
                    pos += 2
                }
                b0 and 0xF0 == 0xE0 && pos + 2 < bytes.size -> {
                    val b1 = bytes[pos + 1].toInt() and 0xFF
                    val b2 = bytes[pos + 2].toInt() and 0xFF
                    out.append((((b0 and 0x0F) shl 12) or ((b1 and 0x3F) shl 6) or (b2 and 0x3F)).toChar())
                    pos += 3
                }
                else -> pos += 1
            }
        }
        return out.toString()
    }

    /** "Lcom/example/Foo;" -> "com.example.Foo"; array/primitive descriptors pass through readably. */
    private fun humanReadable(descriptor: String): String = when {
        descriptor.startsWith("L") && descriptor.endsWith(";") ->
            descriptor.substring(1, descriptor.length - 1).replace('/', '.')
        descriptor.startsWith("[") -> descriptor.trimStart('[').let { inner ->
            val depth = descriptor.length - inner.length
            humanReadable(inner) + "[]".repeat(depth)
        }
        else -> primitiveName(descriptor) ?: descriptor
    }

    private fun primitiveName(descriptor: String): String? = when (descriptor) {
        "V" -> "void"
        "Z" -> "boolean"
        "B" -> "byte"
        "S" -> "short"
        "C" -> "char"
        "I" -> "int"
        "J" -> "long"
        "F" -> "float"
        "D" -> "double"
        else -> null
    }
}
