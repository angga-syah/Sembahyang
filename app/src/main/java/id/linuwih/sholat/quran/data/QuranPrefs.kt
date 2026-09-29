package id.linuwih.sholat.quran.data

import android.content.Context

class QuranPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)

    var latinStyle: LatinStyle
        get() = prefs.getString(KEY_LATIN_STYLE, null)?.let { name ->
            runCatching { LatinStyle.valueOf(name) }.getOrNull()
        } ?: LatinStyle.INDONESIA
        set(value) = prefs.edit().putString(KEY_LATIN_STYLE, value.name).apply()

    var translationLanguage: TranslationLanguage
        get() = prefs.getString(KEY_TRANSLATION_LANG, null)?.let { name ->
            runCatching { TranslationLanguage.valueOf(name) }.getOrNull()
        } ?: TranslationLanguage.INDONESIA
        set(value) = prefs.edit().putString(KEY_TRANSLATION_LANG, value.name).apply()

    var arabicFont: ArabicFont
        get() = prefs.getString(KEY_ARABIC_FONT, null)?.let { name ->
            runCatching { ArabicFont.valueOf(name) }.getOrNull()
        } ?: ArabicFont.OTOMATIS
        set(value) = prefs.edit().putString(KEY_ARABIC_FONT, value.name).apply()

    var fontSize: ReadingFontSize
        get() = prefs.getString(KEY_FONT_SIZE, null)?.let { name ->
            runCatching { ReadingFontSize.valueOf(name) }.getOrNull()
        } ?: ReadingFontSize.NORMAL
        set(value) = prefs.edit().putString(KEY_FONT_SIZE, value.name).apply()

    var mushafStyle: MushafStyle
        get() = prefs.getString(KEY_MUSHAF_STYLE, null)?.let { name ->
            runCatching { MushafStyle.valueOf(name) }.getOrNull()
        } ?: MushafStyle.KEMENAG
        set(value) = prefs.edit().putString(KEY_MUSHAF_STYLE, value.name).apply()

    var tajweedEnabled: Boolean
        // Default nyala: tajwid berwarna adalah nilai jual utama layar baca ini, dan pembaca
        // Indonesia umumnya memang terbiasa dengan mushaf bertanda tajwid.
        get() = prefs.getBoolean(KEY_TAJWEED, true)
        set(value) = prefs.edit().putBoolean(KEY_TAJWEED, value).apply()

    private companion object {
        const val KEY_LATIN_STYLE = "latin_style"
        const val KEY_TRANSLATION_LANG = "translation_language"
        const val KEY_ARABIC_FONT = "arabic_font"
        const val KEY_FONT_SIZE = "font_size"
        const val KEY_MUSHAF_STYLE = "mushaf_style"
        const val KEY_TAJWEED = "tajweed_enabled"
    }
}
