package com.jutlandiatech.archiveplayer.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.jutlandiatech.archiveplayer.AppViewModel
import com.jutlandiatech.archiveplayer.Screen
import com.jutlandiatech.archiveplayer.Tab
import com.jutlandiatech.archiveplayer.cast.FlavourCast
import com.jutlandiatech.archiveplayer.core.settings.AppSettings
import com.jutlandiatech.archiveplayer.core.settings.ThemeResolver

private val TAB_INFO = listOf(
    Triple(Tab.HOME, ElementIds.NAV_HOME, "nav.home") to "⌂",
    Triple(Tab.SEARCH, ElementIds.NAV_SEARCH, "nav.search") to "⌕",
    Triple(Tab.LIBRARY, ElementIds.NAV_LIBRARY, "nav.library") to "≡",
)

/** App shell: top bar on tab roots, bottom bar (< 600 dp) or rail (≥ 600 dp), per-tab back stacks (UI_SHELL_SPEC.md). */
@Composable
fun AppShell(vm: AppViewModel, settings: AppSettings, systemDark: Boolean, tabContent: @Composable (Tab) -> Unit, screenContent: @Composable (Screen) -> Unit) {
    BackHandler(enabled = !vm.playerOpen && (vm.current != null || vm.tab != Tab.HOME)) { vm.back() }
    Surface(Modifier.fillMaxSize().idRoot(ElementIds.APP_ROOT)) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 600.dp
            Row(Modifier.fillMaxSize().safeDrawingPadding()) {
                if (wide) {
                    NavigationRail(Modifier.testTag(ElementIds.NAV_RAIL)) {
                        TAB_INFO.forEach { (info, glyph) ->
                            NavigationRailItem(
                                selected = vm.tab == info.first, onClick = { vm.selectTab(info.first) },
                                icon = { Text(glyph) }, label = { Text(t(info.third)) }, modifier = Modifier.testTag(info.second),
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f).fillMaxSize()) {
                    val screen = vm.current
                    if (screen == null) TopBar(vm, settings, systemDark)
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        if (screen == null) tabContent(vm.tab) else screenContent(screen)
                    }
                    MiniPlayer(vm)
                    if (!wide) {
                        NavigationBar(Modifier.testTag(ElementIds.NAV_BAR)) {
                            TAB_INFO.forEach { (info, glyph) ->
                                NavigationBarItem(
                                    selected = vm.tab == info.first, onClick = { vm.selectTab(info.first) },
                                    icon = { Text(glyph) }, label = { Text(t(info.third), maxLines = 1) }, modifier = Modifier.testTag(info.second),
                                )
                            }
                        }
                    }
                }
            }
            if (vm.playerOpen && vm.player.hasQueue) PlayerScreen(vm)
        }
    }
}

@Composable
private fun TopBar(vm: AppViewModel, settings: AppSettings, systemDark: Boolean) {
    val dark = ThemeResolver.isDark(settings.theme, systemDark)
    val titleKey = when (vm.tab) { Tab.HOME -> "app.name"; Tab.SEARCH -> "search.title"; Tab.LIBRARY -> "library.title" }
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(t(titleKey), style = MaterialTheme.typography.headlineMedium, maxLines = 1, modifier = Modifier.weight(1f).testTag(ElementIds.TOP_BAR_TITLE))
        val themeState = t(if (dark) "settings.theme.noir" else "settings.theme.light")
        GlyphButton(if (dark) "☾" else "☀", t("nav.theme"), ElementIds.TOP_BAR_THEME_TOGGLE, extra = Modifier.semantics { stateDescription = themeState }) {
            vm.toggleTheme(systemDark)
        }
        if (FlavourCast.isAvailable) {
            Box(Modifier.testTag(ElementIds.TOP_BAR_CAST_BUTTON)) { FlavourCast.RouteButton(Modifier) }
        }
        GlyphButton("⚙", t("nav.settings"), ElementIds.TOP_BAR_SETTINGS_BUTTON) { vm.push(Screen.Settings) }
    }
}
