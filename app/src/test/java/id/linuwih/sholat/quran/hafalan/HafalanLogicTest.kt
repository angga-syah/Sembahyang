package id.linuwih.sholat.quran.hafalan

import id.linuwih.sholat.quran.ui.firstLetterCluster
import id.linuwih.sholat.quran.ui.letterCount
import id.linuwih.sholat.quran.ui.splitArabicWords
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HafalanLogicTest {

    private fun ayah(stage: Int, next: Long = 0) = HafalanAyahEntity(
        surahNumber = 114,
        ayahNumber = 1,
        memorizedAt = 0,
        memorizedDay = 0,
        reviewStage = stage,
        nextReviewDay = next,
    )

    @Test
    fun `lancar naik satu tahap dan jaraknya mengikuti tabel`() {
        val r = ayah(stage = 0).reviewed(ReviewRating.LANCAR, today = 100)
        assertEquals(1, r.reviewStage)
        assertEquals(100L + REVIEW_INTERVAL_DAYS[1], r.nextReviewDay)
    }

    @Test
    fun `lancar di tahap terakhir tidak melewati batas`() {
        val last = REVIEW_INTERVAL_DAYS.lastIndex
        val r = ayah(stage = last).reviewed(ReviewRating.LANCAR, today = 10)
        assertEquals(last, r.reviewStage)
        assertEquals(10L + REVIEW_INTERVAL_DAYS.last(), r.nextReviewDay)
    }

    @Test
    fun `ragu tetap di tahap yang sama dengan jarak dipersingkat`() {
        val r = ayah(stage = 3).reviewed(ReviewRating.RAGU, today = 50)
        assertEquals(3, r.reviewStage)
        assertEquals(50L + REVIEW_INTERVAL_DAYS[3] / 2, r.nextReviewDay)
        // Tahap 0 (jarak 1 hari): dibagi dua tetap minimal 1 hari, bukan 0 (jatuh tempo lagi hari ini).
        assertEquals(51L, ayah(stage = 0).reviewed(ReviewRating.RAGU, today = 50).nextReviewDay)
    }

    @Test
    fun `lupa kembali ke tahap nol dan diulang besok`() {
        val r = ayah(stage = 4).reviewed(ReviewRating.LUPA, today = 7)
        assertEquals(0, r.reviewStage)
        assertEquals(8L, r.nextReviewDay)
    }

    @Test
    fun `hari beruntun`() {
        assertEquals(0, streakFrom(emptyList(), 100))
        assertEquals(3, streakFrom(listOf(100, 99, 98, 96), 100))
        // Hari ini belum latihan tapi kemarin latihan: beruntun belum putus.
        assertEquals(2, streakFrom(listOf(99, 98), 100))
        // Terakhir latihan dua hari lalu: beruntun sudah putus.
        assertEquals(0, streakFrom(listOf(98, 97), 100))
    }

    @Test
    fun `pemecahan kata memisahkan tanda waqaf dari kata`() {
        val words = splitArabicWords("قُلْ اَعُوْذُ ۚ بِرَبِّ")
        assertEquals(4, words.size)
        assertTrue(words[0].hasLetter)
        assertFalse("tanda waqaf bukan kata", words[2].hasLetter)
    }

    @Test
    fun `huruf awal ikut harakatnya, bukan huruf kedua`() {
        assertEquals("قُ", firstLetterCluster("قُلْ"))
        assertEquals("بِ", firstLetterCluster("بِرَبِّ"))
        assertEquals(3, letterCount("بِرَبِّ"))
    }
}
