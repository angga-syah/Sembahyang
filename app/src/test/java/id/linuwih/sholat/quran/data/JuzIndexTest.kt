package id.linuwih.sholat.quran.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Batas juz menentukan ayat mana yang ikut terunduh di mode "Per Juz". Salah satu ayat saja
 * membuat sebuah surah dianggap "terunduh sebagian" selamanya, dan pemutaran otomatis akan
 * melewatinya — kegagalan yang sulit dilacak karena tidak memunculkan error apa pun.
 */
class JuzIndexTest {

    /** Jumlah ayat asli tiap surah, dipakai sebagai data uji. */
    private val surahs: List<Surah> = AYAH_COUNTS.mapIndexed { index, count ->
        Surah(
            number = index + 1,
            arabicName = "",
            transliteration = "S${index + 1}",
            translationName = "",
            type = "meccan",
            totalVerses = count,
        )
    }

    @Test
    fun `gabungan seluruh juz sama persis dengan seluruh Al-Quran`() {
        val fromJuz = (1..JuzIndex.TOTAL_JUZ).flatMap { JuzIndex.ayahsIn(it, surahs) }
        val all = JuzIndex.allAyahs(surahs)

        assertEquals("total ayat harus 6236", 6236, all.size)
        assertEquals("gabungan juz harus menutupi seluruh ayat", all.size, fromJuz.size)
        assertEquals(all, fromJuz)
    }

    @Test
    fun `tidak ada ayat yang terhitung dua kali atau terlewat`() {
        val fromJuz = (1..JuzIndex.TOTAL_JUZ).flatMap { JuzIndex.ayahsIn(it, surahs) }
        assertEquals("ada ayat ganda antar juz", fromJuz.size, fromJuz.toSet().size)
    }

    @Test
    fun `setiap juz berisi ayat dan tidak ada yang kosong`() {
        (1..JuzIndex.TOTAL_JUZ).forEach { juz ->
            assertTrue("juz $juz kosong", JuzIndex.ayahsIn(juz, surahs).isNotEmpty())
        }
    }

    @Test
    fun `juz 1 mulai Al-Fatihah 1 dan juz 30 berakhir di An-Nas`() {
        assertEquals(1 to 1, JuzIndex.ayahsIn(1, surahs).first())
        assertEquals(114 to 6, JuzIndex.ayahsIn(30, surahs).last())
    }

    @Test
    fun `ayat dalam satu surah sesuai jumlah aslinya`() {
        val alBaqarah = surahs.first { it.number == 2 }
        val ayahs = JuzIndex.ayahsInSurah(alBaqarah)
        assertEquals(286, ayahs.size)
        assertEquals(2 to 1, ayahs.first())
        assertEquals(2 to 286, ayahs.last())
    }

    private companion object {
        val AYAH_COUNTS = listOf(
            7, 286, 200, 176, 120, 165, 206, 75, 129, 109, 123, 111, 43, 52, 99, 128, 111, 110,
            98, 135, 112, 78, 118, 64, 77, 227, 93, 88, 69, 60, 34, 30, 73, 54, 45, 83, 182, 88,
            75, 85, 54, 53, 89, 59, 37, 35, 38, 29, 18, 45, 60, 49, 62, 55, 78, 96, 29, 22, 24,
            13, 14, 11, 11, 18, 12, 12, 30, 52, 52, 44, 28, 28, 20, 56, 40, 31, 50, 40, 46, 42,
            29, 19, 36, 25, 22, 17, 19, 26, 30, 20, 15, 21, 11, 8, 8, 19, 5, 8, 8, 11, 11, 8, 3,
            9, 5, 4, 7, 3, 6, 3, 5, 4, 5, 6,
        )
    }
}
