package com.thomasnorlund.archiveplayer.core.setup

import com.thomasnorlund.archiveplayer.core.language.LanguagePreselection
import com.thomasnorlund.archiveplayer.core.settings.AppSettings

enum class SetupStep { LANGUAGE, CONTENT_LANGUAGES, READY }

/** First-launch setup state (UI_SETUP_SPEC.md, U01–U07). */
class SetupFlow(private val phoneLanguage: String, private val availableIsoCodes: Collection<String>) {
    var step: SetupStep = SetupStep.LANGUAGE
        private set
    val stepNumber: Int get() = step.ordinal + 1
    val stepCount: Int get() = SetupStep.entries.size
    val showsBack: Boolean get() = step != SetupStep.LANGUAGE
    var appLanguage: String = LanguagePreselection.appLanguage(phoneLanguage, availableIsoCodes)
        private set
    private val selectedContent = LinkedHashSet(LanguagePreselection.contentLanguages(phoneLanguage))
    val contentLanguages: Set<String> get() = selectedContent.toSet()
    var isCompleted: Boolean = false
        private set

    fun selectAppLanguage(isoCode: String) {
        require(isoCode in availableIsoCodes) { "No catalog for $isoCode" }
        appLanguage = isoCode
    }

    fun toggleContentLanguage(isoCode: String) {
        if (!selectedContent.remove(isoCode)) selectedContent.add(isoCode)
    }

    fun next() {
        if (step != SetupStep.READY) step = SetupStep.entries[step.ordinal + 1]
    }

    /** One step back; returns false on the first step (Android Back leaves the app, setup not completed). */
    fun back(): Boolean {
        if (step == SetupStep.LANGUAGE) return false
        step = SetupStep.entries[step.ordinal - 1]
        return true
    }

    /** Completes setup on the Ready step; returns settings the first time only (double tap completes once). */
    fun start(): AppSettings? {
        check(step == SetupStep.READY) { "Setup can only start on the Ready step" }
        if (isCompleted) return null
        isCompleted = true
        return AppSettings.defaults(phoneLanguage, availableIsoCodes)
            .withAppLanguage(appLanguage)
            .withContentLanguages(contentLanguages)
    }
}
