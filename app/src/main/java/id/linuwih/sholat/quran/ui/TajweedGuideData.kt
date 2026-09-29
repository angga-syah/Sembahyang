package id.linuwih.sholat.quran.ui

import id.linuwih.sholat.R

/**
 * Konten kaidah tajwid klasik — ilmu umum yang diajarkan di semua kalangan (tidak ada
 * perbedaan mazhab pada definisi dasar rule di sini), BUKAN dikutip dari sumber pihak ketiga
 * mana pun. Ditulis sebagai dokumentasi dari kaidah yang SUDAH diimplementasikan & diuji di
 * TajweedEngine.kt -- lihat komentar di sana untuk logika deteksinya.
 *
 * [colorRuleName] dipakai untuk mengambil warna lewat [tajweedColorFor] (TajweedColors.kt)
 * supaya swatch warna di panduan ini SELALU cocok dengan warna yang benar-benar dipakai saat
 * membaca Qur'an -- bukan warna yang diketik ulang manual dan bisa menyimpang diam-diam kalau
 * skema warna berubah.
 */
data class TajweedGuideEntry(
    val titleRes: Int,
    val descriptionRes: Int,
    val hurufRes: Int,
    val colorRuleName: String,
)

val TAJWEED_GUIDE_ENTRIES: List<TajweedGuideEntry> = listOf(
    TajweedGuideEntry(
        R.string.tajwid_guide_qalqalah_title,
        R.string.tajwid_guide_qalqalah_desc,
        R.string.tajwid_guide_qalqalah_huruf,
        "qalqalah",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_ghunnah_title,
        R.string.tajwid_guide_ghunnah_desc,
        R.string.tajwid_guide_ghunnah_huruf,
        "ghunnah",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_iqlab_title,
        R.string.tajwid_guide_iqlab_desc,
        R.string.tajwid_guide_iqlab_huruf,
        "iqlab",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_ikhfa_title,
        R.string.tajwid_guide_ikhfa_desc,
        R.string.tajwid_guide_ikhfa_huruf,
        "ikhfa",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_ikhfa_syafawi_title,
        R.string.tajwid_guide_ikhfa_syafawi_desc,
        R.string.tajwid_guide_ikhfa_syafawi_huruf,
        "ikhfa_shafawi",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_idgham_bighunnah_title,
        R.string.tajwid_guide_idgham_bighunnah_desc,
        R.string.tajwid_guide_idgham_bighunnah_huruf,
        "idghaam_ghunnah",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_idgham_bilaghunnah_title,
        R.string.tajwid_guide_idgham_bilaghunnah_desc,
        R.string.tajwid_guide_idgham_bilaghunnah_huruf,
        "idghaam_no_ghunnah",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_idgham_mimi_title,
        R.string.tajwid_guide_idgham_mimi_desc,
        R.string.tajwid_guide_idgham_mimi_huruf,
        "idghaam_shafawi",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_madd_thabii_title,
        R.string.tajwid_guide_madd_thabii_desc,
        R.string.tajwid_guide_madd_thabii_huruf,
        "madd_2",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_madd_muttasil_title,
        R.string.tajwid_guide_madd_muttasil_desc,
        R.string.tajwid_guide_madd_generic_huruf,
        "madd_muttasil",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_madd_munfasil_title,
        R.string.tajwid_guide_madd_munfasil_desc,
        R.string.tajwid_guide_madd_generic_huruf,
        "madd_munfasil",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_madd_lazim_title,
        R.string.tajwid_guide_madd_lazim_desc,
        R.string.tajwid_guide_madd_generic_huruf,
        "madd_6",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_madd_arid_title,
        R.string.tajwid_guide_madd_arid_desc,
        R.string.tajwid_guide_madd_generic_huruf,
        "madd_246",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_hamzat_wasl_title,
        R.string.tajwid_guide_hamzat_wasl_desc,
        R.string.tajwid_guide_hamzat_wasl_huruf,
        "hamzat_wasl",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_lam_syamsiyah_title,
        R.string.tajwid_guide_lam_syamsiyah_desc,
        R.string.tajwid_guide_lam_syamsiyah_huruf,
        "lam_shamsiyyah",
    ),
    TajweedGuideEntry(
        R.string.tajwid_guide_silent_title,
        R.string.tajwid_guide_silent_desc,
        R.string.tajwid_guide_silent_huruf,
        "silent",
    ),
)
