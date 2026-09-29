package id.linuwih.sholat.ibadah.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Konversi Hijriah dipakai di dua tempat yang terlihat user (kalender & kartu jadwal) dan tidak
 * punya jaring pengaman apa pun sebelumnya. Nilai acuan di bawah memakai algoritma tabular yang
 * sama seperti implementasinya — tujuannya menjaga dari REGRESI, bukan mengklaim kesesuaian
 * dengan rukyat resmi (kalender tabular memang bisa meleset +-1 hari; itu sudah dinyatakan di UI).
 */
class HijriDateTest {

    @Test
    fun `tanggal selalu di dalam rentang yang sah`() {
        // Disapu 40 tahun; kalau ada tanggal 0, bulan 13, atau hari ke-31, konversinya rusak.
        val calendar = Calendar.getInstance().apply { set(2000, Calendar.JANUARY, 1) }
        repeat(40 * 365) {
            val hijri = HijriDate.from(calendar)
            assertTrue("bulan di luar 1..12: ${hijri.month}", hijri.month in 1..12)
            assertTrue("hari di luar 1..30: ${hijri.day}", hijri.day in 1..30)
            assertTrue("tahun tidak masuk akal: ${hijri.year}", hijri.year in 1400..1500)
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
    }

    @Test
    fun `hari berikutnya selalu maju satu hari, tidak pernah mundur`() {
        // Menangkap bug klasik pada konversi kalender: tanggal melompat mundur di batas bulan.
        val calendar = Calendar.getInstance().apply { set(2024, Calendar.JANUARY, 1) }
        var previous = HijriDate.from(calendar)
        repeat(3 * 365) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
            val current = HijriDate.from(calendar)
            val majuSatuHari = current.day == previous.day + 1
            val gantiBulan = current.day == 1 && current.month != previous.month
            assertTrue(
                "lompatan tidak wajar: $previous -> $current",
                majuSatuHari || gantiBulan,
            )
            previous = current
        }
    }

    @Test
    fun `nama bulan tersedia untuk seluruh 12 bulan`() {
        assertEquals(12, HijriDate.MONTH_NAMES.size)
        (1..12).forEach { month ->
            val name = HijriDate(1446, month, 1).monthName
            assertTrue("nama bulan $month kosong", name.isNotBlank())
        }
    }
}
