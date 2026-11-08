package com.jutlandiatech.archiveplayer.core.playlist

import com.jutlandiatech.archiveplayer.core.settings.AppSettings
import com.jutlandiatech.archiveplayer.core.settings.DownloadLocation
import com.jutlandiatech.archiveplayer.core.settings.SubtitleDefault
import com.jutlandiatech.archiveplayer.core.settings.ThemeMode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.Deflater
import java.util.zip.Inflater

data class EntryRef(val identifier: String, val fileName: String, val title: String? = null)

data class Playlist(
    val id: String,
    val title: String,
    val description: String?,
    val entries: List<EntryRef>,
    val isLiked: Boolean = false,
)

data class SharedPlaylist(val title: String, val description: String?, val entries: List<EntryRef>)

sealed interface ShareResult {
    data class Link(val url: String) : ShareResult
    data class FileRequired(val linkLength: Int) : ShareResult
}

private fun JsonElement?.str(): String? = (this as? JsonPrimitive)?.takeIf { it.isString }?.content

private fun parseObject(json: String): JsonObject? = try {
    Json.parseToJsonElement(json) as? JsonObject
} catch (e: Exception) {
    null
}

/** GitHub Pages share links with a versioned, compressed fragment payload (R54–R57, A21, A23). */
object ShareLinkCodec {
    private const val VERSION = "v1"
    private const val PAGE = "p"
    val baseUrl: String get() = "https://jutlandiatech.github.io/ArchivePlayer/"
    val maxLength: Int get() = 8000

    fun share(playlist: Playlist): ShareResult {
        val url = encode(SharedPlaylist(playlist.title, playlist.description, playlist.entries))
        return if (url.length <= maxLength) ShareResult.Link(url) else ShareResult.FileRequired(url.length)
    }

    fun encode(shared: SharedPlaylist, base: String = baseUrl): String {
        val json = buildJsonObject {
            put("t", shared.title)
            shared.description?.let { put("d", it) }
            put("e", buildJsonArray {
                shared.entries.forEach { e ->
                    add(buildJsonArray {
                        add(JsonPrimitive(e.identifier)); add(JsonPrimitive(e.fileName))
                        e.title?.let { add(JsonPrimitive(it)) }
                    })
                }
            })
        }.toString()
        val deflater = Deflater(Deflater.BEST_COMPRESSION, true)
        deflater.setInput(json.toByteArray(Charsets.UTF_8))
        deflater.finish()
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!deflater.finished()) out.write(buffer, 0, deflater.deflate(buffer))
        deflater.end()
        val payload = Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray())
        return base.trimEnd('/') + "/$PAGE#$VERSION.$payload"
    }

    /** Null for foreign URLs, unknown payload versions or corrupt payloads. */
    fun decode(url: String, base: String = baseUrl): SharedPlaylist? = try {
        val page = url.substringBefore('#').trimEnd('/')
        val root = base.trimEnd('/')
        if ((page != root && page != "$root/$PAGE") || '#' !in url) null else {
            val fragment = url.substringAfter('#')
            if (!fragment.startsWith("$VERSION.")) null else {
                val bytes = Base64.getUrlDecoder().decode(fragment.removePrefix("$VERSION."))
                val inflater = Inflater(true)
                inflater.setInput(bytes)
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (!inflater.finished()) {
                    val n = inflater.inflate(buffer)
                    if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) break
                    out.write(buffer, 0, n)
                }
                inflater.end()
                val o = parseObject(out.toString(Charsets.UTF_8.name()))!!
                SharedPlaylist(
                    title = o["t"].str()!!,
                    description = o["d"].str(),
                    entries = (o["e"] as JsonArray).map { el ->
                        val a = el as JsonArray
                        EntryRef(a[0].str()!!, a[1].str()!!, a.getOrNull(2).str())
                    },
                )
            }
        }
    } catch (e: Exception) {
        null
    }
}

data class ImportReport(val playlist: Playlist?, val skipped: List<String>)

internal object EntryJson {
    fun encode(e: EntryRef) = buildJsonObject {
        put("identifier", e.identifier); put("fileName", e.fileName); e.title?.let { put("title", it) }
    }

    fun decode(el: JsonElement): EntryRef? {
        val o = el as? JsonObject ?: return null
        val id = o["identifier"].str()?.takeIf { it.isNotBlank() } ?: return null
        val file = o["fileName"].str()?.takeIf { it.isNotBlank() } ?: return null
        return EntryRef(id, file, o["title"].str())
    }

    fun encodePlaylist(p: Playlist, withId: Boolean) = buildJsonObject {
        if (withId) { put("id", p.id); put("isLiked", p.isLiked) }
        put("title", p.title)
        p.description?.let { put("description", it) }
        put("entries", JsonArray(p.entries.map(::encode)))
    }
}

/** `.archiveplaylist` files (R58, A24). */
object PlaylistFileCodec {
    const val FORMAT = "archiveplaylist"
    const val SCHEMA = 1
    val extension: String get() = FORMAT

    fun encode(playlist: Playlist): String = JsonObject(
        mapOf("format" to JsonPrimitive(FORMAT), "schemaVersion" to JsonPrimitive(SCHEMA)) +
            EntryJson.encodePlaylist(playlist, withId = false),
    ).toString()

    /** Always creates a new playlist with [newId]; invalid entries are reported in [ImportReport.skipped]. */
    fun decode(json: String, newId: String): ImportReport {
        val o = parseObject(json) ?: return ImportReport(null, emptyList())
        if (o["format"].str() != FORMAT || (o["schemaVersion"] as? JsonPrimitive)?.intOrNull != SCHEMA) {
            return ImportReport(null, emptyList())
        }
        val title = o["title"].str()?.takeIf { it.isNotBlank() } ?: return ImportReport(null, emptyList())
        val raw = o["entries"] as? JsonArray ?: return ImportReport(null, emptyList())
        val skipped = mutableListOf<String>()
        val entries = raw.mapIndexedNotNull { i, el ->
            EntryJson.decode(el) ?: run { skipped += "entry ${i + 1}: $el"; null }
        }
        return ImportReport(Playlist(newId, title, o["description"].str(), entries), skipped)
    }
}

class LikedListException(message: String) : RuntimeException(message)

/** Playlist editing rules (R51, R52). */
object PlaylistRules {
    fun rename(playlist: Playlist, title: String): Playlist {
        if (playlist.isLiked) throw LikedListException("The liked list cannot be renamed")
        require(title.isNotBlank()) { "Playlist title must not be blank" }
        return playlist.copy(title = title.trim())
    }

    fun canDelete(playlist: Playlist): Boolean = !playlist.isLiked

    fun reorder(playlist: Playlist, from: Int, to: Int): Playlist {
        val list = playlist.entries.toMutableList()
        if (from !in list.indices || to !in list.indices) return playlist
        list.add(to, list.removeAt(from))
        return playlist.copy(entries = list)
    }

    fun remove(playlist: Playlist, index: Int): Playlist =
        if (index !in playlist.entries.indices) playlist
        else playlist.copy(entries = playlist.entries.filterIndexed { i, _ -> i != index })

    fun addItemEntries(playlist: Playlist, entries: List<EntryRef>): Playlist = playlist.copy(entries = playlist.entries + entries)
}

data class LibraryHistoryRecord(val ref: EntryRef, val playedAtMs: Long, val positionMs: Long)

data class Library(
    val playlists: List<Playlist>,
    val likes: List<EntryRef>,
    val follows: List<String>,
    val history: List<LibraryHistoryRecord>,
    val settings: AppSettings?,
)

class InvalidLibraryException(message: String) : RuntimeException(message)

/** Full-library export/import (R74, A29); a superset of the playlist file format (R58). */
object LibraryCodec {
    const val FORMAT = "archiveplayer-library"
    const val SCHEMA = 1

    private fun settingsJson(s: AppSettings) = buildJsonObject {
        put("appLanguage", s.appLanguage)
        put("contentLanguages", JsonArray(s.contentLanguages.sorted().map { JsonPrimitive(it) }))
        put("theme", s.theme.name)
        put("dataSaver", s.dataSaver)
        put("wifiOnly", s.wifiOnly)
        put("showAdultContent", s.showAdultContent)
        put("downloadLocation", s.downloadLocation.name)
        put("subtitleDefault", s.subtitleDefault.name)
        put("includeAutoSubtitles", s.includeAutoSubtitles)
        put("historyPaused", s.historyPaused)
    }

    private fun bool(o: JsonObject, k: String) = (o[k] as? JsonPrimitive)?.booleanOrNull ?: throw InvalidLibraryException("bad $k")

    private fun settingsFrom(o: JsonObject) = AppSettings(
        appLanguage = o["appLanguage"].str() ?: throw InvalidLibraryException("bad appLanguage"),
        contentLanguages = (o["contentLanguages"] as JsonArray).mapNotNull { it.str() }.toSet(),
        theme = ThemeMode.valueOf(o["theme"].str()!!),
        dataSaver = bool(o, "dataSaver"),
        wifiOnly = bool(o, "wifiOnly"),
        showAdultContent = bool(o, "showAdultContent"),
        downloadLocation = DownloadLocation.valueOf(o["downloadLocation"].str()!!),
        subtitleDefault = SubtitleDefault.valueOf(o["subtitleDefault"].str()!!),
        includeAutoSubtitles = bool(o, "includeAutoSubtitles"),
        historyPaused = bool(o, "historyPaused"),
    )

    fun encode(library: Library): String = buildJsonObject {
        put("format", FORMAT)
        put("schemaVersion", SCHEMA)
        put("playlists", JsonArray(library.playlists.map { EntryJson.encodePlaylist(it, withId = true) }))
        put("likes", JsonArray(library.likes.map(EntryJson::encode)))
        put("follows", JsonArray(library.follows.map { JsonPrimitive(it) }))
        put("history", JsonArray(library.history.map { h ->
            buildJsonObject { put("entry", EntryJson.encode(h.ref)); put("playedAtMs", h.playedAtMs); put("positionMs", h.positionMs) }
        }))
        put("settings", library.settings?.let(::settingsJson) ?: JsonNull)
    }.toString()

    /** Throws [InvalidLibraryException] for anything that is not a complete library file. */
    fun decode(json: String): Library = try {
        val o = parseObject(json) ?: throw InvalidLibraryException("not JSON")
        if (o["format"].str() != FORMAT || (o["schemaVersion"] as? JsonPrimitive)?.intOrNull != SCHEMA) {
            throw InvalidLibraryException("unsupported library file")
        }
        Library(
            playlists = (o["playlists"] as JsonArray).map { el ->
                val p = el as JsonObject
                Playlist(p["id"].str()!!, p["title"].str()!!, p["description"].str(),
                    (p["entries"] as JsonArray).mapNotNull(EntryJson::decode),
                    (p["isLiked"] as? JsonPrimitive)?.booleanOrNull ?: false)
            },
            likes = (o["likes"] as JsonArray).mapNotNull(EntryJson::decode),
            follows = (o["follows"] as JsonArray).mapNotNull { it.str() },
            history = (o["history"] as JsonArray).map { el ->
                val h = el as JsonObject
                LibraryHistoryRecord(EntryJson.decode(h["entry"]!!)!!, (h["playedAtMs"] as JsonPrimitive).longOrNull!!,
                    (h["positionMs"] as JsonPrimitive).longOrNull!!)
            },
            settings = (o["settings"] as? JsonObject)?.let(::settingsFrom),
        )
    } catch (e: InvalidLibraryException) {
        throw e
    } catch (e: Exception) {
        throw InvalidLibraryException("corrupt library file: ${e.message}")
    }
}

data class MergeResult(val library: Library, val conflicts: List<String>)

object LibraryMerger {
    /** Merges without silent overwrite: same-ID playlists with different content are kept as copies and reported. */
    fun merge(local: Library, imported: Library): MergeResult {
        val playlists = local.playlists.toMutableList()
        val conflicts = mutableListOf<String>()
        for (p in imported.playlists) {
            val existing = playlists.firstOrNull { it.id == p.id }
            when {
                existing == null -> playlists += p
                existing == p -> Unit
                p.isLiked || existing.isLiked -> {
                    val merged = (existing.entries + p.entries).distinct()
                    playlists[playlists.indexOf(existing)] = existing.copy(entries = merged)
                }
                else -> {
                    var n = 1
                    while (playlists.any { it.id == "${p.id}-import-$n" }) n++
                    playlists += p.copy(id = "${p.id}-import-$n")
                    conflicts += "Playlist '${existing.title}' differs from imported '${p.title}'; kept both"
                }
            }
        }
        return MergeResult(
            Library(
                playlists = playlists,
                likes = (local.likes + imported.likes).distinct(),
                follows = (local.follows + imported.follows).distinct(),
                history = (local.history + imported.history).distinct().sortedByDescending { it.playedAtMs },
                settings = local.settings ?: imported.settings,
            ),
            conflicts,
        )
    }
}
