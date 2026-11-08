package com.thomasnorlund.archiveplayer

import com.thomasnorlund.archiveplayer.ui.CollectionScreen
import com.thomasnorlund.archiveplayer.ui.ItemScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.thomasnorlund.archiveplayer.core.settings.ThemeResolver
import com.thomasnorlund.archiveplayer.ui.AppShell
import com.thomasnorlund.archiveplayer.ui.ElementIds
import com.thomasnorlund.archiveplayer.ui.LocalTexts
import com.thomasnorlund.archiveplayer.ui.SettingsDetailScreen
import com.thomasnorlund.archiveplayer.ui.SettingsScreen
import com.thomasnorlund.archiveplayer.ui.SearchTab
import com.thomasnorlund.archiveplayer.ui.LibraryTab
import com.thomasnorlund.archiveplayer.ui.PlaylistScreen
import com.thomasnorlund.archiveplayer.ui.ImportScreen
import com.thomasnorlund.archiveplayer.ui.CategoryScreen
import com.thomasnorlund.archiveplayer.ui.SetupScreen
import com.thomasnorlund.archiveplayer.ui.HomeTab
import com.thomasnorlund.archiveplayer.ui.theme.ArchivePlayerTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    /** Video keeps playing in picture-in-picture when the user leaves the app (UI_PLAYER_SPEC.md P16). */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        vm.handleIntent(this, intent)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (vm.player.isVideo && vm.player.hasQueue && vm.player.isPlaying) {
            runCatching {
                enterPictureInPictureMode(
                    android.app.PictureInPictureParams.Builder().setAspectRatio(android.util.Rational(16, 9)).build()
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Outside touch mode Compose focuses the first text field that scrolls into view (e.g. the item
        // filter), which pops the keyboard and yanks the list back up.
        @OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
        androidx.compose.ui.ComposeUiFlags.isInitialFocusOnFocusableAvailable = false
        if (savedInstanceState == null) vm.handleIntent(this, intent)
        enableEdgeToEdge()
        setContent {
            val settings = vm.settings
            val systemDark = isSystemInDarkTheme()
            if (settings == null) {
                ArchivePlayerTheme(darkTheme = false) { SetupScreen(vm) }
            } else {
                ArchivePlayerTheme(darkTheme = ThemeResolver.isDark(settings.theme, systemDark)) {
                    CompositionLocalProvider(LocalTexts provides vm.texts.forLanguage(settings.appLanguage)) {
                        AppShell(
                            vm, settings, systemDark,
                            tabContent = { tab ->
                                val tag = when (tab) {
                                    Tab.HOME -> ElementIds.HOME_SCREEN
                                    Tab.SEARCH -> ElementIds.SEARCH_SCREEN
                                    Tab.LIBRARY -> ElementIds.LIBRARY_SCREEN
                                }
                                when (tab) { Tab.HOME -> HomeTab(vm); Tab.SEARCH -> SearchTab(vm); Tab.LIBRARY -> LibraryTab(vm) }
                            },
                            screenContent = { screen ->
                                when (screen) {
                                    Screen.Settings -> SettingsScreen(vm, settings)
                                    is Screen.SettingsDetail -> SettingsDetailScreen(vm, settings, screen.row)
                                    is Screen.Category -> CategoryScreen(vm, screen.categoryId)
                                    is Screen.Item -> ItemScreen(vm, screen.identifier)
                                    is Screen.Collection -> CollectionScreen(vm, screen.identifier)
                                    is Screen.Playlist -> PlaylistScreen(vm, screen.playlistId)
                                    Screen.Import -> ImportScreen(vm)
    else -> Box(Modifier.fillMaxSize())
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
