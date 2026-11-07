package com.thomasnorlund.archiveplayer.core.setup

import com.thomasnorlund.archiveplayer.core.settings.AppSettings

enum class SetupStep { LANGUAGE, CONTENT_LANGUAGES, READY }

/** First-launch setup state (UI_SETUP_SPEC.md, U01–U07). */
class SetupFlow(phoneLanguage: String, availableIsoCodes: Collection<String>) {
    val step: SetupStep get() = TODO()
    val stepNumber: Int get() = TODO()
    val stepCount: Int get() = TODO()
    val showsBack: Boolean get() = TODO()
    val appLanguage: String get() = TODO()
    val contentLanguages: Set<String> get() = TODO()
    val isCompleted: Boolean get() = TODO()

    fun selectAppLanguage(isoCode: String): Unit = TODO()
    fun toggleContentLanguage(isoCode: String): Unit = TODO()
    fun next(): Unit = TODO()

    /** One step back; returns false on the first step (Android Back leaves the app, setup not completed). */
    fun back(): Boolean = TODO()

    /** Completes setup on the Ready step; returns settings the first time only (double tap completes once). */
    fun start(): AppSettings? = TODO()
}
