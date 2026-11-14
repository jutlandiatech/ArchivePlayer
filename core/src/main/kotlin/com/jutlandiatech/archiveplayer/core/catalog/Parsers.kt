package com.jutlandiatech.archiveplayer.core.catalog

import com.jutlandiatech.archiveplayer.core.language.LanguageNormalizer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal object JsonValues {
    fun parseObject(json: String): JsonObject? = try {
        Json.parseToJsonElement(json) as? JsonObject
    } catch (e: Exception) {
        null
    }

    /** All string values of a string-or-array field. */
    fun strings(element: JsonElement?): List<String> = when (element) {
        is JsonPrimitive -> if (element.isString || element.content != "null") listOf(element.content) else emptyList()
        is JsonArray -> element.flatMap { strings(it) }
        else -> emptyList()
    }

    fun first(element: JsonElement?): String? = strings(element).firstOrNull { it.isNotBlank() }?.trim()

    fun long(element: JsonElement?): Long? = first(element)?.toDoubleOrNull()?.toLong()
}

/** Media file formats archive.org uses for audio and video files. */
internal object MediaFormats {
    val audio = setOf(
        "vbr mp3", "mp3", "128kbps mp3", "64kbps mp3", "320kbps mp3", "256kbps mp3", "ogg vorbis", "flac",
        "24bit flac", "apple lossless audio", "wave", "aiff", "m4a", "aac",
    )
    val video = setOf(
        "h.264", "h.264 ia", "512kb mpeg4", "mpeg4", "mpeg2", "mpeg1", "ogg video", "cinepack", "quicktime",
        "matroska", "windows media", "avi", "divx", "h.264 hd",
    )

    fun isMedia(format: String): Boolean = format.trim().lowercase().let { it in audio || it in video }
}

/** Parses `https://archive.org/metadata/<id>` responses (R07–R13, R45, R78). Never throws. */
object MetadataParser {
    private class FileRec(val name: String, val format: String, val source: String, val original: String?,
                          val track: Int?, val title: String?, val length: String?, val size: Long?)

    /** Null for invalid JSON, empty responses or unsupported media types. */
    fun parse(json: String): ArchiveItem? = try {
        parseOrNull(json)
    } catch (e: Exception) {
        null
    }

    private fun parseOrNull(json: String): ArchiveItem? {
        val root = JsonValues.parseObject(json) ?: return null
        val meta = root["metadata"] as? JsonObject ?: return null
        val identifier = JsonValues.first(meta["identifier"]) ?: return null
        val kind = MediaKind.fromMediatype(JsonValues.first(meta["mediatype"])) ?: return null
        val files = (root["files"] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val name = JsonValues.first(o["name"]) ?: return@mapNotNull null
            FileRec(
                name = name,
                format = JsonValues.first(o["format"]) ?: "",
                source = JsonValues.first(o["source"]) ?: "",
                original = (o["original"] as? JsonPrimitive)?.content,
                track = JsonValues.first(o["track"])?.let { Regex("""\d+""").find(it)?.value?.toIntOrNull() },
                title = JsonValues.first(o["title"]),
                length = JsonValues.first(o["length"]),
                size = JsonValues.long(o["size"]),
            )
        }
        val byName = files.associateBy { it.name }
        fun root(file: FileRec): FileRec {
            var current = file
            val seen = mutableSetOf(current.name)
            while (true) {
                val parent = current.original?.let { byName[it] } ?: return current
                if (!seen.add(parent.name)) return current
                current = parent
            }
        }

        val media = files.filter { MediaFormats.isMedia(it.format) }
        val groups = media.groupBy { stem(root(it).name) }
        val subtitleFiles = files.mapNotNull { f -> SubtitleDetector.classify(f.name, f.format)?.let { f to it } }

        val unordered = groups.map { (stem, group) ->
            val ordered = group.sortedWith(compareBy<FileRec> { if (it.source == "original") 1 else 0 }.thenBy { it.name })
            val track = group.firstNotNullOfOrNull { it.track }
            val title = group.firstOrNull { it.source == "original" }?.title?.takeIf { it.isNotBlank() }
                ?: group.firstNotNullOfOrNull { it.title?.takeIf { t -> t.isNotBlank() } }
                ?: stem.substringAfterLast('/')
            Triple(stem, track, Pair(title, ordered))
        }.sortedWith(compareBy<Triple<String, Int?, Pair<String, List<FileRec>>>> { it.second ?: Int.MAX_VALUE }
            .thenComparator { a, b -> NaturalOrder.compare(a.first, b.first) })

        val singleEntry = unordered.size == 1
        val entries = unordered.mapIndexed { index, (stem, _, pair) ->
            val (title, ordered) = pair
            val subs = subtitleFiles.filter { (f, _) -> subtitleStem(f.name) == stem || singleEntry }.map { it.second }
            Entry(
                identifier = identifier,
                index = index,
                title = title,
                kind = kind,
                durationSeconds = (ordered.filter { it.source == "original" && it.length?.contains(':') == false } +
                    ordered.filter { it.length?.contains(':') == false } + ordered)
                    .firstNotNullOfOrNull { DurationParser.parseSeconds(it.length) },
                formats = ordered.map { MediaFormat(it.name, it.format, it.size) },
                subtitles = subs,
            )
        }

        val artwork = files.firstOrNull { it.name == "__ia_thumb.jpg" || it.format == "Item Tile" }?.name
            ?: files.firstOrNull { it.source == "original" && it.format.lowercase() in setOf("jpeg", "png") }?.name
        return ArchiveItem(
            identifier = identifier,
            title = JsonValues.first(meta["title"]) ?: identifier,
            creator = JsonValues.first(meta["creator"]),
            date = JsonValues.first(meta["date"]),
            kind = kind,
            languages = LanguageNormalizer.normalizeAll(JsonValues.strings(meta["language"])),
            collections = JsonValues.strings(meta["collection"]).map { it.trim() }.filter { it.isNotEmpty() },
            subjects = JsonValues.strings(meta["subject"]).flatMap { it.split(';') }.map { it.trim() }.filter { it.isNotEmpty() },
            description = JsonValues.strings(meta["description"]).joinToString("\n").takeIf { it.isNotBlank() }
                ?.let { HtmlSanitizer.toPlainText(it) },
            artworkFile = artwork,
            entries = entries,
        )
    }

    private val VARIANT_SUFFIX = Regex("""(_512kb|_64kb|_128kb|_vbr|\.ia|_hd)$""", RegexOption.IGNORE_CASE)

    internal fun stem(name: String): String = VARIANT_SUFFIX.replace(name.substringBeforeLast('.'), "")

    private fun subtitleStem(name: String): String {
        var parts = name.substringBeforeLast('.').split('.')
        while (parts.size > 1 && (parts.last().lowercase() in setOf("asr", "autogenerated") ||
                LanguageNormalizer.normalize(parts.last()) != null)) {
            parts = parts.dropLast(1)
        }
        return parts.joinToString(".")
    }
}

/** Natural ordering: digit runs compare numerically (`ep2` < `ep10`). */
object NaturalOrder : Comparator<String> {
    private val CHUNK = Regex("""\d+|\D+""")
    override fun compare(a: String, b: String): Int {
        val x = CHUNK.findAll(a.lowercase()).map { it.value }.toList()
        val y = CHUNK.findAll(b.lowercase()).map { it.value }.toList()
        for (i in 0 until minOf(x.size, y.size)) {
            val p = x[i]; val q = y[i]
            val c = if (p[0].isDigit() && q[0].isDigit()) {
                p.trimStart('0').length.compareTo(q.trimStart('0').length).takeIf { it != 0 }
                    ?: p.trimStart('0').compareTo(q.trimStart('0'))
            } else p.compareTo(q)
            if (c != 0) return c
        }
        return x.size.compareTo(y.size).takeIf { it != 0 } ?: a.compareTo(b)
    }
}

/** Parses `advancedsearch.php` JSON responses (R22–R25). Never throws; invalid input gives an empty page. */
object SearchResponseParser {
    fun parse(json: String): SearchPage = try {
        val response = JsonValues.parseObject(json)?.get("response") as? JsonObject
        val docs = response?.get("docs") as? JsonArray
        if (response == null || docs == null) SearchPage(0, emptyList()) else {
            val hits = docs.mapNotNull { el ->
                val d = el as? JsonObject ?: return@mapNotNull null
                val id = JsonValues.first(d["identifier"]) ?: return@mapNotNull null
                SearchHit(
                    identifier = id,
                    title = JsonValues.first(d["title"]) ?: id,
                    creator = JsonValues.first(d["creator"]),
                    kind = MediaKind.fromMediatype(JsonValues.first(d["mediatype"])),
                    languages = LanguageNormalizer.normalizeAll(JsonValues.strings(d["language"])),
                    collections = JsonValues.strings(d["collection"]),
                    subjects = JsonValues.strings(d["subject"]).flatMap { it.split(';') }.map { it.trim() }.filter { it.isNotEmpty() },
                    downloads = JsonValues.long(d["downloads"]) ?: 0L,
                    date = JsonValues.first(d["date"]) ?: JsonValues.first(d["year"]),
                )
            }
            SearchPage(JsonValues.long(response["numFound"])?.toInt() ?: hits.size, hits)
        }
    } catch (e: Exception) {
        SearchPage(0, emptyList())
    }
}

/** Parses archive.org website-search responses (`services/search/beta/page_production`, R87). Null means "fall back". */
object WebSearchResponseParser {
    fun parse(json: String): SearchPage? = try {
        val body = ((JsonValues.parseObject(json)?.get("response") as? JsonObject)?.get("body") as? JsonObject)?.get("hits") as? JsonObject
        val list = body?.get("hits") as? JsonArray
        if (body == null || list == null) null else {
            val hits = list.mapNotNull { el ->
                val d = (el as? JsonObject)?.get("fields") as? JsonObject ?: return@mapNotNull null
                val id = JsonValues.first(d["identifier"]) ?: return@mapNotNull null
                SearchHit(
                    identifier = id,
                    title = JsonValues.first(d["title"]) ?: id,
                    creator = JsonValues.first(d["creator"]),
                    kind = MediaKind.fromMediatype(JsonValues.first(d["mediatype"])),
                    languages = LanguageNormalizer.normalizeAll(JsonValues.strings(d["language"])),
                    collections = JsonValues.strings(d["collection"]),
                    subjects = JsonValues.strings(d["subject"]).flatMap { it.split(';') }.map { it.trim() }.filter { it.isNotEmpty() },
                    downloads = JsonValues.long(d["downloads"]) ?: 0L,
                    date = JsonValues.first(d["date"]) ?: JsonValues.first(d["year"]),
                )
            }
            SearchPage(JsonValues.long(body["total"])?.toInt() ?: hits.size, hits)
        }
    } catch (e: Exception) {
        null
    }
}

/** Subtitle-file classification (R45, A33, A40). */
object SubtitleDetector {
    /** Null if [fileName] is not a SubRip/WebVTT file. */
    fun classify(fileName: String, format: String?): SubtitleTrack? {
        val type = when (fileName.substringAfterLast('.', "").lowercase()) {
            "srt" -> SubtitleType.SRT
            "vtt" -> SubtitleType.VTT
            else -> return null
        }
        val parts = fileName.substringBeforeLast('.').split('.').drop(1).map { it.lowercase() }
        val auto = parts.any { it == "asr" || it == "autogenerated" }
        val language = parts.filterNot { it == "asr" || it == "autogenerated" }
            .lastOrNull()?.let { LanguageNormalizer.normalize(it) }
        return SubtitleTrack(fileName, type, language, auto)
    }
}

/** Turns archive.org description HTML into safe plain text (R12, R78). */
object HtmlSanitizer {
    private val BLOCKS = Regex("""<(script|style)[^>]*>.*?</\1\s*>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val BREAKS = Regex("""<br\s*/?>|</p\s*>|</div\s*>|</li\s*>""", RegexOption.IGNORE_CASE)
    private val TAGS = Regex("""<[^>]*>""")
    private val NUMERIC = Regex("""&#(x?)([0-9a-fA-F]+);""")
    private val NAMED = mapOf("&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&apos;" to "'", "&#39;" to "'", "&nbsp;" to " ")

    fun toPlainText(html: String): String {
        var text = BLOCKS.replace(html, "")
        text = BREAKS.replace(text, "\n")
        text = TAGS.replace(text, "")
        text = NUMERIC.replace(text) { m ->
            val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) 10 else 16)
            if (code == null || code !in 1..0x10FFFF) "" else String(Character.toChars(code))
        }
        NAMED.forEach { (entity, value) -> text = text.replace(entity, value) }
        return text.lines().joinToString("\n") { it.trimEnd() }.trim('\n', ' ')
    }
}

/** archive.org length values: seconds (`664.03`) or `mm:ss` / `hh:mm:ss`. Null when unparsable. */
object DurationParser {
    fun parseSeconds(raw: String?): Double? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (':' !in value) return value.toDoubleOrNull()?.takeIf { it >= 0 }
        val parts = value.split(':').map { it.toDoubleOrNull() ?: return null }
        if (parts.size > 3) return null
        return parts.fold(0.0) { acc, p -> acc * 60 + p }
    }
}
