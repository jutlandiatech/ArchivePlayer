package com.thomasnorlund.archiveplayer.data

import android.content.Context
import com.thomasnorlund.archiveplayer.core.settings.AppSettings
import com.thomasnorlund.archiveplayer.core.settings.DownloadLocation
import com.thomasnorlund.archiveplayer.core.settings.SubtitleDefault
import com.thomasnorlund.archiveplayer.core.settings.ThemeMode

/** Persists [AppSettings]; null until first-launch setup has completed (UI_SETUP_SPEC.md). */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): AppSettings? {
        if (!prefs.getBoolean(SETUP_DONE, false)) return null
        return AppSettings(
            appLanguage = prefs.getString("appLanguage", "en")!!,
            contentLanguages = prefs.getStringSet("contentLanguages", setOf("en"))!!.toSet(),
            theme = enumOr(prefs.getString("theme", null), ThemeMode.LIGHT),
            dataSaver = prefs.getBoolean("dataSaver", true),
            wifiOnly = prefs.getBoolean("wifiOnly", true),
            showAdultContent = prefs.getBoolean("showAdultContent", false),
            downloadLocation = enumOr(prefs.getString("downloadLocation", null), DownloadLocation.INTERNAL),
            subtitleDefault = enumOr(prefs.getString("subtitleDefault", null), SubtitleDefault.OFF),
            includeAutoSubtitles = prefs.getBoolean("includeAutoSubtitles", true),
            historyPaused = prefs.getBoolean("historyPaused", false),
        )
    }

    fun save(s: AppSettings) {
        prefs.edit()
            .putBoolean(SETUP_DONE, true)
            .putString("appLanguage", s.appLanguage)
            .putStringSet("contentLanguages", s.contentLanguages)
            .putString("theme", s.theme.name)
            .putBoolean("dataSaver", s.dataSaver)
            .putBoolean("wifiOnly", s.wifiOnly)
            .putBoolean("showAdultContent", s.showAdultContent)
            .putString("downloadLocation", s.downloadLocation.name)
            .putString("subtitleDefault", s.subtitleDefault.name)
            .putBoolean("includeAutoSubtitles", s.includeAutoSubtitles)
            .putBoolean("historyPaused", s.historyPaused)
            .commit()
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        runCatching { enumValueOf<E>(name!!) }.getOrDefault(default)

    private companion object {
        const val SETUP_DONE = "setupDone"
    }
}
