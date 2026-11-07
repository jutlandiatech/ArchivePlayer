package com.thomasnorlund.archiveplayer.core.language

/** Maps messy archive.org `language` values to ISO 639-1 codes (R13). */
object LanguageNormalizer {
    /** ISO 639-1 code for a raw value (`fre`, `French`, `Français`, `fr`, `FR`), or null if unmapped. */
    fun normalize(raw: String): String? = TODO()

    /** Normalizes all raw values, also splitting `;`/`,`-separated lists; unmapped values are dropped. */
    fun normalizeAll(raw: List<String>): Set<String> = TODO()

    /** All ISO 639-1 codes known to the mapping table ("More languages", UI_SETUP_SPEC step 2). */
    val knownCodes: Set<String> get() = TODO()

    /** Native display name of an ISO code, e.g. `da` → `Dansk`. */
    fun nativeName(iso: String): String = TODO()
}

/** Language filter rule (R28). */
object LanguageFilter {
    fun matches(itemLanguages: Set<String>, filter: Set<String>): Boolean = TODO()
}

/** First-launch preselection (R67, SO1, SO2, D30). */
object LanguagePreselection {
    /** Content-language chips shown directly in setup and Settings. */
    val directContentChips: List<String> get() = TODO()

    /** UI language: the phone's language when a catalog exists for it, otherwise English. [phoneLanguage] is a BCP-47 tag or ISO code. */
    fun appLanguage(phoneLanguage: String, availableIsoCodes: Collection<String>): String = TODO()

    /** Content languages: the phone's language (if it is a known language) plus English; independent of the app language. */
    fun contentLanguages(phoneLanguage: String): Set<String> = TODO()
}
