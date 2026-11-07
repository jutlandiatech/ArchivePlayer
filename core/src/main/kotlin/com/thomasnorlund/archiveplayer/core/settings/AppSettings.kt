package com.thomasnorlund.archiveplayer.core.settings

enum class ThemeMode { LIGHT, NOIR, SYSTEM }
enum class DownloadLocation { INTERNAL, SD_CARD }
enum class SubtitleDefault { OFF, CONTENT_LANGUAGES, ALWAYS }

/** All user settings (R72). App language and content languages are independent (R27, A42). */
data class AppSettings(
    val appLanguage: String,
    val contentLanguages: Set<String>,
    val theme: ThemeMode,
    val dataSaver: Boolean,
    val wifiOnly: Boolean,
    val showAdultContent: Boolean,
    val downloadLocation: DownloadLocation,
    val subtitleDefault: SubtitleDefault,
    val includeAutoSubtitles: Boolean,
    val historyPaused: Boolean,
) {
    fun withAppLanguage(isoCode: String): AppSettings = TODO()
    fun withContentLanguages(isoCodes: Set<String>): AppSettings = TODO()

    companion object {
        /** Defaults for a fresh install on a phone with [phoneLanguage]. */
        fun defaults(phoneLanguage: String, availableIsoCodes: Collection<String>): AppSettings = TODO()
    }
}

/** Theme resolution and the top-bar toggle (R63, R82). */
object ThemeResolver {
    fun isDark(mode: ThemeMode, systemDark: Boolean): Boolean = TODO()

    /** Top-bar toggle: flips what is currently shown between LIGHT and NOIR. */
    fun toggle(mode: ThemeMode, systemDark: Boolean): ThemeMode = TODO()
}
