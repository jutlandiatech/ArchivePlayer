package com.thomasnorlund.archiveplayer.ui

/**
 * Stable, language-independent element IDs (R69), registered in UI_SHELL_SPEC.md, UI_SETUP_SPEC.md and
 * UI_SETTINGS_SPEC.md. Attached with Modifier.testTag and exposed as resource IDs for UI Automator.
 */
object ElementIds {
    // Setup (UI_SETUP_SPEC.md)
    const val SETUP_ROOT = "setup_root"
    const val SETUP_PROGRESS = "setup_progress"
    const val SETUP_LANGUAGE_SCREEN = "setup_language_screen"
    const val SETUP_LANGUAGE_TITLE = "setup_language_title"
    const val SETUP_LANGUAGE_HINT = "setup_language_hint"
    fun setupLanguage(iso: String) = "setup_language_$iso"
    const val SETUP_CONTENT_LANGUAGES_SCREEN = "setup_content_languages_screen"
    const val SETUP_CONTENT_LANGUAGES_TITLE = "setup_content_languages_title"
    const val SETUP_CONTENT_LANGUAGES_HINT = "setup_content_languages_hint"
    fun setupContentLanguage(iso: String) = "setup_content_language_$iso"
    const val SETUP_CONTENT_LANGUAGES_MORE_BUTTON = "setup_content_languages_more_button"
    const val SETUP_READY_SCREEN = "setup_ready_screen"
    const val SETUP_READY_TITLE = "setup_ready_title"
    const val SETUP_READY_PRIVACY = "setup_ready_privacy"
    const val SETUP_READY_ADULT_CONTENT = "setup_ready_adult_content"
    const val SETUP_CONTINUE_BUTTON = "setup_continue_button"
    const val SETUP_BACK_BUTTON = "setup_back_button"
    const val SETUP_START_BUTTON = "setup_start_button"

    // Shell (UI_SHELL_SPEC.md)
    const val APP_ROOT = "app_root"
    const val NAV_BAR = "nav_bar"
    const val NAV_RAIL = "nav_rail"
    const val NAV_HOME = "nav_home"
    const val NAV_SEARCH = "nav_search"
    const val NAV_LIBRARY = "nav_library"
    const val TOP_BAR_TITLE = "top_bar_title"
    const val TOP_BAR_THEME_TOGGLE = "top_bar_theme_toggle"
    const val TOP_BAR_CAST_BUTTON = "top_bar_cast_button"
    const val TOP_BAR_SETTINGS_BUTTON = "top_bar_settings_button"
    const val HOME_SCREEN = "home_screen"
    const val SEARCH_SCREEN = "search_screen"
    const val LIBRARY_SCREEN = "library_screen"
    const val MINI_PLAYER = "mini_player"
    const val MINI_PLAYER_CAST_INDICATOR = "mini_player_cast_indicator"
    const val PLAYER_SCREEN = "player_screen"
    const val PLAYER_CAST_BUTTON = "player_cast_button"

    // Settings (UI_SETTINGS_SPEC.md)
    const val SETTINGS_SCREEN = "settings_screen"
    const val SETTINGS_BACK_BUTTON = "settings_back_button"
    val SETTINGS_ROWS = listOf(
        "app_language", "appearance", "content_languages", "data_saver", "wifi_only", "adult_content",
        "storage", "subtitles", "history", "search_history", "export_import", "about",
    )
    fun settingsRow(name: String) = "settings_row_$name"
    fun settingsLanguage(iso: String) = "settings_language_$iso"
    fun settingsAppearance(option: String) = "settings_appearance_$option"
    fun settingsContentLanguage(iso: String) = "settings_content_language_$iso"
    fun settingsSwitch(name: String) = "settings_${name}_switch"
    const val SETTINGS_STORAGE_LOCATION_INTERNAL = "settings_storage_location_int"
    const val SETTINGS_STORAGE_LOCATION_SD = "settings_storage_location_sd"
    const val SETTINGS_ABOUT_FLAVOUR = "settings_about_flavour"
    const val SETTINGS_ABOUT_NOTICES = "settings_about_notices"
    const val SETTINGS_ABOUT_SOURCE = "settings_about_source"
}
