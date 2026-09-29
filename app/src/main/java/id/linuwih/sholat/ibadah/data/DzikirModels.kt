package id.linuwih.sholat.ibadah.data

enum class DzikirSet(val assetFile: String) {
    SUGRO_PAGI("dzikir/sugro-pagi.json"),
    SUGRO_PETANG("dzikir/sugro-petang.json"),
    KUBRO_PAGI("dzikir/kubro-pagi.json"),
    KUBRO_PETANG("dzikir/kubro-petang.json"),
}

/** isHeader = grup/bagian (mis. "Surat Al-Fatihah") — ditampilkan sebagai judul seksi, bukan bacaan. */
data class DzikirItem(
    val title: String,
    val arabic: String,
    val latin: String,
    val translation: String,
    val repeat: Int,
    val isHeader: Boolean,
)
