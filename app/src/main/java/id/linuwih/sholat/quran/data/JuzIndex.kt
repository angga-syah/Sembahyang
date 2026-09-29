package id.linuwih.sholat.quran.data

/**
 * Batas awal tiap juz dalam bentuk (surah, ayat) — konvensi pembagian juz standar yang dipakai
 * mushaf pada umumnya. Juz N mencakup dari JUZ_STARTS[N-1] sampai tepat SEBELUM JUZ_STARTS[N];
 * juz 30 berakhir di ayat terakhir surah 114.
 *
 * Disimpan sebagai tabel batas (bukan daftar 6.236 ayat) supaya ringan — daftar ayat per juz
 * dihitung saat dibutuhkan dari metadata jumlah ayat tiap surah.
 */
object JuzIndex {
    const val TOTAL_JUZ = 30

    private val JUZ_STARTS = listOf(
        1 to 1, 2 to 142, 2 to 253, 3 to 93, 4 to 24, 4 to 148,
        5 to 82, 6 to 111, 7 to 88, 8 to 41, 9 to 93, 11 to 6,
        12 to 53, 15 to 1, 17 to 1, 18 to 75, 21 to 1, 23 to 1,
        25 to 21, 27 to 56, 29 to 46, 33 to 31, 36 to 28, 39 to 32,
        41 to 47, 46 to 1, 51 to 31, 58 to 1, 67 to 1, 78 to 1,
    )

    /** Nomor surah tempat juz ini dimulai — dipakai untuk label ringkas di UI. */
    fun startSurahOf(juz: Int): Int = JUZ_STARTS[juz - 1].first

    /** Semua pasangan (surah, ayat) di dalam satu juz, urut dari awal ke akhir. */
    fun ayahsIn(juz: Int, surahs: List<Surah>): List<Pair<Int, Int>> {
        val (startSurah, startAyah) = JUZ_STARTS[juz - 1]
        val endExclusive = if (juz < TOTAL_JUZ) JUZ_STARTS[juz] else null
        val result = mutableListOf<Pair<Int, Int>>()

        for (surah in surahs.filter { it.number >= startSurah }.sortedBy { it.number }) {
            for (ayah in 1..surah.totalVerses) {
                if (surah.number == startSurah && ayah < startAyah) continue
                if (endExclusive != null &&
                    (surah.number > endExclusive.first ||
                        (surah.number == endExclusive.first && ayah >= endExclusive.second))
                ) {
                    return result
                }
                result += surah.number to ayah
            }
        }
        return result
    }

    /** Semua ayat di seluruh Al-Qur'an, urut — dipakai untuk mode unduh "semua surah". */
    fun allAyahs(surahs: List<Surah>): List<Pair<Int, Int>> =
        surahs.sortedBy { it.number }.flatMap { surah ->
            (1..surah.totalVerses).map { surah.number to it }
        }

    /** Semua ayat dalam satu surah. */
    fun ayahsInSurah(surah: Surah): List<Pair<Int, Int>> =
        (1..surah.totalVerses).map { surah.number to it }
}
