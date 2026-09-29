package id.linuwih.sholat.ibadah.data

import java.util.Calendar

/**
 * Konversi Masehi <-> Hijriyah pakai algoritma tabular (kadang disebut "Kuwaiti algorithm") —
 * murni aritmatika, tidak perlu data/API eksternal, sesuai prinsip offline-first app ini.
 * Catatan akurasi: kalender tabular ini estimasi ±1 hari dari rukyat/hisab resmi (wajar,
 * dipakai banyak app kalender Islam sebagai pendekatan tanpa observasi bulan real).
 */
data class HijriDate(val year: Int, val month: Int, val day: Int) {
    val monthName: String get() = MONTH_NAMES[month - 1]

    override fun toString(): String = "$day $monthName $year H"

    companion object {
        val MONTH_NAMES = listOf(
            "Muharram", "Safar", "Rabiul Awal", "Rabiul Akhir",
            "Jumadil Awal", "Jumadil Akhir", "Rajab", "Sya'ban",
            "Ramadhan", "Syawal", "Dzulqaidah", "Dzulhijjah",
        )

        fun from(calendar: Calendar): HijriDate =
            fromGregorian(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH))

        fun today(): HijriDate = from(Calendar.getInstance())

        fun fromGregorian(year: Int, month: Int, day: Int): HijriDate {
            val jdn = gregorianToJdn(year, month, day)
            return jdnToHijri(jdn)
        }

        private fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
            val a = (14 - month) / 12
            val y = year + 4800 - a
            val m = month + 12 * a - 3
            return day + (153L * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
        }

        private fun jdnToHijri(jdn: Long): HijriDate {
            var l = jdn - 1948440 + 10632
            val n = (l - 1) / 10631
            l = l - 10631 * n + 354
            val j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238)
            l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29
            val month = (24 * l) / 709
            val day = l - (709 * month) / 24
            val year = 30 * n + j - 30
            return HijriDate(year.toInt(), month.toInt(), day.toInt())
        }
    }
}
