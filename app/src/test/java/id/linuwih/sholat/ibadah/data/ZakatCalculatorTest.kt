package id.linuwih.sholat.ibadah.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ZakatCalculatorTest {

    // ------------------------------------------------------------------
    // Zakat Fitrah
    // ------------------------------------------------------------------

    @Test
    fun `fitrah - satu jiwa`() {
        val hasil = ZakatCalculator.hitungFitrah(jumlahJiwa = 1, hargaBerasPerKg = 15_000.0)
        assertEquals(37_500.0, hasil, 0.01) // 2.5 kg * 15000
    }

    @Test
    fun `fitrah - lima jiwa`() {
        val hasil = ZakatCalculator.hitungFitrah(jumlahJiwa = 5, hargaBerasPerKg = 12_000.0)
        assertEquals(150_000.0, hasil, 0.01) // 5 * 2.5 * 12000
    }

    @Test
    fun `fitrah - nol jiwa menghasilkan nol`() {
        assertEquals(0.0, ZakatCalculator.hitungFitrah(0, 15_000.0), 0.01)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fitrah - jumlah jiwa negatif ditolak`() {
        ZakatCalculator.hitungFitrah(-1, 15_000.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fitrah - harga beras negatif ditolak`() {
        ZakatCalculator.hitungFitrah(1, -1.0)
    }

    // ------------------------------------------------------------------
    // Zakat Maal
    // ------------------------------------------------------------------

    @Test
    fun `maal - harta di atas nisab dan sudah haul wajib zakat`() {
        // nisab = 85 * 1_000_000 = 85_000_000
        val hasil = ZakatCalculator.hitungMaal(
            totalHarta = 100_000_000.0,
            hargaEmasPerGram = 1_000_000.0,
            sudahHaul = true,
        )
        assertEquals(85_000_000.0, hasil.nisab, 0.01)
        assertTrue(hasil.wajibZakat)
        assertEquals(2_500_000.0, hasil.jumlahZakat, 0.01) // 2.5% * 100jt
    }

    @Test
    fun `maal - harta di bawah nisab tidak wajib zakat`() {
        val hasil = ZakatCalculator.hitungMaal(
            totalHarta = 50_000_000.0,
            hargaEmasPerGram = 1_000_000.0,
            sudahHaul = true,
        )
        assertFalse(hasil.wajibZakat)
        assertEquals(0.0, hasil.jumlahZakat, 0.01)
    }

    @Test
    fun `maal - harta di atas nisab tapi belum haul tidak wajib zakat`() {
        val hasil = ZakatCalculator.hitungMaal(
            totalHarta = 200_000_000.0,
            hargaEmasPerGram = 1_000_000.0,
            sudahHaul = false,
        )
        assertFalse("belum haul harusnya tidak wajib walau sudah di atas nisab", hasil.wajibZakat)
        assertEquals(0.0, hasil.jumlahZakat, 0.01)
    }

    @Test
    fun `maal - tepat di angka nisab wajib zakat (batas inklusif)`() {
        val hasil = ZakatCalculator.hitungMaal(
            totalHarta = 85_000_000.0,
            hargaEmasPerGram = 1_000_000.0,
            sudahHaul = true,
        )
        assertTrue(hasil.wajibZakat)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maal - total harta negatif ditolak`() {
        ZakatCalculator.hitungMaal(-1.0, 1_000_000.0, true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `maal - harga emas negatif ditolak`() {
        ZakatCalculator.hitungMaal(100_000_000.0, -1.0, true)
    }
}
