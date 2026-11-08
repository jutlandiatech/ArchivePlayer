package com.thomasnorlund.archiveplayer

import com.thomasnorlund.archiveplayer.ui.ref
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.thomasnorlund.archiveplayer.data.ArchiveRepository
import com.thomasnorlund.archiveplayer.data.LibraryStore
import com.thomasnorlund.archiveplayer.ui.SearchController
import com.thomasnorlund.archiveplayer.core.settings.AppSettings
import com.thomasnorlund.archiveplayer.core.settings.ThemeResolver
import com.thomasnorlund.archiveplayer.core.setup.SetupFlow
import com.thomasnorlund.archiveplayer.data.SettingsStore
import com.thomasnorlund.archiveplayer.ui.AppTexts
import java.util.Locale

enum class Tab { HOME, SEARCH, LIBRARY }

/** A destination pushed onto a tab's back stack (UI_SHELL_SPEC.md → Top-level structure). */
sealed interface Screen {
    data object Settings : Screen
    data class SettingsDetail(val row: String) : Screen
    data class Item(val identifier: String) : Screen
    data class Collection(val identifier: String) : Screen
    data class Category(val categoryId: String) : Screen
    data class Playlist(val playlistId: String) : Screen
}

/** Survives configuration changes and activity recreation; owns settings, setup and navigation state. */
class AppViewModel(application: Application) : AndroidViewModel(application) {
    val texts: AppTexts = AppTexts.load(application)
    private val store = SettingsStore(application)
    val repo = ArchiveRepository(application)
    val library = LibraryStore(application)
    val phoneLanguage: String = Locale.getDefault().toLanguageTag()

    var settings: AppSettings? by mutableStateOf(store.load())
        private set

    val setup = SetupFlow(phoneLanguage, texts.isoCodes)
    /** Bumped on every setup action so Compose re-reads [setup]. */
    var setupVersion by mutableIntStateOf(0)
        private set

    fun onSetup(action: SetupFlow.() -> Unit) {
        setup.action()
        setupVersion++
    }

    fun startApp() {
        val result = setup.start() ?: return
        updateSettings(result)
    }

    fun updateSettings(new: AppSettings) {
        store.save(new)
        settings = new
    }

    fun toggleTheme(systemDark: Boolean) {
        val s = settings ?: return
        updateSettings(s.copy(theme = ThemeResolver.toggle(s.theme, systemDark)))
    }

    val search = SearchController(viewModelScope, repo, library, { settings?.showAdultContent == true }, { settings?.appLanguage ?: "en" })

    var tab by mutableStateOf(Tab.HOME)
        private set
    private val stacks: Map<Tab, SnapshotStateList<Screen>> = Tab.entries.associateWith { mutableStateListOf() }
    fun stack(tab: Tab): List<Screen> = stacks.getValue(tab)
    val current: Screen? get() = stacks.getValue(tab).lastOrNull()

    /** Reselecting the active tab pops to its root (N04). */
    fun selectTab(target: Tab) {
        if (target == tab) stacks.getValue(tab).clear() else tab = target
    }

    fun push(screen: Screen) {
        stacks.getValue(tab).add(screen)
    }

    /** Android/in-app Back: pop within the tab, then return to Home; false when the app should close (N05). */
    fun back(): Boolean {
        val stack = stacks.getValue(tab)
        return when {
            stack.isNotEmpty() -> { stack.removeAt(stack.lastIndex); true }
            tab != Tab.HOME -> { tab = Tab.HOME; true }
            else -> false
        }
    }

    // Playback/downloads are wired by the playback slice; these entry points are the UI contract.
    fun playEntries(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem, entries: List<com.thomasnorlund.archiveplayer.core.catalog.Entry>, start: Int, shuffle: Boolean = false) = Unit
    fun enqueue(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem, entry: com.thomasnorlund.archiveplayer.core.catalog.Entry, next: Boolean) = Unit
    fun downloadItem(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem) = Unit
    fun downloadEntry(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem, entry: com.thomasnorlund.archiveplayer.core.catalog.Entry) = Unit
    fun downloadLabelKey(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem): String = "item.download"

    fun setLiked(item: com.thomasnorlund.archiveplayer.core.catalog.ArchiveItem, entries: List<com.thomasnorlund.archiveplayer.core.catalog.Entry>, on: Boolean) =
        library.update { s ->
            val liked = s.liked
            val kept = liked.entries.filterNot { it.identifier == item.identifier }
            val next = if (on) kept + entries.map { it.ref() } else kept
            s.copy(playlists = s.playlists.map { if (it.isLiked) liked.copy(entries = next) else it })
        }

    fun setEntryLiked(entry: com.thomasnorlund.archiveplayer.core.catalog.Entry, on: Boolean) = library.update { s ->
        val ref = entry.ref()
        val liked = s.liked
        val next = if (on) liked.entries.filterNot { it == ref } + ref else liked.entries.filterNot { it == ref }
        s.copy(playlists = s.playlists.map { if (it.isLiked) liked.copy(entries = next) else it })
    }

    fun setFollowing(identifier: String, on: Boolean) = library.update { s ->
        s.copy(follows = if (on) (s.follows - identifier) + identifier else s.follows - identifier)
    }

    fun createPlaylist(name: String, entries: List<com.thomasnorlund.archiveplayer.core.playlist.EntryRef>): String {
        val id = java.util.UUID.randomUUID().toString()
        library.update { s -> s.copy(playlists = s.playlists + com.thomasnorlund.archiveplayer.core.playlist.Playlist(id, name, null, entries)) }
        return id
    }

    fun addToPlaylist(playlistId: String, entries: List<com.thomasnorlund.archiveplayer.core.playlist.EntryRef>) = library.update { s ->
        s.copy(playlists = s.playlists.map { if (it.id == playlistId) com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.addItemEntries(it, entries) else it })
    }

    var libraryFilter by mutableStateOf("playlists")
    var deletedPlaylist by mutableStateOf<Pair<Int, com.thomasnorlund.archiveplayer.core.playlist.Playlist>?>(null)
        private set

    private fun updatePlaylist(id: String, change: (com.thomasnorlund.archiveplayer.core.playlist.Playlist) -> com.thomasnorlund.archiveplayer.core.playlist.Playlist) =
        library.update { s -> s.copy(playlists = s.playlists.map { if (it.id == id) change(it) else it }) }

    fun renamePlaylist(id: String, title: String) = updatePlaylist(id) { com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.rename(it, title) }
    fun describePlaylist(id: String, description: String) = updatePlaylist(id) { it.copy(description = description.trim().ifEmpty { null }) }
    fun reorderPlaylist(id: String, from: Int, to: Int) = updatePlaylist(id) { com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.reorder(it, from, to) }
    fun removeFromPlaylist(id: String, index: Int) = updatePlaylist(id) { com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.remove(it, index) }

    fun deletePlaylist(id: String) {
        val all = library.state.playlists
        val index = all.indexOfFirst { it.id == id }
        val playlist = all.getOrNull(index)?.takeIf { com.thomasnorlund.archiveplayer.core.playlist.PlaylistRules.canDelete(it) } ?: return
        deletedPlaylist = index to playlist
        library.update { s -> s.copy(playlists = s.playlists.filterNot { it.id == id }) }
    }

    fun undoDelete() {
        val (index, playlist) = deletedPlaylist ?: return
        deletedPlaylist = null
        library.update { s -> s.copy(playlists = s.playlists.toMutableList().apply { add(index.coerceAtMost(size), playlist) }) }
    }

    fun dismissUndo() { deletedPlaylist = null }

    fun clearHistory() = library.update { it.copy(history = emptyList()) }

    fun playRefs(refs: List<com.thomasnorlund.archiveplayer.core.playlist.EntryRef>, start: Int, shuffle: Boolean = false) = Unit
    fun downloadPlaylist(playlist: com.thomasnorlund.archiveplayer.core.playlist.Playlist) = Unit
    fun sharePlaylist(context: android.content.Context, playlist: com.thomasnorlund.archiveplayer.core.playlist.Playlist) = Unit
}
