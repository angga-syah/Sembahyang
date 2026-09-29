package id.linuwih.sholat.quran.ui

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import id.linuwih.sholat.R
import id.linuwih.sholat.quran.data.ArabicFont
import id.linuwih.sholat.quran.data.MushafStyle

val amiriQuranFontFamily = FontFamily(Font(R.font.amiri_quran))
val lpmqIsepMisbahFontFamily = FontFamily(Font(R.font.lpmq_isep_misbah))

/**
 * [style] wajib ikut karena ArabicFont.OTOMATIS memilih font berdasarkan gaya mushaf --
 * lihat dokumentasi ArabicFont soal karakter yang tidak saling meng-cover.
 */
fun ArabicFont.toFontFamily(style: MushafStyle): FontFamily? = when (resolve(style)) {
    ArabicFont.SYSTEM -> null
    ArabicFont.AMIRI_QURAN -> amiriQuranFontFamily
    ArabicFont.LPMQ_ISEP_MISBAH -> lpmqIsepMisbahFontFamily
    ArabicFont.OTOMATIS -> null // tidak tercapai: resolve() selalu mengembalikan font konkret
}
