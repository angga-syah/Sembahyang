package id.linuwih.sholat.quran.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TajweedEngineTest {

    // ------------------------------------------------------------------
    // Qalqalah
    // ------------------------------------------------------------------

    @Test
    fun `qalqalah - huruf dal dengan sukun terdeteksi`() {
        // يَقُولُ = tidak ada qalqalah; يَدْ = dal + sukun = qalqalah
        val text = "يَدْخُلُ"
        val rules = TajweedEngine.compute(text)
        val qalqalah = rules.filter { it.rule == "qalqalah" }
        assertEquals(1, qalqalah.size)
        val r = qalqalah[0]
        assertEquals('د', text[r.start])
        assertEquals('ْ', text[r.start + 1])
    }

    @Test
    fun `qalqalah - huruf ba dengan sukun terdeteksi`() {
        val text = "يَبْدَأُ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "qalqalah" && text[it.start] == 'ب' })
    }

    @Test
    fun `qalqalah - huruf tanpa sukun tidak terdeteksi`() {
        val text = "كَتَبَ"   // ba tanpa sukun
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.none { it.rule == "qalqalah" })
    }

    // ------------------------------------------------------------------
    // Ghunnah
    // ------------------------------------------------------------------

    @Test
    fun `ghunnah - nun bertasydid terdeteksi`() {
        val text = "إِنَّ"   // nun + tasydid + fathah
        val rules = TajweedEngine.compute(text)
        val ghunnah = rules.filter { it.rule == "ghunnah" }
        assertEquals(1, ghunnah.size)
        assertEquals('ن', text[ghunnah[0].start])
    }

    @Test
    fun `ghunnah - mim bertasydid terdeteksi`() {
        val text = "ثُمَّ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "ghunnah" && text[it.start] == 'م' })
    }

    @Test
    fun `ghunnah - span mencakup harakat setelah tasydid`() {
        // مَّ = mim + tasydid + fathah: span harus menutup harakat fathah
        val text = "ثُمَّ"
        val rules = TajweedEngine.compute(text)
        val g = rules.first { it.rule == "ghunnah" }
        // posisi setelah span bukan lagi combining mark
        if (g.end < text.length) {
            assertTrue(!isCombiningMark(text.codePointAt(g.end)))
        }
    }

    // ------------------------------------------------------------------
    // Iqlab
    // ------------------------------------------------------------------

    @Test
    fun `iqlab - nun sakinah sebelum ba terdeteksi`() {
        // مِنْ بَعْدِ = nun sakinah diikuti ba
        val text = "مِنْ بَعْدِ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "iqlab" })
    }

    @Test
    fun `iqlab - tanwin sebelum ba terdeteksi`() {
        // كِتَابٌ بِ = tanwin damma diikuti ba
        val text = "كِتَابٌ بِ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "iqlab" })
    }

    @Test
    fun `iqlab - span tidak melebihi trigger`() {
        val text = "مِنْ بَعْدِ"
        val rules = TajweedEngine.compute(text)
        val r = rules.first { it.rule == "iqlab" }
        // span berakhir setelah sukun, sebelum spasi
        assertTrue(r.end <= text.indexOf(' ') + 1)
    }

    // ------------------------------------------------------------------
    // Ikhfa
    // ------------------------------------------------------------------

    @Test
    fun `ikhfa - nun sakinah sebelum ta terdeteksi`() {
        val text = "كُنْتُمْ"  // nun + sukun eksplisit + ta
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "ikhfa" })
    }

    @Test
    fun `ikhfa - nun sakinah sebelum kaf terdeteksi`() {
        val text = "مَنْكَ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "ikhfa" })
    }

    // ------------------------------------------------------------------
    // Idgham
    // ------------------------------------------------------------------

    @Test
    fun `idgham bighunnah - nun sakinah sebelum ya terdeteksi`() {
        val text = "مِنْ يَعْمَلُ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "idghaam_ghunnah" })
    }

    @Test
    fun `idgham bilaghunnah - nun sakinah sebelum lam terdeteksi`() {
        val text = "مِنْ لَدُنْهُ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "idghaam_no_ghunnah" })
    }

    // ------------------------------------------------------------------
    // Mim sakinah
    // ------------------------------------------------------------------

    @Test
    fun `ikhfa syafawi - mim sakinah sebelum ba terdeteksi`() {
        val text = "هُمْ بِهِ"  // mim + sukun + ba
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "ikhfa_shafawi" })
    }

    @Test
    fun `idgham mimi - mim sakinah sebelum mim terdeteksi`() {
        val text = "لَهُمْ مَا"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "idghaam_shafawi" })
    }

    // ------------------------------------------------------------------
    // Madd
    // ------------------------------------------------------------------

    @Test
    fun `madd_2 - alef setelah fathah terdeteksi`() {
        // قَالَ = qaf+fathah + alef + lam+fathah
        val text = "قَالَ"
        val rules = TajweedEngine.compute(text)
        assertTrue("tidak ada madd sama sekali di '$text'", rules.any { it.rule.startsWith("madd") })
    }

    @Test
    fun `madd_2 - waw sukun setelah damma terdeteksi`() {
        // يَقُولُ = ya + qaf+damma + waw+sukun + lam+damma
        val text = "يَقُولُ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule.startsWith("madd") && text[it.start] == 'و' })
    }

    @Test
    fun `madd_2 - ya sukun setelah kasrah terdeteksi`() {
        // فِيهِ = fa+kasrah + ya+sukun + ha+kasrah
        val text = "فِيهِ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule.startsWith("madd") && text[it.start] == 'ي' })
    }

    @Test
    fun `madd_muttasil - alef sebelum hamzah satu kata terdeteksi`() {
        // جَاءَ = jim+fathah + alef + hamzah+fathah
        val text = "جَاءَ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "madd_muttasil" })
    }

    @Test
    fun `madd_munfasil - alef di akhir kata sebelum hamzah kata berikutnya`() {
        // إِنَّا أَعْطَيْنَاكَ = ...نَّا (space) أَ
        val text = "إِنَّا أَعْطَيْنَا"
        val rules = TajweedEngine.compute(text)
        assertTrue("tidak ada madd_munfasil di '$text'", rules.any { it.rule == "madd_munfasil" })
    }

    @Test
    fun `madd alef tanpa fathah sebelumnya tidak terdeteksi salah`() {
        // ٱللَّهِ – alef wasla bukan madd (bukan ا polos setelah fathah)
        val text = "ٱللَّهِ"
        val rules = TajweedEngine.compute(text)
        // alef wasla harus terdeteksi sebagai hamzat_wasl, bukan madd
        assertTrue(rules.none { it.rule.startsWith("madd") && text[it.start] == 'ٱ' })
    }

    // ------------------------------------------------------------------
    // Hamzat wasla
    // ------------------------------------------------------------------

    @Test
    fun `hamzat_wasl - alef wasla terdeteksi`() {
        val text = "ٱللَّهِ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "hamzat_wasl" })
    }

    // ------------------------------------------------------------------
    // Lam syamsiyah
    // ------------------------------------------------------------------

    @Test
    fun `lam syamsiyah - lam sebelum huruf syamsiyah bertasydid terdeteksi`() {
        // ٱلشَّمْسِ = alef wasla + lam(tanpa vokal) + syin+tasydid
        val text = "ٱلشَّمْسِ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.any { it.rule == "lam_shamsiyyah" && text[it.start] == 'ل' })
    }

    @Test
    fun `lam qamariyah tidak salah terdeteksi sebagai syamsiyah`() {
        // ٱلْقَمَرِ = lam+sukun + qaf (huruf qamariyah, tanpa tasydid) -- bukan syamsiyah
        val text = "ٱلْقَمَرِ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.none { it.rule == "lam_shamsiyyah" })
    }

    @Test
    fun `lam syamsiyah - span hanya mencakup lam, tidak huruf syamsiyah`() {
        val text = "ٱلشَّمْسِ"
        val rules = TajweedEngine.compute(text)
        val r = rules.first { it.rule == "lam_shamsiyyah" }
        assertEquals('ل', text[r.start])
        // huruf sesudah span bukan lagi lam
        assertTrue(text[r.end] != 'ل')
    }

    // ------------------------------------------------------------------
    // Huruf silent
    // ------------------------------------------------------------------

    @Test
    fun `silent - alef setelah wau jamaah di akhir kata terdeteksi`() {
        // قَالُوا۟ = qaf+fathah + lam+damma + waw(madd) + alef(silent) + tanda sukun kecil
        val text = "قَالُوا۟"
        val rules = TajweedEngine.compute(text)
        assertTrue("tidak ada silent di '$text'", rules.any { it.rule == "silent" })
    }

    @Test
    fun `silent - span hanya mencakup alef, bukan waw`() {
        val text = "قَالُوا۟"
        val rules = TajweedEngine.compute(text)
        val r = rules.first { it.rule == "silent" }
        assertEquals('ا', text[r.start])
    }

    @Test
    fun `alef biasa setelah waw berharakat tidak salah terdeteksi silent`() {
        // وَاحِدٌ = waw+fathah (bukan sukun/madd) + alef -- alef ini madd biasa, bukan silent
        val text = "وَاحِدٌ"
        val rules = TajweedEngine.compute(text)
        assertTrue(rules.none { it.rule == "silent" })
    }

    // ------------------------------------------------------------------
    // Invariant: tidak ada overlap span
    // ------------------------------------------------------------------

    @Test
    fun `tidak ada dua aturan yang span-nya saling tumpang tindih`() {
        // Ayat yang kaya aturan: Al-Baqarah 2:19
        val text = "صَيِّبٍ مِّنَ السَّمَاۤءِ فِيْهِ ظُلُمٰتٌ وَّرَعْدٌ وَّبَرْقٌۚ"
        val rules = TajweedEngine.compute(text)
        for (i in 0 until rules.lastIndex) {
            assertTrue(
                "span ${rules[i]} dan ${rules[i + 1]} tumpang tindih",
                rules[i].end <= rules[i + 1].start,
            )
        }
    }

    @Test
    fun `semua nama aturan dikenal oleh TajweedColors`() {
        val text = "صَيِّبٍ مِّنَ السَّمَاۤءِ فِيْهِ ظُلُمٰتٌ وَّرَعْدٌ وَّبَرْقٌۚ"
        val rules = TajweedEngine.compute(text)
        rules.forEach { rule ->
            assertTrue(
                "aturan '${rule.rule}' tidak ada di peta warna",
                tajweedColorFor(rule.rule) != androidx.compose.ui.graphics.Color.Unspecified,
            )
        }
    }

    @Test
    fun `semua batas span valid dalam teks`() {
        val text = "وَمَا أَنزَلْنَا عَلَيْكَ الْكِتَابَ إِلَّا لِتُبَيِّنَ لَهُمُ الَّذِي اخْتَلَفُوا فِيهِ"
        val rules = TajweedEngine.compute(text)
        rules.forEach { rule ->
            assertTrue("start ${rule.start} < 0", rule.start >= 0)
            assertTrue("end ${rule.end} > text.length", rule.end <= text.length)
            assertTrue("start >= end: $rule", rule.start < rule.end)
        }
    }
}
