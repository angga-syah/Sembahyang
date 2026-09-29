package id.linuwih.sholat.quran.data

data class Surah(
    val number: Int,
    val arabicName: String,
    val transliteration: String,
    val translationName: String,
    val type: String,
    val totalVerses: Int,
)

/**
 * Gaya penulisan mushaf. Bukan sekadar beda font — ini dua tradisi penulisan yang berbeda:
 *
 * - [UTSMANI]: rasm Utsmani standar internasional (mushaf Madinah dsb). Hamzah washal ditulis
 *   polos (ٱ) tanpa tanda vokal; pembaca diandaikan sudah tahu kaidahnya.
 * - [KEMENAG]: Mushaf Standar Indonesia. Sengaja menambahkan tanda baca eksplisit — mis.
 *   hamzah washal diberi harakat (اِهْدِنَا, bukan ٱهْدِنَا) — supaya bisa dibaca benar tanpa
 *   harus menguasai kaidah Arab lebih dulu. Ini yang lazim dipakai di Indonesia.
 *
 * Tiap gaya punya berkas teks DAN berkas aturan tajwidnya sendiri yang index-nya selaras,
 * jadi mode tajwid tidak pernah memakai teks berbeda dari mode biasa.
 */
enum class MushafStyle { UTSMANI, KEMENAG }

enum class LatinStyle { INDONESIA, INGGRIS, TIDAK_TAMPIL }

enum class TranslationLanguage { INDONESIA, INGGRIS, KEDUANYA }

/**
 * Font harus cocok dengan [MushafStyle], bukan sekadar selera. Tiap gaya mushaf memakai
 * karakter yang tidak ada di gaya lain, dan tidak ada satu font pun yang meng-cover keduanya:
 *
 * - Teks Kemenag/MSI memakai 5 karakter khas Indonesia (a.l. U+08D6 ARABIC SMALL HIGH AIN,
 *   560 kemunculan) yang TIDAK ADA di Amiri Quran -> Android jatuh ke font sistem di tengah
 *   kata, shaping run terpecah, harakat di sekitarnya meleset.
 * - Teks Utsmani memakai U+0671 ALEF WASLA (13.483 kemunculan) yang TIDAK ADA di LPMQ.
 *
 * Karena itu [OTOMATIS] adalah default: font mengikuti gaya mushaf, dan user tidak bisa
 * memasangkan kombinasi yang rusak. Pilihan manual tetap disediakan untuk yang mau.
 * Dijaga oleh ArabicFontCoverageTest.
 */
enum class ArabicFont {
    /** Ikut gaya mushaf: Kemenag -> LPMQ Isep Misbah, Utsmani -> Amiri Quran. */
    OTOMATIS,
    LPMQ_ISEP_MISBAH,
    AMIRI_QURAN,
    SYSTEM;

    /** Font yang benar-benar dipakai untuk merender [style]. */
    fun resolve(style: MushafStyle): ArabicFont = when (this) {
        OTOMATIS -> when (style) {
            MushafStyle.KEMENAG -> LPMQ_ISEP_MISBAH
            MushafStyle.UTSMANI -> AMIRI_QURAN
        }
        else -> this
    }
}

/** Pengali ukuran teks Arab/Latin/terjemahan — disimpan sebagai skala, bukan sp mentah. */
enum class ReadingFontSize(val scale: Float) {
    KECIL(0.85f),
    NORMAL(1f),
    BESAR(1.25f),
    SANGAT_BESAR(1.5f),
}

/** start/end = index code point (bukan byte), setengah-terbuka [start,end) — lihat ATTRIBUTION.md. */
data class TajweedRule(val start: Int, val end: Int, val rule: String)

data class Ayah(
    val number: Int,
    val latinTextId: String,
    val latinTextEn: String,
    val translationId: String,
    val translationEn: String,
    /** Satu-satunya sumber teks Arab yang ditampilkan (mode tajwid & non-tajwid) — index-nya
     *  selaras dengan [tajweedRules]. Dipakai untuk kedua mode supaya wrapping baris & posisi
     *  harakat identik saat toggle tajwid; lihat ATTRIBUTION.md. */
    val arabicTextTajweed: String,
    val tajweedRules: List<TajweedRule>,
) {
    fun latinText(style: LatinStyle): String? = when (style) {
        LatinStyle.INDONESIA -> latinTextId
        LatinStyle.INGGRIS -> latinTextEn
        LatinStyle.TIDAK_TAMPIL -> null
    }
}
