package id.linuwih.sholat.quran.ui

import androidx.compose.ui.graphics.Color
import id.linuwih.sholat.quran.data.TajweedRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Mengunci satu invarian: menyalakan tajwid HANYA boleh mengubah warna, tidak boleh mengubah
 * struktur span sedikit pun.
 *
 * Kenapa sekaku ini — android.text.TextLine memecah baris menjadi potongan gambar di setiap
 * batas span CharacterStyle, lalu mengakumulasi offset x per potongan. Jumlah span yang berbeda
 * antar mode berarti pemecahan yang berbeda, akumulasi yang berbeda, dan glyph yang tergambar
 * bergeser. Dulu mode non-tajwid dibangun dengan emptyList() -- nol span melawan puluhan span --
 * dan itu yang membuat harakat terlihat geser saat tajwid dinyalakan.
 *
 * Perilaku ini sudah beberapa kali hilang lagi karena dikira "warna tidak mungkin berpengaruh".
 * Test ini ada supaya perubahan seperti itu gagal di build, bukan di mata pembaca Al-Qur'an.
 */
class TajweedTextTest {

    private val plain = Color(0xFF101010)

    /** Potongan Al-Baqarah 2:19 -- mengandung mim kecil iqlab dan beberapa aturan bersebelahan. */
    private val text = "صَيِّبٍ مِّنَ السَّمَاۤءِ فِيْهِ ظُلُمٰتٌ وَّرَعْدٌ وَّبَرْقٌۚ"

    private val rules = listOf(
        TajweedRule(0, 7, "ghunnah"),
        TajweedRule(8, 13, "idgham-bighunnah"),
        TajweedRule(17, 23, "madd-wajib-muttasil"),
        TajweedRule(33, 40, "madd-arid-lissukun"),
        TajweedRule(48, 54, "qalqalah"),
    )

    @Test
    fun `menyalakan tajwid tidak mengubah satu pun batas span`() {
        val off = buildTajweedAnnotatedString(text, rules, tajweedEnabled = false, plainColor = plain)
        val on = buildTajweedAnnotatedString(text, rules, tajweedEnabled = true, plainColor = plain)

        assertEquals("teksnya harus sama persis", off.text, on.text)
        assertEquals("jumlah span berbeda antar mode", off.spanStyles.size, on.spanStyles.size)
        off.spanStyles.indices.forEach { i ->
            assertEquals("batas awal span $i bergeser", off.spanStyles[i].start, on.spanStyles[i].start)
            assertEquals("batas akhir span $i bergeser", off.spanStyles[i].end, on.spanStyles[i].end)
        }
    }

    @Test
    fun `yang berubah memang warnanya`() {
        val off = buildTajweedAnnotatedString(text, rules, tajweedEnabled = false, plainColor = plain)
        val on = buildTajweedAnnotatedString(text, rules, tajweedEnabled = true, plainColor = plain)

        assertTrue("tidak ada span sama sekali; test jadi tidak bermakna", off.spanStyles.isNotEmpty())
        off.spanStyles.forEach { assertEquals(plain, it.item.color) }
        assertTrue(
            "mode tajwid tidak memberi warna berbeda satu pun",
            on.spanStyles.any { it.item.color != plain },
        )
    }

    @Test
    fun `aturan tak dikenal tetap menghasilkan span, bukan dihilangkan diam-diam`() {
        // Compose TIDAK membuat span untuk Color.Unspecified. Kalau warna aturan tak dikenal
        // diteruskan apa adanya, jumlah span menyusut dan invarian di atas jebol tanpa terlihat.
        val aneh = listOf(TajweedRule(0, 7, "aturan-yang-tidak-ada-di-peta-warna"))
        val on = buildTajweedAnnotatedString(text, aneh, tajweedEnabled = true, plainColor = plain)
        val off = buildTajweedAnnotatedString(text, aneh, tajweedEnabled = false, plainColor = plain)

        assertEquals(1, on.spanStyles.size)
        assertEquals(1, off.spanStyles.size)
        assertEquals(plain, on.spanStyles.single().item.color)
        assertNotEquals(Color.Unspecified, on.spanStyles.single().item.color)
    }

    @Test
    fun `batas warna tidak pernah memisahkan huruf dari harakatnya`() {
        val on = buildTajweedAnnotatedString(text, rules, tajweedEnabled = true, plainColor = plain)
        on.spanStyles.forEach { span ->
            if (span.start > 0) {
                assertTrue(
                    "span mulai di tengah harakat pada index ${span.start}",
                    !isCombiningMark(text.codePointAt(span.start)),
                )
            }
            if (span.end < text.length) {
                assertTrue(
                    "span berakhir sebelum harakat pada index ${span.end}: huruf berwarna tapi harakatnya tidak",
                    !isCombiningMark(text.codePointAt(span.end)),
                )
            }
        }
    }

    @Test
    fun `pelebaran batas tidak mencaplok wilayah aturan tetangga`() {
        // Dua aturan berdempetan: yang kedua tidak boleh ditarik mundur melewati akhir yang pertama.
        val berdempetan = listOf(TajweedRule(0, 7, "ghunnah"), TajweedRule(7, 13, "qalqalah"))
        val on = buildTajweedAnnotatedString(text, berdempetan, tajweedEnabled = true, plainColor = plain)
        assertEquals(2, on.spanStyles.size)
        assertTrue(
            "span saling tumpang tindih: warna akan jatuh di huruf yang salah",
            on.spanStyles[0].end <= on.spanStyles[1].start,
        )
    }
}
