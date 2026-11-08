package com.jutlandiatech.archiveplayer

import com.jutlandiatech.archiveplayer.ui.CollectionScreen
import com.jutlandiatech.archiveplayer.ui.ItemScreen
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
import com.jutlandiatech.archiveplayer.core.settings.ThemeResolver
import com.jutlandiatech.archiveplayer.ui.AppShell
import com.jutlandiatech.archiveplayer.ui.ElementIds
import com.jutlandiatech.archiveplayer.ui.LocalTexts
import com.jutlandiatech.archiveplayer.ui.SettingsDetailScreen
import com.jutlandiatech.archiveplayer.ui.SettingsScreen
import com.jutlandiatech.archiveplayer.ui.SearchTab
import com.jutlandiatech.archiveplayer.ui.LibraryTab
import com.jutlandiatech.archiveplayer.ui.PlaylistScreen
import com.jutlandiatech.archiveplayer.ui.ImportScreen
import com.jutlandiatech.archiveplayer.ui.CategoryScreen
import com.jutlandiatech.archiveplayer.ui.SetupScreen
import com.jutlandiatech.archiveplayer.ui.HomeTab
import com.jutlandiatech.archiveplayer.ui.theme.ArchivePlayerTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    /** Video keeps playing in picture-in-picture when the user leaves the app (UI_PLAYER_SPEC.md P16). */
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        vm.handleIntent(this, intent)
    }

    /** PiP window buttons: back 10 s, play/pause, forward 30 s (R38). */
    private val pipReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            when (intent.getIntExtra(PIP_EXTRA, 0)) {
                PIP_BACK -> vm.player.skip(-10_000)
                PIP_TOGGLE -> vm.player.togglePlay()
                PIP_FORWARD -> vm.player.skip(30_000)
            }
            window.decorView.postDelayed({ if (isInPictureInPictureMode) runCatching { setPictureInPictureParams(pipParams()) } }, 300)
        }
    }

    private fun pipText(key: String) = vm.texts.forLanguage(vm.settings?.appLanguage ?: "en").text(key, emptyMap())

    private fun pipAction(code: Int, icon: Int, key: String): android.app.RemoteAction {
        val label = pipText(key)
        val intent = android.content.Intent(PIP_ACTION).setPackage(packageName).putExtra(PIP_EXTRA, code)
        val pending = android.app.PendingIntent.getBroadcast(this, code, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT)
        return android.app.RemoteAction(android.graphics.drawable.Icon.createWithResource(this, icon), label, label, pending)
    }

    private fun pipParams(): android.app.PictureInPictureParams = android.app.PictureInPictureParams.Builder()
        .setAspectRatio(android.util.Rational(16, 9))
        .setActions(listOf(
            pipAction(PIP_BACK, android.R.drawable.ic_media_rew, "player.skipBack"),
            if (vm.player.isPlaying) pipAction(PIP_TOGGLE, android.R.drawable.ic_media_pause, "player.pause")
            else pipAction(PIP_TOGGLE, android.R.drawable.ic_media_play, "player.play"),
            pipAction(PIP_FORWARD, android.R.drawable.ic_media_ff, "player.skipForward"),
        ))
        .build()

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipReceiver) }
        super.onDestroy()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (vm.player.isVideo && vm.player.hasQueue && vm.player.isPlaying) {
            runCatching {
                enterPictureInPictureMode(
                    pipParams()
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) vm.handleIntent(this, intent)
        enableEdgeToEdge()
        androidx.core.content.ContextCompat.registerReceiver(this, pipReceiver, android.content.IntentFilter(PIP_ACTION),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED)
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

private const val PIP_ACTION = "com.jutlandiatech.archiveplayer.PIP"
private const val PIP_EXTRA = "pip"
private const val PIP_BACK = 1
private const val PIP_TOGGLE = 2
private const val PIP_FORWARD = 3
