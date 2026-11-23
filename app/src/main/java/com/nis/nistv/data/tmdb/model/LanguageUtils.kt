package com.nis.nistv.data.tmdb.model

/**
 * Utility to map UI language names to ISO 639-1 language codes for TMDB requests.
 */
object LanguageUtils {
    fun toIso6391(languageName: String): String {
        return when (languageName.trim().lowercase()) {
            "tamil" -> "ta"
            "telugu" -> "te"
            "hindi" -> "hi"
            "malayalam" -> "ml"
            "kannada" -> "kn"
            "bengali" -> "bn"
            "marathi" -> "mr"
            "gujarati" -> "gu"
            "punjabi" -> "pa"
            "english" -> "en"
            "spanish", "español" -> "es"
            "french", "français" -> "fr"
            "german", "deutsch" -> "de"
            "japanese" -> "ja"
            "korean" -> "ko"
            "chinese", "mandarin" -> "zh"
            "arabic" -> "ar"
            "russian" -> "ru"
            "italian" -> "it"
            "portuguese" -> "pt"
            else -> "en"
        }
    }
}
