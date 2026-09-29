package id.linuwih.sholat.quran.ui

import id.linuwih.sholat.quran.data.ArabicFont
import id.linuwih.sholat.quran.data.MushafStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile

/**
 * Menjaga agar tiap gaya mushaf dipasangkan dengan font yang benar-benar punya glyph untuk
 * SELURUH karakternya.
 *
 * Ini bukan uji teoretis. Sebelum test ini ada, teks Kemenag dirender dengan Amiri Quran, dan
 * 689 karakter (a.l. U+08D6 ARABIC SMALL HIGH AIN sebanyak 560 kali) tidak punya glyph di font
 * itu. Android lalu jatuh ke font sistem DI TENGAH KATA, yang memecah shaping run sehingga
 * harakat di sekitarnya meleset — persis keluhan "harakat berantakan" yang dilaporkan user.
 * Tidak ada error, tidak ada warning; cuma tampilan yang salah.
 *
 * Karena itu kegagalan di sini harus dianggap serius: menambah mushaf atau mengganti font tanpa
 * memeriksa cakupannya akan mengulang bug yang sama.
 */
class ArabicFontCoverageTest {

    @Test
    fun `font tiap gaya mushaf meng-cover seluruh karakter teksnya`() {
        MushafStyle.entries.forEach { style ->
            val font = ArabicFont.OTOMATIS.resolve(style)
            val covered = codepointsIn(fontFileFor(font))
            val missing = arabicCodepointsIn(textFileFor(style))
                .filterNot { it in covered }
                .sorted()

            assertTrue(
                "Gaya mushaf $style dirender dengan $font, tapi ${missing.size} karakter tidak " +
                    "punya glyph di font itu: " +
                    missing.joinToString { "U+%04X".format(it) } +
                    " -- Android akan jatuh ke font sistem di tengah kata dan merusak posisi harakat.",
                missing.isEmpty(),
            )
        }
    }

    /**
     * Bukti bahwa test di atas tidak lolos secara semu. Kalau pembaca cmap di bawah rusak dan
     * mengembalikan "semua ter-cover", test ini yang akan gagal lebih dulu.
     *
     * Sekaligus mengunci pasangan yang DULU dipakai dan salah: Amiri Quran adalah font rasm
     * Utsmani, bukan Mushaf Standar Indonesia.
     */
    @Test
    fun `Amiri Quran memang tidak meng-cover teks Kemenag -- pembaca cmap berfungsi`() {
        val covered = codepointsIn(fontFileFor(ArabicFont.AMIRI_QURAN))
        val missing = arabicCodepointsIn(textFileFor(MushafStyle.KEMENAG)).filterNot { it in covered }

        assertTrue("pembaca cmap tampaknya rusak: Amiri diklaim meng-cover seluruh teks Kemenag", missing.isNotEmpty())
        assertTrue(
            "U+08D6 (khas Mushaf Standar Indonesia) seharusnya tidak ada di Amiri Quran",
            0x08D6 in missing,
        )
        assertTrue("cakupan Amiri terbaca terlalu sedikit, pembaca cmap patut dicurigai", covered.size > 200)
    }

    @Test
    fun `OTOMATIS selalu menghasilkan font konkret, tidak pernah dirinya sendiri`() {
        MushafStyle.entries.forEach { style ->
            val resolved = ArabicFont.OTOMATIS.resolve(style)
            assertTrue("resolve($style) mengembalikan OTOMATIS", resolved != ArabicFont.OTOMATIS)
        }
        // Pilihan manual tidak boleh diam-diam ditimpa oleh gaya mushaf.
        assertEquals(ArabicFont.AMIRI_QURAN, ArabicFont.AMIRI_QURAN.resolve(MushafStyle.KEMENAG))
        assertEquals(ArabicFont.SYSTEM, ArabicFont.SYSTEM.resolve(MushafStyle.UTSMANI))
    }

    private fun fontFileFor(font: ArabicFont): File = when (font) {
        ArabicFont.LPMQ_ISEP_MISBAH -> file("src/main/res/font/lpmq_isep_misbah.ttf")
        ArabicFont.AMIRI_QURAN -> file("src/main/res/font/amiri_quran.ttf")
        // SYSTEM tidak punya berkas; OTOMATIS sudah di-resolve sebelum sampai sini.
        else -> throw IllegalArgumentException("tidak ada berkas font untuk $font")
    }

    private fun textFileFor(style: MushafStyle): File = when (style) {
        MushafStyle.KEMENAG -> file("src/main/assets/quran/quran_ar_kemenag.json")
        MushafStyle.UTSMANI -> file("src/main/assets/quran/quran_ar_tajweed.json")
    }

    private fun file(path: String): File = File(path).also {
        assertTrue("berkas tidak ditemukan: ${it.absolutePath}", it.isFile)
    }

    /**
     * Memindai code point Arab langsung dari berkas mentah, tanpa parser JSON — struktur JSON
     * seluruhnya ASCII, jadi apa pun di rentang Arab pasti bagian teks yang akan dirender.
     */
    private fun arabicCodepointsIn(file: File): Set<Int> {
        val arabicRanges = listOf(
            0x0600..0x06FF, // Arabic
            0x0750..0x077F, // Arabic Supplement
            0x08A0..0x08FF, // Arabic Extended-A (mis. U+08D6, khas Mushaf Standar Indonesia)
            0xFB50..0xFDFF, // Arabic Presentation Forms-A
            0xFE70..0xFEFF, // Arabic Presentation Forms-B
        )
        val result = mutableSetOf<Int>()
        file.readText().codePoints().forEach { cp ->
            if (arabicRanges.any { cp in it }) result.add(cp)
        }
        return result
    }

    /** Pembaca cmap TrueType minimal — cukup untuk menjawab "font ini punya glyph untuk X?". */
    private fun codepointsIn(font: File): Set<Int> = RandomAccessFile(font, "r").use { raf ->
        raf.seek(4)
        val tableCount = raf.readUnsignedShort()
        raf.skipBytes(6)
        var cmapOffset = -1L
        repeat(tableCount) {
            val tag = ByteArray(4).also { raf.readFully(it) }.toString(Charsets.US_ASCII)
            raf.skipBytes(4) // checksum
            val offset = raf.readInt().toLong() and 0xFFFFFFFFL
            raf.skipBytes(4) // length
            if (tag == "cmap") cmapOffset = offset
        }
        assertTrue("font tanpa tabel cmap: ${font.name}", cmapOffset >= 0)

        raf.seek(cmapOffset + 2)
        val subtableCount = raf.readUnsignedShort()
        val subtableOffsets = mutableListOf<Long>()
        repeat(subtableCount) {
            raf.skipBytes(4) // platformID + encodingID
            subtableOffsets += cmapOffset + (raf.readInt().toLong() and 0xFFFFFFFFL)
        }

        // Gabungan semua subtabel: satu code point cukup ada di salah satunya.
        val result = mutableSetOf<Int>()
        subtableOffsets.forEach { offset ->
            raf.seek(offset)
            when (raf.readUnsignedShort()) {
                4 -> readFormat4(raf, result)
                12 -> readFormat12(raf, offset, result)
                else -> Unit // format lain tidak dipakai font-font ini
            }
        }
        result
    }

    private fun readFormat4(raf: RandomAccessFile, out: MutableSet<Int>) {
        raf.skipBytes(4) // length + language
        val segCountX2 = raf.readUnsignedShort()
        val segCount = segCountX2 / 2
        raf.skipBytes(6) // searchRange + entrySelector + rangeShift
        val endCodes = IntArray(segCount) { raf.readUnsignedShort() }
        raf.skipBytes(2) // reservedPad
        val startCodes = IntArray(segCount) { raf.readUnsignedShort() }
        val idDeltas = IntArray(segCount) { raf.readShort().toInt() }
        val idRangeOffsetPos = raf.filePointer
        val idRangeOffsets = IntArray(segCount) { raf.readUnsignedShort() }

        for (seg in 0 until segCount) {
            if (startCodes[seg] == 0xFFFF) continue
            for (cp in startCodes[seg]..endCodes[seg]) {
                val glyph = if (idRangeOffsets[seg] == 0) {
                    (cp + idDeltas[seg]) and 0xFFFF
                } else {
                    val at = idRangeOffsetPos + seg * 2 + idRangeOffsets[seg] + (cp - startCodes[seg]) * 2
                    raf.seek(at)
                    val g = raf.readUnsignedShort()
                    if (g == 0) 0 else (g + idDeltas[seg]) and 0xFFFF
                }
                if (glyph != 0) out.add(cp)
            }
        }
    }

    private fun readFormat12(raf: RandomAccessFile, offset: Long, out: MutableSet<Int>) {
        raf.seek(offset + 12)
        val groupCount = raf.readInt()
        repeat(groupCount) {
            val start = raf.readInt()
            val end = raf.readInt()
            val startGlyph = raf.readInt()
            if (startGlyph != 0) for (cp in start..end) out.add(cp)
        }
    }
}
