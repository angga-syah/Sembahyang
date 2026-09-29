package id.linuwih.sholat.prayer.ui

/** isToday menandai baris tanggal hari ini supaya mudah ditemukan di daftar sebulan. */
data class MonthlyDayRow(
    val dateLabel: String,
    val fajr: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val isToday: Boolean = false,
)

data class MonthlySchedule(val monthLabel: String, val rows: List<MonthlyDayRow>)
