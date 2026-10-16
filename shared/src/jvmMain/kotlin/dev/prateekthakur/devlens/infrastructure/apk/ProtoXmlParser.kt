package dev.prateekthakur.devlens.infrastructure.apk

/** Minimal protobuf wire-format reader shared by the manifest and resource-table decoders. */
internal class PbReader(val b: ByteArray, var pos: Int, val end: Int) {
    fun hasMore() = pos < end

    fun varint(): Long {
        var result = 0L
        var shift = 0
        while (true) {
            val byte = b[pos++].toInt()
            result = result or ((byte and 0x7F).toLong() shl shift)
            if (byte and 0x80 == 0) return result
            shift += 7
            require(shift < 70) { "Malformed varint" }
        }
    }

    /** Returns (fieldNumber, wireType). */
    fun tag(): Pair<Int, Int> {
        val t = varint()
        return (t ushr 3).toInt() to (t and 7).toInt()
    }

    fun lengthDelimited(): PbReader {
        val len = varint().toInt()
        require(len >= 0 && pos + len <= end) { "Malformed length" }
        val r = PbReader(b, pos, pos + len)
        pos += len
        return r
    }

    fun string(): String {
        val r = lengthDelimited()
        return String(b, r.pos, r.end - r.pos, Charsets.UTF_8)
    }

    fun skip(wire: Int) {
        when (wire) {
            0 -> varint()
            1 -> pos += 8
            2 -> lengthDelimited()
            5 -> pos += 4
            else -> error("Unsupported wire type $wire")
        }
    }
}

/**
 * Decoder for AAPT2's protobuf XML (`aapt.pb.XmlNode`), the format of manifests inside `.aab` files.
 * Only the fields needed to rebuild an element tree with attribute values are read.
 *
 *   XmlNode      { 1: XmlElement element, 2: string text }
 *   XmlElement   { 3: string name, 4: XmlAttribute attribute, 5: XmlNode child }
 *   XmlAttribute { 2: string name, 3: string value, 6: Item compiled_item }
 *   Item         { 1: Reference ref, 7: Primitive prim }   Reference { 3: string name }
 *   Primitive    { 6: int32 decimal, 7: uint32 hex, 8: bool }
 */
object ProtoXmlParser {

    fun parse(bytes: ByteArray): AaptXmlNode? {
        val node = readNode(PbReader(bytes, 0, bytes.size)) ?: return null
        return node.takeIf { it.name == "manifest" }
    }

    private fun readNode(r: PbReader): AaptXmlNode? {
        var element: AaptXmlNode? = null
        while (r.hasMore()) {
            val (field, wire) = r.tag()
            if (field == 1 && wire == 2) element = readElement(r.lengthDelimited()) else r.skip(wire)
        }
        return element
    }

    private fun readElement(r: PbReader): AaptXmlNode {
        var name = ""
        val attributes = mutableListOf<Pair<String, String>>()
        val children = mutableListOf<AaptXmlNode>()
        while (r.hasMore()) {
            val (field, wire) = r.tag()
            when {
                field == 3 && wire == 2 -> name = r.string()
                field == 4 && wire == 2 -> readAttribute(r.lengthDelimited())?.let { attributes += it }
                field == 5 && wire == 2 -> readNode(r.lengthDelimited())?.let { children += it }
                else -> r.skip(wire)
            }
        }
        return AaptXmlNode(name).also { n ->
            attributes.forEach { (k, v) -> n.attributes[k] = v }
            n.children += children
        }
    }

    private fun readAttribute(r: PbReader): Pair<String, String>? {
        var name = ""
        var value = ""
        var compiled: String? = null
        while (r.hasMore()) {
            val (field, wire) = r.tag()
            when {
                field == 2 && wire == 2 -> name = r.string()
                field == 3 && wire == 2 -> value = r.string()
                field == 6 && wire == 2 -> compiled = readItem(r.lengthDelimited())
                else -> r.skip(wire)
            }
        }
        if (name.isEmpty()) return null
        return name to value.ifEmpty { compiled.orEmpty() }
    }

    /** A compiled item as text: `@type/name` for references, otherwise the primitive's value. */
    private fun readItem(r: PbReader): String? {
        var result: String? = null
        while (r.hasMore()) {
            val (field, wire) = r.tag()
            when {
                field == 1 && wire == 2 -> {
                    val ref = r.lengthDelimited()
                    var refName: String? = null
                    var refId = 0L
                    while (ref.hasMore()) {
                        val (rf, rw) = ref.tag()
                        when {
                            rf == 3 && rw == 2 -> refName = ref.string()
                            rf == 2 && rw == 0 -> refId = ref.varint()
                            else -> ref.skip(rw)
                        }
                    }
                    result = refName?.takeIf { it.isNotEmpty() }?.let { "@$it" } ?: if (refId != 0L) "@id:0x" + refId.toString(16) else null
                }
                field == 7 && wire == 2 -> {
                    val p = r.lengthDelimited()
                    while (p.hasMore()) {
                        val (pf, pw) = p.tag()
                        when {
                            pf == 6 && pw == 0 -> result = p.varint().toInt().toString()
                            pf == 7 && pw == 0 -> result = "0x" + p.varint().toString(16)
                            pf == 8 && pw == 0 -> result = (p.varint() != 0L).toString()
                            else -> p.skip(pw)
                        }
                    }
                }
                else -> r.skip(wire)
            }
        }
        return result
    }
}

/**
 * Looks up the files behind a resource in a bundle's `resources.pb`, e.g. `mipmap/ic_launcher` -> `res/ee.png`.
 * Needed because release builds usually shorten resource paths, so the name cannot be guessed from the file name.
 *
 *   ResourceTable { 2: Package }  Package { 1: PackageId, 3: Type }  Type { 1: TypeId, 2: string name, 3: Entry }
 *   Entry { 1: EntryId, 2: string name, 6: ConfigValue }  ConfigValue { 2: Value }  Value { 4: Item }
 *   Item { 5: FileReference }  FileReference { 1: string path }
 */
object ResourceTableReader {

    /** Match by `type/name` (e.g. `mipmap`, `ic_launcher`) or by numeric resource id (0xPPTTEEEE). */
    class Query(val typeName: String? = null, val entryName: String? = null, val resourceId: Long? = null) {
        val pkgId get() = resourceId?.let { (it ushr 24).toInt() }
        val typeId get() = resourceId?.let { ((it ushr 16) and 0xFF).toInt() }
        val entryId get() = resourceId?.let { (it and 0xFFFF).toInt() }
    }

    fun filePaths(resourcesPb: ByteArray, query: Query): List<String> {
        val paths = mutableListOf<String>()
        val table = PbReader(resourcesPb, 0, resourcesPb.size)
        while (table.hasMore()) {
            val (f, w) = table.tag()
            if (f == 2 && w == 2) readPackage(table.lengthDelimited(), query, paths) else table.skip(w)
        }
        return paths
    }

    /** PackageId / TypeId / EntryId are messages of the form `{ uint32 id = 1 }`. */
    private fun readId(r: PbReader): Int {
        var id = 0 // proto3 omits zero, so an empty message means id 0
        while (r.hasMore()) {
            val (f, w) = r.tag()
            if (f == 1 && w == 0) id = r.varint().toInt() else r.skip(w)
        }
        return id
    }

    private fun readPackage(r: PbReader, q: Query, out: MutableList<String>) {
        var id = 0
        val types = mutableListOf<PbReader>()
        while (r.hasMore()) {
            val (f, w) = r.tag()
            when {
                f == 1 && w == 2 -> id = readId(r.lengthDelimited())
                f == 3 && w == 2 -> types += r.lengthDelimited()
                else -> r.skip(w)
            }
        }
        if (q.pkgId != null && q.pkgId != id) return
        for (t in types) readType(t, q, out)
    }

    private fun readType(r: PbReader, q: Query, out: MutableList<String>) {
        var id = 0
        var name = ""
        val entries = mutableListOf<PbReader>()
        while (r.hasMore()) {
            val (f, w) = r.tag()
            when {
                f == 1 && w == 2 -> id = readId(r.lengthDelimited())
                f == 2 && w == 2 -> name = r.string()
                f == 3 && w == 2 -> entries += r.lengthDelimited()
                else -> r.skip(w)
            }
        }
        if (q.typeName != null && q.typeName != name) return
        if (q.typeId != null && q.typeId != id) return
        for (e in entries) readEntry(e, q, out)
    }

    private fun readEntry(r: PbReader, q: Query, out: MutableList<String>) {
        var id = 0
        var name = ""
        val configs = mutableListOf<PbReader>()
        while (r.hasMore()) {
            val (f, w) = r.tag()
            when {
                f == 1 && w == 2 -> id = readId(r.lengthDelimited())
                f == 2 && w == 2 -> name = r.string()
                f == 6 && w == 2 -> configs += r.lengthDelimited()
                else -> r.skip(w)
            }
        }
        if (q.entryName != null && q.entryName != name) return
        if (q.entryId != null && q.entryId != id) return
        for (c in configs) {
            while (c.hasMore()) {
                val (f, w) = c.tag()
                if (f == 2 && w == 2) readValue(c.lengthDelimited(), out) else c.skip(w)
            }
        }
    }

    private fun readValue(r: PbReader, out: MutableList<String>) {
        while (r.hasMore()) {
            val (f, w) = r.tag()
            if (f == 4 && w == 2) {
                val item = r.lengthDelimited()
                while (item.hasMore()) {
                    val (itf, itw) = item.tag()
                    if (itf == 5 && itw == 2) {
                        val file = item.lengthDelimited()
                        while (file.hasMore()) {
                            val (ff, fw) = file.tag()
                            if (ff == 1 && fw == 2) out += file.string() else file.skip(fw)
                        }
                    } else item.skip(itw)
                }
            } else r.skip(w)
        }
    }
}
