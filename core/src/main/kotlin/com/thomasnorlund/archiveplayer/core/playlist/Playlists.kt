package com.thomasnorlund.archiveplayer.core.playlist

import com.thomasnorlund.archiveplayer.core.settings.AppSettings

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

/** GitHub Pages share links with a versioned, compressed fragment payload (R54–R57, A21, A23). */
object ShareLinkCodec {
    val baseUrl: String get() = TODO()
    val maxLength: Int get() = TODO()
    fun share(playlist: Playlist): ShareResult = TODO()
    fun encode(shared: SharedPlaylist): String = TODO()

    /** Null for foreign URLs, unknown payload versions or corrupt payloads. */
    fun decode(url: String): SharedPlaylist? = TODO()
}

data class ImportReport(val playlist: Playlist?, val skipped: List<String>)

/** `.archiveplaylist` files (R58, A24). */
object PlaylistFileCodec {
    val extension: String get() = TODO()
    fun encode(playlist: Playlist): String = TODO()

    /** Always creates a new playlist with [newId]; invalid entries are reported in [ImportReport.skipped]. */
    fun decode(json: String, newId: String): ImportReport = TODO()
}

class LikedListException(message: String) : RuntimeException(message)

/** Playlist editing rules (R51, R52). */
object PlaylistRules {
    fun rename(playlist: Playlist, title: String): Playlist = TODO()
    fun canDelete(playlist: Playlist): Boolean = TODO()
    fun reorder(playlist: Playlist, from: Int, to: Int): Playlist = TODO()
    fun remove(playlist: Playlist, index: Int): Playlist = TODO()
    fun addItemEntries(playlist: Playlist, entries: List<EntryRef>): Playlist = TODO()
}

data class LibraryHistoryRecord(val ref: EntryRef, val playedAtMs: Long, val positionMs: Long)

data class Library(
    val playlists: List<Playlist>,
    val likes: List<EntryRef>,
    val follows: List<String>,
    val history: List<LibraryHistoryRecord>,
    val settings: AppSettings?,
)

/** Full-library export/import (R74, A29); a superset of the playlist file format (R58). */
object LibraryCodec {
    fun encode(library: Library): String = TODO()
    fun decode(json: String): Library = TODO()
}

data class MergeResult(val library: Library, val conflicts: List<String>)

object LibraryMerger {
    /** Merges without silent overwrite: same-ID playlists with different content are kept as copies and reported. */
    fun merge(local: Library, imported: Library): MergeResult = TODO()
}
