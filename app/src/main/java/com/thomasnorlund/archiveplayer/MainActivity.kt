package com.thomasnorlund.archiveplayer

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
import com.thomasnorlund.archiveplayer.ui.SetupScreen
import com.thomasnorlund.archiveplayer.ui.theme.ArchivePlayerTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                                Box(Modifier.fillMaxSize().testTag(tag))
                            },
                            screenContent = { screen ->
                                when (screen) {
                                    Screen.Settings -> SettingsScreen(vm, settings)
                                    is Screen.SettingsDetail -> SettingsDetailScreen(vm, settings, screen.row)
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
