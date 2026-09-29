package id.linuwih.sholat.quran.ui

import androidx.compose.ui.graphics.Color

/**
 * Skema warna tajwid mengikuti konvensi umum (alquran.cloud/tajweed-guide) — lihat
 * assets/quran/ATTRIBUTION.md. madd_246 (al-'Aarid/leen) tidak terdaftar eksplisit di
 * panduan itu, didekati dengan warna biru terdekat yang belum dipakai rule lain.
 *
 * PENTING: key di map ini harus PERSIS sama dengan nilai field "r" di
 * assets/quran/quran_tajweed_rules.json (dataset semarketir/quranjson memakai ejaan
 * "idghaam" — dobel-a — bukan "idgham"). Key yang salah ejaan sebelumnya (idgham_ghunnah,
 * idgham_no_ghunnah, idgham_shafawi, idgham_mutajaanisain, idgham_mutaqaaribain) membuat
 * tajweedColorFor() diam-diam mengembalikan Color.Unspecified untuk SEMUA rule idgham —
 * ini sebabnya banyak idgham yang seharusnya berwarna malah tampil putih polos. Sudah
 * diverifikasi manual: 18 nama rule di bawah ini adalah SEMUA nilai unik "r" yang ada di
 * seluruh dataset (6.236 ayat), tidak ada satupun yang tidak tercakup.
 */
private val TAJWEED_COLORS: Map<String, Color> = mapOf(
    "hamzat_wasl" to Color(0xFFAAAAAA),
    "silent" to Color(0xFFAAAAAA),
    "lam_shamsiyyah" to Color(0xFFAAAAAA),
    "madd_2" to Color(0xFF537FFF),
    "madd_munfasil" to Color(0xFF4050FF),
    "madd_246" to Color(0xFF6C6CFF),
    "madd_muttasil" to Color(0xFF000EBC),
    "madd_6" to Color(0xFF2144C1),
    "qalqalah" to Color(0xFFDD0008),
    "ikhfa_shafawi" to Color(0xFFD500B7),
    "ikhfa" to Color(0xFF9400A8),
    "idghaam_shafawi" to Color(0xFF58B800),
    "iqlab" to Color(0xFF26BFFD),
    "idghaam_ghunnah" to Color(0xFF169777),
    "idghaam_no_ghunnah" to Color(0xFF169200),
    "idghaam_mutajanisayn" to Color(0xFFA1A1A1),
    "idghaam_mutaqaribayn" to Color(0xFFA1A1A1),
    "ghunnah" to Color(0xFFFF7E1E),

    // Nama aturan versi dataset Kemenag (kodepandai/colorful-quran) — beda ejaan/istilah dari
    // dataset Utsmani di atas, tapi dipetakan ke warna yang sama supaya arti warna tetap
    // konsisten kalau user berganti gaya mushaf.
    "idgham-bighunnah" to Color(0xFF169777),
    "idgham-bilaghunnah" to Color(0xFF169200),
    "idgham-mimi" to Color(0xFF58B800),
    "ikhfa-syafawi" to Color(0xFFD500B7),
    "iqlab" to Color(0xFF26BFFD),
    "madd-arid-lissukun" to Color(0xFF6C6CFF),
    "madd-jaiz" to Color(0xFF4050FF),
    "madd-wajib" to Color(0xFF000EBC),
    "madd-lazim-mukhofaf-harfi" to Color(0xFF2144C1),
    "madd-lazim-mutsaqal-harfi" to Color(0xFF2144C1),
)

fun tajweedColorFor(rule: String): Color = TAJWEED_COLORS[rule] ?: Color.Unspecified
