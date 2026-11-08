package com.thomasnorlund.archiveplayer.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.thomasnorlund.archiveplayer.core.home.HistoryEvent
import com.thomasnorlund.archiveplayer.core.playlist.EntryRef
import com.thomasnorlund.archiveplayer.core.playlist.Playlist
import com.thomasnorlund.archiveplayer.ui.ElementIds
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** On-device library: playlists (liked first), follows, history and recent searches (R44, R47, R51). */
data class LibraryState(
    val playlists: List<Playlist> = listOf(Playlist(ElementIds.LIKED_PLAYLIST_ID, "", null, emptyList(), isLiked = true)),
    val follows: List<String> = emptyList(),
    val followCheckMs: Long? = null,
    val history: List<HistoryEvent> = emptyList(),
    val recentSearches: List<String> = emptyList(),
) {
    val liked: Playlist get() = playlists.first { it.isLiked }
}

class LibraryStore(context: Context) {
    private val file = File(context.filesDir, "library.json")

    var state: LibraryState by mutableStateOf(load())
        private set

    fun update(transform: (LibraryState) -> LibraryState) {
        synchronized(this) {
            state = transform(state)
            file.writeText(encode(state).toString())
        }
    }

    private fun load(): LibraryState = runCatching { decode(JSONObject(file.readText())) }.getOrNull() ?: LibraryState()

    private fun ref(e: EntryRef) = JSONObject().put("identifier", e.identifier).put("file", e.fileName).put("title", e.title)
    private fun ref(o: JSONObject) = EntryRef(o.getString("identifier"), o.getString("file"), o.optString("title").takeIf { o.has("title") && it.isNotEmpty() })
    private fun strings(a: JSONArray?) = (0 until (a?.length() ?: 0)).map { a!!.getString(it) }
    private fun objects(a: JSONArray?) = (0 until (a?.length() ?: 0)).map { a!!.getJSONObject(it) }

    private fun encode(s: LibraryState) = JSONObject()
        .put("playlists", JSONArray(s.playlists.map { p ->
            JSONObject().put("id", p.id).put("title", p.title).put("description", p.description).put("liked", p.isLiked)
                .put("entries", JSONArray(p.entries.map(::ref)))
        }))
        .put("follows", JSONArray(s.follows))
        .put("followCheckMs", s.followCheckMs ?: JSONObject.NULL)
        .put("history", JSONArray(s.history.map { h ->
            JSONObject().put("ref", ref(h.ref)).put("item", h.itemIdentifier).put("itemTitle", h.itemTitle).put("creator", h.creator)
                .put("collections", JSONArray(h.collections)).put("subjects", JSONArray(h.subjects))
                .put("playedAt", h.playedAtMs).put("position", h.positionMs).put("duration", h.durationMs)
        }))
        .put("recentSearches", JSONArray(s.recentSearches))

    private fun decode(o: JSONObject) = LibraryState(
        playlists = objects(o.optJSONArray("playlists")).map { p ->
            Playlist(p.getString("id"), p.optString("title"), p.optString("description").takeIf { p.has("description") && it.isNotEmpty() },
                objects(p.optJSONArray("entries")).map(::ref), p.optBoolean("liked"))
        }.ifEmpty { LibraryState().playlists },
        follows = strings(o.optJSONArray("follows")),
        followCheckMs = if (o.isNull("followCheckMs")) null else o.optLong("followCheckMs"),
        history = objects(o.optJSONArray("history")).map { h ->
            HistoryEvent(ref(h.getJSONObject("ref")), h.getString("item"), h.optString("itemTitle"), h.optString("creator").takeIf { h.has("creator") && it.isNotEmpty() },
                strings(h.optJSONArray("collections")), strings(h.optJSONArray("subjects")), h.optLong("playedAt"), h.optLong("position"), h.optLong("duration"))
        },
        recentSearches = strings(o.optJSONArray("recentSearches")),
    )
}
