package id.linuwih.sholat.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyTextFieldTest {

    @Test
    fun `format ribuan dasar`() {
        assertEquals("15.000", formatThousands("15000"))
        assertEquals("1.000.000", formatThousands("1000000"))
        assertEquals("100", formatThousands("100"))
        assertEquals("1", formatThousands("1"))
    }

    @Test
    fun `input kosong menghasilkan kosong`() {
        assertEquals("", formatThousands(""))
    }

    @Test
    fun `nol tunggal tetap nol, bukan kosong`() {
        assertEquals("0", formatThousands("0"))
    }

    @Test
    fun `nol di depan dibuang`() {
        assertEquals("15", formatThousands("0015"))
        assertEquals("15.000", formatThousands("00015000"))
    }

    @Test
    fun `karakter non-digit diabaikan (titik yang sudah ada tidak dobel)`() {
        // Simulasi user mengetik lanjut di atas string yang sudah terformat -- titik lama
        // harus diabaikan lalu dihitung ulang, bukan numpuk.
        assertEquals("150.000", formatThousands("15.0000"))
    }

    @Test
    fun `stripThousands mengembalikan angka mentah`() {
        assertEquals("15000", stripThousands("15.000"))
        assertEquals("1000000", stripThousands("1.000.000"))
        assertEquals("", stripThousands(""))
    }

    @Test
    fun `roundtrip format lalu strip kembali ke angka asli`() {
        val original = "123456789"
        assertEquals(original, stripThousands(formatThousands(original)))
    }
}
