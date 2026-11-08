package com.jutlandiatech.archiveplayer.core.language

import java.text.Normalizer

/** Maps messy archive.org `language` values to ISO 639-1 codes (R13). */
object LanguageNormalizer {
    private class Lang(val iso: String, val native: String, vararg val aliases: String)

    private val table = listOf(
        Lang("en", "English", "eng", "english", "englisch", "anglais"),
        Lang("de", "Deutsch", "ger", "deu", "german", "deutsch", "allemand", "tysk"),
        Lang("da", "Dansk", "dan", "danish", "dansk", "danois", "dänisch"),
        Lang("fr", "Français", "fre", "fra", "french", "français", "francais", "französisch"),
        Lang("es", "Español", "spa", "spanish", "español", "espanol", "castellano"),
        Lang("it", "Italiano", "ita", "italian", "italiano"),
        Lang("sv", "Svenska", "swe", "swedish", "svenska"),
        Lang("no", "Norsk", "nor", "nob", "nno", "nb", "nn", "norwegian", "norsk", "bokmål"),
        Lang("nl", "Nederlands", "dut", "nld", "dutch", "nederlands", "flemish"),
        Lang("ja", "日本語", "jpn", "japanese", "日本語"),
        Lang("pt", "Português", "por", "portuguese", "português", "portugues"),
        Lang("ru", "Русский", "rus", "russian", "русский"),
        Lang("pl", "Polski", "pol", "polish", "polski"),
        Lang("fi", "Suomi", "fin", "finnish", "suomi"),
        Lang("is", "Íslenska", "ice", "isl", "icelandic", "íslenska"),
        Lang("cs", "Čeština", "cze", "ces", "czech", "čeština"),
        Lang("sk", "Slovenčina", "slo", "slk", "slovak"),
        Lang("hu", "Magyar", "hun", "hungarian", "magyar"),
        Lang("el", "Ελληνικά", "gre", "ell", "greek", "ελληνικά"),
        Lang("tr", "Türkçe", "tur", "turkish", "türkçe"),
        Lang("ar", "العربية", "ara", "arabic", "العربية"),
        Lang("he", "עברית", "heb", "hebrew", "עברית"),
        Lang("hi", "हिन्दी", "hin", "hindi"),
        Lang("zh", "中文", "chi", "zho", "chinese", "mandarin", "中文"),
        Lang("ko", "한국어", "kor", "korean", "한국어"),
        Lang("uk", "Українська", "ukr", "ukrainian"),
        Lang("ro", "Română", "rum", "ron", "romanian"),
        Lang("bg", "Български", "bul", "bulgarian"),
        Lang("hr", "Hrvatski", "hrv", "croatian"),
        Lang("sr", "Српски", "srp", "serbian"),
        Lang("sl", "Slovenščina", "slv", "slovenian", "slovene"),
        Lang("et", "Eesti", "est", "estonian"),
        Lang("lv", "Latviešu", "lav", "latvian"),
        Lang("lt", "Lietuvių", "lit", "lithuanian"),
        Lang("ga", "Gaeilge", "gle", "irish", "gaelic"),
        Lang("cy", "Cymraeg", "wel", "cym", "welsh"),
        Lang("ca", "Català", "cat", "catalan"),
        Lang("eu", "Euskara", "baq", "eus", "basque"),
        Lang("la", "Latina", "lat", "latin"),
        Lang("eo", "Esperanto", "epo", "esperanto"),
        Lang("yi", "ייִדיש", "yid", "yiddish"),
        Lang("fa", "فارسی", "per", "fas", "persian", "farsi"),
        Lang("id", "Bahasa Indonesia", "ind", "indonesian"),
        Lang("vi", "Tiếng Việt", "vie", "vietnamese"),
        Lang("th", "ไทย", "tha", "thai"),
        Lang("tl", "Tagalog", "tgl", "fil", "tagalog", "filipino"),
        Lang("sw", "Kiswahili", "swa", "swahili"),
        Lang("af", "Afrikaans", "afr", "afrikaans"),
        Lang("bn", "বাংলা", "ben", "bengali"),
        Lang("ta", "தமிழ்", "tam", "tamil"),
        Lang("ur", "اردو", "urd", "urdu"),
    )

    private fun key(raw: String): String = raw.trim().lowercase()
    private fun folded(raw: String): String =
        Normalizer.normalize(key(raw), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private val lookup: Map<String, String> = buildMap {
        for (lang in table) {
            for (alias in listOf(lang.iso, lang.native) + lang.aliases) {
                put(key(alias), lang.iso)
                putIfAbsent(folded(alias), lang.iso)
            }
        }
    }
    private val natives = table.associate { it.iso to it.native }

    /** ISO 639-1 code for a raw value (`fre`, `French`, `Français`, `fr`, `FR`), or null if unmapped. */
    fun normalize(raw: String): String? {
        if (raw.isBlank()) return null
        return lookup[key(raw)] ?: lookup[folded(raw)]
    }

    /** Normalizes all raw values, also splitting `;`/`,`-separated lists; unmapped values are dropped. */
    fun normalizeAll(raw: List<String>): Set<String> =
        raw.flatMap { it.split(';', ',') }.mapNotNull { normalize(it) }.toCollection(LinkedHashSet())

    /** All ISO 639-1 codes known to the mapping table ("More languages", UI_SETUP_SPEC step 2). */
    val knownCodes: Set<String> get() = natives.keys

    /** English name of an ISO code, e.g. `fr` → `French`; used in archive.org language queries. */
    fun englishName(iso: String): String? = table.firstOrNull { it.iso == iso.lowercase() }?.aliases
        ?.firstOrNull { it.length > 3 && it.all { c -> c in 'a'..'z' } }?.replaceFirstChar { it.uppercase() }

    /** Raw archive.org values that mean [iso]: the code itself, ISO 639-2 codes and the English name. */
    fun queryValues(iso: String): List<String> {
        val lang = table.firstOrNull { it.iso == iso.lowercase() } ?: return listOf(iso)
        return (listOf(lang.iso) + lang.aliases.filter { it.length == 3 && it.all { c -> c in 'a'..'z' } } +
            listOfNotNull(englishName(iso))).distinct()
    }

    /** Native display name of an ISO code, e.g. `da` → `Dansk`. */
    fun nativeName(iso: String): String = natives[iso.lowercase()] ?: iso
}

/** Language filter rule (R28). */
object LanguageFilter {
    fun matches(itemLanguages: Set<String>, filter: Set<String>): Boolean =
        filter.isEmpty() || itemLanguages.any { it in filter }
}

/** First-launch preselection (R67, SO1, SO2, D30). */
object LanguagePreselection {
    /** Content-language chips shown directly in setup and Settings. */
    val directContentChips: List<String> get() = listOf("en", "de", "da", "fr", "es", "it", "sv", "no", "nl", "ja")

    private fun primary(tag: String): String = tag.trim().replace('_', '-').substringBefore('-').lowercase()

    /** UI language: the phone's language when a catalog exists for it, otherwise English. [phoneLanguage] is a BCP-47 tag or ISO code. */
    fun appLanguage(phoneLanguage: String, availableIsoCodes: Collection<String>): String {
        val code = primary(phoneLanguage)
        return if (code in availableIsoCodes) code else "en"
    }

    /** Content languages: the phone's language (if it is a known language) plus English; independent of the app language. */
    fun contentLanguages(phoneLanguage: String): Set<String> {
        val code = primary(phoneLanguage)
        return if (code in LanguageNormalizer.knownCodes) linkedSetOf(code, "en") else linkedSetOf("en")
    }
}
