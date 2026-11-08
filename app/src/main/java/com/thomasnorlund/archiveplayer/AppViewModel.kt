package com.thomasnorlund.archiveplayer

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
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
}
