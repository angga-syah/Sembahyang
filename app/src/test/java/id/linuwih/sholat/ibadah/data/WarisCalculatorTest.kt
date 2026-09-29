package id.linuwih.sholat.ibadah.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kasus-kasus di sini adalah contoh baku yang dimuat di kitab-kitab faraid (dipilih karena
 * jawabannya diketahui pasti secara ijma', bukan diambil dari kalkulator lain) — dipakai untuk
 * memverifikasi WarisCalculator menghasilkan angka yang SAMA, bukan sekadar "masuk akal".
 */
class WarisCalculatorTest {

    private fun totalFraksi(hasil: WarisCalculator.HasilWaris) = hasil.bagian.sumOf { it.fraksi }

    @Test
    fun `gharrawain - suami ayah ibu tanpa anak`() {
        // Kasus 'umariyyatan/gharrawain: ibu dapat 1/3 dari SISA (bukan 1/3 total).
        // Jawaban baku: suami 1/2, ayah 1/3, ibu 1/6.
        val hasil = WarisCalculator.hitung(
            WarisCalculator.Ahli(pewarisLakiLaki = false, suamiHidup = true, ayahHidup = true, ibuHidup = true),
        )
        val suami = hasil.bagian.first { it.ahli == "Suami" }.fraksi
        val ibu = hasil.bagian.first { it.ahli == "Ibu" }.fraksi
        val ayah = hasil.bagian.filter { it.ahli.startsWith("Ayah") }.sumOf { it.fraksi }

        assertEquals(0.5, suami, 1e-9)
        assertEquals(1.0 / 6, ibu, 1e-9)
        assertEquals(1.0 / 3, ayah, 1e-9)
        assertEquals(1.0, totalFraksi(hasil), 1e-9)
        assertFalse(hasil.adaAul)
        assertFalse(hasil.butuhRadd)
    }

    @Test
    fun `satu anak perempuan, ayah, ibu, tanpa pasangan`() {
        // Jawaban baku: anak perempuan 1/2, ayah 1/6 + ashabah 1/6 = 1/3, ibu 1/6.
        val hasil = WarisCalculator.hitung(
            WarisCalculator.Ahli(pewarisLakiLaki = true, anakPerempuan = 1, ayahHidup = true, ibuHidup = true),
        )
        val anak = hasil.bagian.first { it.ahli == "Anak perempuan" }.fraksi
        val ayah = hasil.bagian.filter { it.ahli.startsWith("Ayah") }.sumOf { it.fraksi }
        val ibu = hasil.bagian.first { it.ahli == "Ibu" }.fraksi

        assertEquals(0.5, anak, 1e-9)
        assertEquals(1.0 / 3, ayah, 1e-9)
        assertEquals(1.0 / 6, ibu, 1e-9)
        assertEquals(1.0, totalFraksi(hasil), 1e-9)
    }

    @Test
    fun `aul - suami, 2 anak perempuan, ibu`() {
        // Asal masalah 12 -> 'aul jadi 13. Jawaban baku: suami 3/13, anak perempuan (gabungan)
        // 8/13, ibu 2/13.
        val hasil = WarisCalculator.hitung(
            WarisCalculator.Ahli(pewarisLakiLaki = false, suamiHidup = true, anakPerempuan = 2, ibuHidup = true),
        )
        val suami = hasil.bagian.first { it.ahli == "Suami" }.fraksi
        val anak = hasil.bagian.first { it.ahli == "Anak perempuan" }.fraksi
        val ibu = hasil.bagian.first { it.ahli == "Ibu" }.fraksi

        assertTrue(hasil.adaAul)
        assertEquals(3.0 / 13, suami, 1e-9)
        assertEquals(8.0 / 13, anak, 1e-9)
        assertEquals(2.0 / 13, ibu, 1e-9)
        assertEquals(1.0, totalFraksi(hasil), 1e-9)
    }

    @Test
    fun `anak laki-laki tunggal tanpa ahli waris lain mendapat seluruh harta`() {
        val hasil = WarisCalculator.hitung(WarisCalculator.Ahli(pewarisLakiLaki = true, anakLakiLaki = 1))
        assertEquals(1.0, hasil.bagian.sumOf { it.fraksi }, 1e-9)
        assertFalse(hasil.butuhRadd)
    }

    @Test
    fun `2 anak laki-laki dan 1 anak perempuan berbagi rasio 2- 2- 1`() {
        val hasil = WarisCalculator.hitung(
            WarisCalculator.Ahli(pewarisLakiLaki = true, anakLakiLaki = 2, anakPerempuan = 1),
        )
        val lakiLaki = hasil.bagian.first { it.ahli.startsWith("Anak laki-laki") }
        val perempuan = hasil.bagian.first { it.ahli.startsWith("Anak perempuan") }

        assertEquals(4.0 / 5, lakiLaki.fraksi, 1e-9) // 2 anak x 2 unit = 4 dari 5 unit total
        assertEquals(1.0 / 5, perempuan.fraksi, 1e-9) // 1 anak x 1 unit = 1 dari 5 unit total
        assertEquals(2, lakiLaki.jumlahJiwa)
        assertEquals(1, perempuan.jumlahJiwa)
        assertEquals(1.0, totalFraksi(hasil), 1e-9)
    }

    @Test
    fun `istri tunggal tanpa ahli waris lain memicu butuhRadd, bukan ditebak`() {
        val hasil = WarisCalculator.hitung(
            WarisCalculator.Ahli(pewarisLakiLaki = true, jumlahIstriHidup = 1),
        )
        assertTrue("sisa harta tanpa ashabah harus ditandai butuh radd, bukan diam-diam hilang", hasil.butuhRadd)
    }

    @Test
    fun `istri dan anak laki-laki - istri dapat 1-8 karena ada keturunan`() {
        val hasil = WarisCalculator.hitung(
            WarisCalculator.Ahli(pewarisLakiLaki = true, jumlahIstriHidup = 1, anakLakiLaki = 1),
        )
        val istri = hasil.bagian.first { it.ahli == "Istri" }.fraksi
        assertEquals(1.0 / 8, istri, 1e-9)
        assertEquals(1.0, totalFraksi(hasil), 1e-9)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `jumlah istri di atas 4 ditolak`() {
        WarisCalculator.hitung(WarisCalculator.Ahli(pewarisLakiLaki = true, jumlahIstriHidup = 5))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `jumlah anak negatif ditolak`() {
        WarisCalculator.hitung(WarisCalculator.Ahli(pewarisLakiLaki = true, anakLakiLaki = -1))
    }
}
