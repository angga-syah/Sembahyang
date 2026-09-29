package id.linuwih.sholat.quran.audio

/**
 * Daftar qori dibatasi 3 pilihan dulu (lihat plan) supaya UI tidak ramai — bisa ditambah
 * nanti. cdnPath dipakai untuk CDN Quran.com (sumber utama + 1 mirror), everyayahFolder
 * untuk fallback ketiga (everyayah.com) — semua sudah diverifikasi manual, lihat plan
 * bagian "Ketergantungan server & pihak ketiga".
 */
enum class Reciter(val displayName: String, val cdnPath: String, val everyayahFolder: String) {
    ALAFASY("Mishary Rashid Al-Afasy", "Alafasy/mp3", "Alafasy_128kbps"),
    ABDUL_BASIT("Abdul Basit Abdus Samad", "AbdulBaset/Murattal/mp3", "Abdul_Basit_Murattal_192kbps"),
    SUDAIS("Abdurrahman As-Sudais", "Sudais/mp3", "Abdurrahmaan_As-Sudais_192kbps"),
}
