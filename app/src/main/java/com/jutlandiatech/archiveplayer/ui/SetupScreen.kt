package com.jutlandiatech.archiveplayer.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jutlandiatech.archiveplayer.AppViewModel
import com.jutlandiatech.archiveplayer.core.language.LanguageNormalizer
import com.jutlandiatech.archiveplayer.core.setup.SetupStep

/** Languages shown before "More languages" (UI_SETUP_SPEC.md step 2). */
val COMMON_CONTENT_LANGUAGES = listOf("en", "de", "da", "fr", "es", "it", "nl", "sv", "pt", "pl", "ru", "ja", "zh", "ar")

@Composable
fun SetupScreen(vm: AppViewModel) {
    @Suppress("UNUSED_VARIABLE") val version = vm.setupVersion
    val flow = vm.setup
    val catalog = vm.texts.forLanguage(flow.appLanguage)
    CompositionLocalProvider(LocalTexts provides catalog) {
        Surface(Modifier.fillMaxSize().idRoot(ElementIds.SETUP_ROOT)) {
            Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
                AppLogoLockup(t("app.name"), 36.dp, MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.padding(8.dp))
                Text(
                    t("setup.progress", "current" to flow.stepNumber, "total" to flow.stepCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.testTag(ElementIds.SETUP_PROGRESS),
                )
                Spacer(Modifier.padding(8.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (flow.step) {
                        SetupStep.LANGUAGE -> LanguageStep(vm)
                        SetupStep.CONTENT_LANGUAGES -> ContentLanguageStep(vm)
                        SetupStep.READY -> ReadyStep()
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (flow.showsBack) {
                        OutlinedButton(onClick = { vm.onSetup { back() } }, Modifier.heightIn(min = 48.dp).testTag(ElementIds.SETUP_BACK_BUTTON)) {
                            Text(t("action.back"))
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    if (flow.step == SetupStep.READY) {
                        Button(onClick = { vm.startApp() }, Modifier.heightIn(min = 48.dp).testTag(ElementIds.SETUP_START_BUTTON)) { Text(t("action.start")) }
                    } else {
                        Button(onClick = { vm.onSetup { next() } }, Modifier.heightIn(min = 48.dp).testTag(ElementIds.SETUP_CONTINUE_BUTTON)) { Text(t("action.continue")) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LanguageStep(vm: AppViewModel) {
    @Suppress("UNUSED_VARIABLE") val version = vm.setupVersion
    Column(Modifier.testTag(ElementIds.SETUP_LANGUAGE_SCREEN)) {
        Text(t("setup.language.title"), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag(ElementIds.SETUP_LANGUAGE_TITLE))
        Text(t("setup.language.hint"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp).testTag(ElementIds.SETUP_LANGUAGE_HINT))
        vm.texts.catalogs.values.forEach { c ->
            val selected = vm.setup.appLanguage == c.isoCode
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp)
                    .selectable(selected = selected, role = Role.RadioButton) { vm.onSetup { selectAppLanguage(c.isoCode) } }
                    .testTag(ElementIds.setupLanguage(c.isoCode)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = selected, onClick = null)
                Text(c.selfName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LanguageChips(selected: Set<String>, tagOf: (String) -> String, moreTag: String, onToggle: (String) -> Unit) {
    var more by rememberSaveable { mutableStateOf(false) }
    val codes = (selected + COMMON_CONTENT_LANGUAGES + if (more) LanguageNormalizer.knownCodes.sorted() else emptyList()).distinct()
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        codes.forEach { iso ->
            val on = iso in selected
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (on) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline),
                modifier = Modifier.padding(vertical = 4.dp).heightIn(min = 48.dp)
                    .toggleable(value = on, role = Role.Checkbox) { onToggle(iso) }.testTag(tagOf(iso)),
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (on) Text("✓ ", color = MaterialTheme.colorScheme.primary)
                    Text(LanguageNormalizer.nativeName(iso), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
    if (!more) {
        OutlinedButton(onClick = { more = true }, Modifier.heightIn(min = 48.dp).testTag(moreTag)) { Text(t("setup.contentLanguages.more")) }
    }
}

@Composable
private fun ContentLanguageStep(vm: AppViewModel) {
    @Suppress("UNUSED_VARIABLE") val version = vm.setupVersion
    Column(Modifier.testTag(ElementIds.SETUP_CONTENT_LANGUAGES_SCREEN)) {
        Text(t("setup.contentLanguages.title"), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag(ElementIds.SETUP_CONTENT_LANGUAGES_TITLE))
        Text(t("setup.contentLanguages.hint"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp).testTag(ElementIds.SETUP_CONTENT_LANGUAGES_HINT))
        LanguageChips(vm.setup.contentLanguages, ElementIds::setupContentLanguage, ElementIds.SETUP_CONTENT_LANGUAGES_MORE_BUTTON) { iso ->
            vm.onSetup { toggleContentLanguage(iso) }
        }
    }
}

@Composable
private fun ReadyStep() {
    Column(Modifier.testTag(ElementIds.SETUP_READY_SCREEN), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(t("setup.ready.title"), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.testTag(ElementIds.SETUP_READY_TITLE))
        Text(t("setup.ready.privacy"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(ElementIds.SETUP_READY_PRIVACY))
        Text(t("setup.ready.adultContent"), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(ElementIds.SETUP_READY_ADULT_CONTENT))
    }
}
