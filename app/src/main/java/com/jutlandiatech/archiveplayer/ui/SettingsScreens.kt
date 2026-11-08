package com.jutlandiatech.archiveplayer.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jutlandiatech.archiveplayer.AppViewModel
import com.jutlandiatech.archiveplayer.BuildConfig
import com.jutlandiatech.archiveplayer.core.language.LanguageNormalizer
import com.jutlandiatech.archiveplayer.core.settings.AppSettings
import com.jutlandiatech.archiveplayer.core.settings.DownloadLocation
import com.jutlandiatech.archiveplayer.core.settings.SubtitleDefault
import com.jutlandiatech.archiveplayer.core.settings.ThemeMode

@Composable
fun BackHeader(title: String, backTag: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        GlyphButton("←", t("action.back"), backTag, onClick = onBack)
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}

private fun onOff(on: Boolean) = if (on) "settings.on" else "settings.off"

@Composable
private fun rowValue(row: String, s: AppSettings, vm: AppViewModel): String = when (row) {
    "app_language" -> vm.texts.forLanguage(s.appLanguage).selfName
    "appearance" -> t("settings.theme." + s.theme.name.lowercase())
    "content_languages" -> s.contentLanguages.sorted().joinToString(", ") { LanguageNormalizer.nativeName(it) }.ifEmpty { "—" }
    "data_saver" -> t(onOff(s.dataSaver))
    "wifi_only" -> t(onOff(s.wifiOnly))
    "adult_content" -> t(if (s.showAdultContent) "settings.adult.shown" else "settings.adult.hidden")
    "storage" -> t(if (s.downloadLocation == DownloadLocation.SD_CARD) "settings.storage.sd" else "settings.storage.internal")
    "subtitles" -> t(subtitleKey(s.subtitleDefault))
    "history" -> t(onOff(!s.historyPaused))
    "search_history" -> vm.library.state.recentSearches.size.toString()
    "about" -> t("settings.about.version", "version" to BuildConfig.VERSION_NAME)
    else -> ""
}

private fun subtitleKey(d: SubtitleDefault) = when (d) {
    SubtitleDefault.OFF -> "settings.subtitles.off"
    SubtitleDefault.CONTENT_LANGUAGES -> "settings.subtitles.content"
    SubtitleDefault.ALWAYS -> "settings.subtitles.always"
}

@Composable
fun SettingsScreen(vm: AppViewModel, settings: AppSettings) {
    Column(Modifier.fillMaxSize().testTag(ElementIds.SETTINGS_SCREEN)) {
        BackHeader(t("settings.title"), ElementIds.SETTINGS_BACK_BUTTON) { vm.back() }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ElementIds.SETTINGS_ROWS.forEach { row ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.push(com.jutlandiatech.archiveplayer.Screen.SettingsDetail(row)) }
                        .testTag(ElementIds.settingsRow(row)),
                ) {
                    Row(Modifier.padding(16.dp).heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(t("settings.row.$row"), style = MaterialTheme.typography.bodyLarge)
                            val value = rowValue(row, settings, vm)
                            if (value.isNotEmpty()) Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Glyph("›")
                    }
                }
            }
        }
    }
}

@Composable
private fun Choice(selected: Boolean, label: String, tag: String, onSelect: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected = selected, role = Role.RadioButton, onClick = onSelect).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, tag: String, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = checked, role = Role.Switch, onValueChange = onChange).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
fun SettingsDetailScreen(vm: AppViewModel, settings: AppSettings, row: String, extra: @Composable (String) -> Unit = {}) {
    val update = vm::updateSettings
    Column(Modifier.fillMaxSize().testTag(ElementIds.SETTINGS_SCREEN + "_" + row)) {
        BackHeader(t("settings.row.$row"), ElementIds.SETTINGS_BACK_BUTTON) { vm.back() }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (row) {
                "app_language" -> vm.texts.catalogs.values.forEach { c ->
                    Choice(settings.appLanguage == c.isoCode, c.selfName, ElementIds.settingsLanguage(c.isoCode)) { update(settings.withAppLanguage(c.isoCode)) }
                }
                "appearance" -> listOf(ThemeMode.LIGHT, ThemeMode.NOIR, ThemeMode.SYSTEM).forEach { m ->
                    Choice(settings.theme == m, t("settings.theme." + m.name.lowercase()), ElementIds.settingsAppearance(m.name.lowercase())) { update(settings.copy(theme = m)) }
                }
                "content_languages" -> {
                    Text(t("setup.contentLanguages.hint"), style = MaterialTheme.typography.bodyLarge)
                    LanguageChips(settings.contentLanguages, ElementIds::settingsContentLanguage, "settings_content_languages_more_button") { iso ->
                        val set = settings.contentLanguages.toMutableSet().apply { if (!remove(iso)) add(iso) }
                        update(settings.withContentLanguages(set))
                    }
                }
                "data_saver" -> {
                    SwitchRow(t("settings.row.data_saver"), settings.dataSaver, ElementIds.settingsSwitch(row)) { update(settings.copy(dataSaver = it)) }
                    Text(t("settings.dataSaver.hint"), style = MaterialTheme.typography.bodyMedium)
                }
                "wifi_only" -> SwitchRow(t("settings.row.wifi_only"), settings.wifiOnly, ElementIds.settingsSwitch(row)) { update(settings.copy(wifiOnly = it)) }
                "adult_content" -> {
                    SwitchRow(t("settings.row.adult_content"), settings.showAdultContent, ElementIds.settingsSwitch(row)) { update(settings.copy(showAdultContent = it)) }
                    Text(t("setup.ready.adultContent"), style = MaterialTheme.typography.bodyMedium)
                }
                "subtitles" -> {
                    SubtitleDefault.entries.forEach { d ->
                        Choice(settings.subtitleDefault == d, t(subtitleKey(d)), "settings_subtitles_${d.name.lowercase()}") { update(settings.copy(subtitleDefault = d)) }
                    }
                    SwitchRow(t("settings.subtitles.auto"), settings.includeAutoSubtitles, ElementIds.settingsSwitch("auto_subtitles")) { update(settings.copy(includeAutoSubtitles = it)) }
                }
                "search_history" -> vm.library.state.recentSearches.forEachIndexed { i, q ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(q, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).testTag("settings_search_history_$i"))
                        TagGlyphButton("settings_search_history_remove_$i", "✕", t("action.delete")) { vm.search.removeRecent(i) }
                    }
                }
                "history" -> SwitchRow(t("settings.history.pause"), settings.historyPaused, ElementIds.settingsSwitch("history_pause")) { update(settings.copy(historyPaused = it)) }
                "about" -> {
                    Text(t("app.name"), style = MaterialTheme.typography.headlineMedium)
                    Text(t("settings.about.version", "version" to BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge)
                    Text(t("settings.about.flavour", "flavour" to BuildConfig.FLAVOR), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(ElementIds.SETTINGS_ABOUT_FLAVOUR))
                    Text(t("settings.about.license"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(ElementIds.SETTINGS_ABOUT_NOTICES))
                    Text(t("settings.about.source"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(ElementIds.SETTINGS_ABOUT_SOURCE))
                }
            }
            extra(row)
        }
    }
}
