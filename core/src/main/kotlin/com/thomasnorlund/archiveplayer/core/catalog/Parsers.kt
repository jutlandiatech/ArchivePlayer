package com.thomasnorlund.archiveplayer.core.catalog

/** Parses `https://archive.org/metadata/<id>` responses (R07–R13, R45, R78). Never throws. */
object MetadataParser {
    /** Null for invalid JSON, empty responses or unsupported media types. */
    fun parse(json: String): ArchiveItem? = TODO()
}

/** Parses `advancedsearch.php` JSON responses (R22–R25). Never throws; invalid input gives an empty page. */
object SearchResponseParser {
    fun parse(json: String): SearchPage = TODO()
}

/** Subtitle-file classification (R45, A33, A40). */
object SubtitleDetector {
    /** Null if [fileName] is not a SubRip/WebVTT file. */
    fun classify(fileName: String, format: String?): SubtitleTrack? = TODO()
}

/** Turns archive.org description HTML into safe plain text (R12, R78). */
object HtmlSanitizer {
    fun toPlainText(html: String): String = TODO()
}

/** archive.org length values: seconds (`664.03`) or `mm:ss` / `hh:mm:ss`. Null when unparsable. */
object DurationParser {
    fun parseSeconds(raw: String?): Double? = TODO()
}
