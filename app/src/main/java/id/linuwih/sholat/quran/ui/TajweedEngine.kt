package id.linuwih.sholat.quran.ui

import id.linuwih.sholat.quran.data.TajweedRule

/**
 * Menghitung aturan tajwid langsung dari teks Arab tanpa dataset yang diindeks sebelumnya.
 *
 * Pendekatan ini menghilangkan masalah sinkronisasi indeks-teks: dataset menyimpan indeks
 * huruf di berkas terpisah dari teksnya, dan keduanya harus tetap selaras seumur hidup
 * aplikasi. Dengan menghitung dari teks yang sebenarnya dirender, masalah itu tidak ada.
 *
 * Nama aturan yang dikembalikan cocok dengan kunci di [tajweedColorFor] (TajweedColors.kt).
 * Cakupan: qalqalah, ghunnah, iqlab, ikhfa, idgham (4 jenis), ikhfa syafawi, idgham mimi,
 * madd (natural/muttasil/munfasil/lazim/arid), hamzat wasla, lam syamsiyah, huruf silent.
 */
object TajweedEngine {

    // --- Tanda baca ---
    private const val SUKUN        = 'ْ' // U+0652
    private const val TASYDID      = 'ّ' // U+0651
    private const val FATHAH       = 'َ' // U+064E
    private const val DAMMA        = 'ُ' // U+064F
    private const val KASRAH       = 'ِ' // U+0650
    private const val MADDAH       = 'ٓ' // U+0653 (maddah combining)
    private const val TANWIN_FATH  = 'ً' // U+064B
    private const val TANWIN_KASRA = 'ٍ' // U+064D
    private const val TANWIN_DAMMA = 'ٌ' // U+064C

    // --- Huruf utama ---
    private const val NUN        = 'ن' // U+0646
    private const val MIM        = 'م' // U+0645
    private const val BA         = 'ب' // U+0628
    private const val ALEF       = 'ا' // U+0627 – alef polos (carrier mad panjang)
    private const val ALEF_MAD   = 'آ' // U+0622 – alef maddah (precomposed)
    private const val ALEF_WASLA = 'ٱ' // U+0671 – alef wasla / hamzat wasl

    // Huruf qalqalah: ق ط ب ج د
    private val QALQALAH = setOf('ق', 'ط', 'ب', 'ج', 'د')

    // Huruf ikhfa (15 huruf)
    private val IKHFA = setOf(
        'ت', 'ث', 'ج', 'د', 'ذ',
        'ز', 'س', 'ش', 'ص', 'ض',
        'ط', 'ظ', 'ف', 'ق', 'ك',
    )

    // Huruf idgham bighunnah: ي ن م و
    private val IDGHAM_GHUNNAH = setOf('ي', 'ن', 'م', 'و')

    // Huruf idgham bilaghunnah: ر ل
    private val IDGHAM_NO_GHUNNAH = setOf('ر', 'ل')

    // Hamzah dalam berbagai bentuk: ء أ إ ؤ ئ آ
    private val HAMZAH = setOf('ء', 'أ', 'إ', 'ؤ', 'ئ', 'آ')

    // 14 huruf syamsiyah — lam artikel "al-" melebur ke huruf ini (ditandai tasydid di huruf
    // berikutnya, lam sendiri tidak dibaca).
    private val SUN_LETTERS = setOf(
        'ت', 'ث', 'د', 'ذ', 'ر',
        'ز', 'س', 'ش', 'ص', 'ض',
        'ط', 'ظ', 'ل', 'ن',
    )

    /** Hitung semua aturan tajwid untuk satu ayat. Hasilnya diurutkan berdasarkan posisi. */
    fun compute(text: String): List<TajweedRule> = buildList {
        addAll(findQalqalah(text))
        addAll(findGhunnah(text))
        addAll(findNunSakinahRules(text))
        addAll(findMimSakinahRules(text))
        addAll(findMadd(text))
        addAll(findHamzatWasl(text))
        addAll(findLamShamsiyyah(text))
        addAll(findSilentLetters(text))
    }.sortedBy { it.start }

    // ------------------------------------------------------------------
    // Qalqalah: huruf ق ط ب ج د + sukun
    // ------------------------------------------------------------------
    private fun findQalqalah(text: String): List<TajweedRule> = buildList {
        var i = 0
        while (i < text.length - 1) {
            if (text[i] in QALQALAH && text[i + 1] == SUKUN) {
                add(TajweedRule(i, i + 2, "qalqalah"))
                i += 2
            } else i++
        }
    }

    // ------------------------------------------------------------------
    // Ghunnah: نّ atau مّ
    //
    // Tasydid bisa muncul sebelum atau sesudah harakat tergantung enkoding teks — urutan
    // UTF-16 tidak selalu huruf→tasydid langsung. Contoh: "إِنَّ" bisa mengandung
    // NUN + FATHA + TASYDID atau NUN + TASYDID + FATHA. Keduanya harus terdeteksi.
    // ------------------------------------------------------------------
    private fun findGhunnah(text: String): List<TajweedRule> = buildList {
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == NUN || ch == MIM) {
                if (hasTasydidInCombining(text, i + 1)) {
                    val end = advancePastCombining(text, i + 1)
                    add(TajweedRule(i, end, "ghunnah"))
                    i = end
                    continue
                }
            }
            i++
        }
    }

    /** True jika ada tasydid di antara combining mark yang langsung mengikuti posisi [from]. */
    private fun hasTasydidInCombining(text: String, from: Int): Boolean {
        var i = from
        while (i < text.length && isCombiningMark(text.codePointAt(i))) {
            if (text[i] == TASYDID) return true
            i++
        }
        return false
    }

    // ------------------------------------------------------------------
    // Aturan nun sakinah / tanwin: iqlab, ikhfa, idgham bighunnah, idgham bilaghunnah
    //
    // Span: hanya mencakup pemicu (nun+sukun atau huruf yang menanggung tanwin), tidak
    // meluas ke huruf berikutnya. Ini menghindari overlap dengan aturan huruf berikutnya
    // (misal ba yang juga bisa qalqalah) dan konsisten dengan penanda visual HijrahApp.
    // ------------------------------------------------------------------
    private fun findNunSakinahRules(text: String): List<TajweedRule> = buildList {
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            val isNunSakin = ch == NUN && i + 1 < text.length && text[i + 1] == SUKUN
            val isTanwin = ch == TANWIN_FATH || ch == TANWIN_KASRA || ch == TANWIN_DAMMA

            if (!isNunSakin && !isTanwin) { i++; continue }

            // Lewati sukun (untuk nun sakinah) atau harakat (untuk tanwin) lalu cari
            // huruf Arab pertama sesudahnya — boleh melewati spasi dan harakat.
            val afterTrigger = if (isNunSakin) i + 2 else i + 1
            val nextIdx = nextArabicBase(text, afterTrigger)
            if (nextIdx >= text.length) { i++; continue }

            val nextCh = text[nextIdx]
            // Start span: untuk tanwin, mundur ke huruf dasar yang menanggungnya.
            val spanStart = if (isTanwin) retreatToBase(text, i) else i
            val spanEnd = afterTrigger

            val ruleName = when (nextCh) {
                BA                    -> "iqlab"
                in IKHFA              -> "ikhfa"
                in IDGHAM_GHUNNAH     -> "idghaam_ghunnah"
                in IDGHAM_NO_GHUNNAH  -> "idghaam_no_ghunnah"
                else                  -> null
            }
            if (ruleName != null) {
                add(TajweedRule(spanStart, spanEnd, ruleName))
                i = spanEnd
            } else i++
        }
    }

    // ------------------------------------------------------------------
    // Aturan mim sakinah: ikhfa syafawi (مْب) dan idgham mimi (مْم)
    // ------------------------------------------------------------------
    private fun findMimSakinahRules(text: String): List<TajweedRule> = buildList {
        var i = 0
        while (i < text.length - 1) {
            if (text[i] != MIM || text[i + 1] != SUKUN) { i++; continue }

            val afterMimSakin = i + 2
            val nextIdx = nextArabicBase(text, afterMimSakin)
            if (nextIdx >= text.length) { i++; continue }

            val ruleName = when (text[nextIdx]) {
                BA  -> "ikhfa_shafawi"
                MIM -> "idghaam_shafawi"
                else -> null
            }
            if (ruleName != null) {
                add(TajweedRule(i, afterMimSakin, ruleName))
                i = afterMimSakin
            } else i++
        }
    }

    // ------------------------------------------------------------------
    // Madd (pemanjangan): alef, وْ setelah dammah, يْ setelah kasrah
    //
    // Klasifikasi berdasarkan konteks sesudahnya:
    //   madd_muttasil : hamzah dalam kata yang sama       (4-5 ketukan)
    //   madd_munfasil : hamzah di kata berikutnya         (2-5 ketukan)
    //   madd_6        : huruf berikutnya bersukun/tasydid (6 ketukan, lazim)
    //   madd_246      : akhir ayat / tanda waqf           (arid lissukun)
    //   madd_2        : konteks biasa                     (2 ketukan)
    //
    // ALEF POLOS (ا U+0627) hanya dianggap madd bila:
    //   – huruf sebelumnya berfathah, DAN
    //   – alef sendiri tidak punya harakat (bukan kursi hamzah أ/إ/ؤ/ئ).
    // Alef di awal kata tidak mempunyai huruf berfathah sebelumnya, jadi otomatis terlewat.
    // ------------------------------------------------------------------
    private fun findMadd(text: String): List<TajweedRule> = buildList {
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            when {
                // آ (alef maddah precomposed) = selalu madd
                ch == ALEF_MAD -> {
                    val madEnd = i + 1
                    add(TajweedRule(i, madEnd, classifyMadd(text, madEnd)))
                    i = madEnd
                }

                // ا polos + maddah combining (versi terurai dari آ)
                ch == ALEF && i + 1 < text.length && text[i + 1] == MADDAH -> {
                    val madEnd = advancePastCombining(text, i + 1)
                    add(TajweedRule(i, madEnd, classifyMadd(text, madEnd)))
                    i = madEnd
                }

                // ا polos setelah fathah, tanpa harakat sendiri = madd alef
                ch == ALEF -> {
                    val noOwnVowel = i + 1 >= text.length || !isCombiningMark(text.codePointAt(i + 1))
                    if (precedingVowel(text, i) == FATHAH && noOwnVowel) {
                        val madEnd = i + 1
                        add(TajweedRule(i, madEnd, classifyMadd(text, madEnd)))
                        i = madEnd
                    } else i++
                }

                // و = madd waw bila:
                //   (a) punya sukun eksplisit (وْ), ATAU
                //   (b) tidak punya harakat apapun (و polos) — encoding tanpa sukun eksplisit
                // …dan huruf sebelumnya berdammah.
                ch == 'و' -> {
                    val hasSukun = i + 1 < text.length && text[i + 1] == SUKUN
                    val noDiacritic = i + 1 >= text.length || !isCombiningMark(text.codePointAt(i + 1))
                    if ((hasSukun || noDiacritic) && precedingVowel(text, i) == DAMMA) {
                        val madEnd = if (hasSukun) i + 2 else i + 1
                        add(TajweedRule(i, madEnd, classifyMadd(text, madEnd)))
                        i = madEnd
                    } else i++
                }

                // ي = madd ya, logika sama dengan waw di atas, tapi kasrah.
                ch == 'ي' -> {
                    val hasSukun = i + 1 < text.length && text[i + 1] == SUKUN
                    val noDiacritic = i + 1 >= text.length || !isCombiningMark(text.codePointAt(i + 1))
                    if ((hasSukun || noDiacritic) && precedingVowel(text, i) == KASRAH) {
                        val madEnd = if (hasSukun) i + 2 else i + 1
                        add(TajweedRule(i, madEnd, classifyMadd(text, madEnd)))
                        i = madEnd
                    } else i++
                }

                else -> i++
            }
        }
    }

    /** Klasifikasi jenis madd berdasarkan konteks setelah huruf madd (posisi [afterMad]). */
    private fun classifyMadd(text: String, afterMad: Int): String {
        val nextBase = nextArabicBase(text, afterMad)
        if (nextBase >= text.length) return "madd_246"

        // Tanda waqf berada di U+06D6..U+06ED — bila ada di antara madd dan huruf berikutnya,
        // pembaca berhenti di sini: arid lissukun.
        val gap = text.substring(afterMad, nextBase)
        if (gap.any { it.code in 0x06D6..0x06ED }) return "madd_246"

        val hasSpace = gap.contains(' ')
        val nextCh = text[nextBase]
        return when {
            nextCh in HAMZAH && !hasSpace -> "madd_muttasil"
            nextCh in HAMZAH && hasSpace  -> "madd_munfasil"
            hasSukunOrTasydid(text, nextBase + 1) -> "madd_6"
            else -> "madd_2"
        }
    }

    /** Cari harakat vokal (fathah/damma/kasrah) pada combining mark yang mendahului posisi [pos]. */
    private fun precedingVowel(text: String, pos: Int): Char? {
        var i = pos - 1
        while (i >= 0 && isCombiningMark(text.codePointAt(i))) {
            val ch = text[i]
            if (ch == FATHAH || ch == DAMMA || ch == KASRAH) return ch
            i--
        }
        return null
    }

    /** True bila ada sukun atau tasydid di antara combining mark mulai dari [afterLetter]. */
    private fun hasSukunOrTasydid(text: String, afterLetter: Int): Boolean {
        var i = afterLetter
        while (i < text.length && isCombiningMark(text.codePointAt(i))) {
            if (text[i] == SUKUN || text[i] == TASYDID) return true
            i++
        }
        return false
    }

    // ------------------------------------------------------------------
    // Hamzat wasla: ٱ (U+0671)
    // ------------------------------------------------------------------
    private fun findHamzatWasl(text: String): List<TajweedRule> = buildList {
        var i = 0
        while (i < text.length) {
            if (text[i] == ALEF_WASLA) add(TajweedRule(i, i + 1, "hamzat_wasl"))
            i++
        }
    }

    // ------------------------------------------------------------------
    // Lam syamsiyah: lam (tanpa vokal sendiri) langsung diikuti huruf syamsiyah + tasydid.
    // Lam artikel "al-" melebur bunyi ke huruf itu, jadi lam-nya sendiri tidak dibaca.
    //
    // Span HANYA mencakup lam-nya (bukan huruf syamsiyah sesudahnya, yang tetap dibaca normal
    // dengan tasydid) -- konsisten dengan span nun/mim sakinah lain di engine ini.
    // ------------------------------------------------------------------
    private fun findLamShamsiyyah(text: String): List<TajweedRule> = buildList {
        var i = 0
        while (i < text.length) {
            if (text[i] == 'ل') {
                val hasSukun = i + 1 < text.length && text[i + 1] == SUKUN
                val noOwnVowel = i + 1 >= text.length || !isCombiningMark(text.codePointAt(i + 1))
                if (hasSukun || noOwnVowel) {
                    val afterLam = if (hasSukun) i + 2 else i + 1
                    if (afterLam < text.length &&
                        text[afterLam] in SUN_LETTERS &&
                        hasTasydidInCombining(text, afterLam + 1)
                    ) {
                        add(TajweedRule(i, afterLam, "lam_shamsiyyah"))
                        i = afterLam
                        continue
                    }
                }
            }
            i++
        }
    }

    // ------------------------------------------------------------------
    // Huruf silent: alef tambahan setelah "wau jama'ah" (mis. قَالُوا۟) -- murni konvensi
    // tulisan penanda bentuk jamak, TIDAK PERNAH dilafalkan. Pola: وا di akhir kata, alef-nya
    // tanpa harakat sendiri.
    //
    // و sebelumnya sudah ditandai madd oleh findMadd (ia tetap dibaca sebagai bunyi panjang
    // "u") -- yang silent cuma alef sesudahnya, jadi span di sini HANYA mencakup alef-nya.
    // ------------------------------------------------------------------
    // U+06DF ARABIC SMALL HIGH ROUNDED ZERO -- secara Unicode tergolong combining mark (Mn),
    // tapi maknanya justru KEBALIKAN dari harakat biasa: ini tanda resmi mushaf untuk "huruf
    // ditulis tapi tidak dibaca". Kalau diperlakukan sama seperti harakat lain, alef silent
    // yang justru ditandai tanda ini malah dianggap "berharakat" dan gagal terdeteksi.
    private const val SILENT_MARK = '۟'

    private fun findSilentLetters(text: String): List<TajweedRule> = buildList {
        var i = 0
        while (i < text.length - 1) {
            if (text[i] == 'و' && text[i + 1] == ALEF) {
                val afterAlef = i + 2
                val alefNoVowel = afterAlef >= text.length ||
                    text[afterAlef] == SILENT_MARK ||
                    !isCombiningMark(text.codePointAt(afterAlef))
                val atWordEnd = afterAlef >= text.length ||
                    text[afterAlef] == ' ' ||
                    text[afterAlef].code in 0x06D6..0x06ED
                if (alefNoVowel && atWordEnd) {
                    add(TajweedRule(i + 1, afterAlef, "silent"))
                    i = afterAlef
                    continue
                }
            }
            i++
        }
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    /** Lewati semua non-huruf (spasi, harakat, tanda baca) sampai huruf dasar Arab berikutnya. */
    private fun nextArabicBase(text: String, from: Int): Int {
        var i = from
        while (i < text.length && !isArabicBase(text[i])) i++
        return i
    }

    /** Mundur dari posisi harakat/tanwin ke huruf dasar yang menanggungnya. */
    private fun retreatToBase(text: String, harakatPos: Int): Int {
        var i = harakatPos - 1
        while (i > 0 && isCombiningMark(text.codePointAt(i))) {
            i -= Character.charCount(text.codePointBefore(i))
        }
        return maxOf(0, i)
    }

    /** Maju melewati semua combining mark (harakat, sukun, tasydid, dll). */
    private fun advancePastCombining(text: String, from: Int): Int {
        var i = from
        while (i < text.length && isCombiningMark(text.codePointAt(i))) {
            i += Character.charCount(text.codePointAt(i))
        }
        return i
    }

    private fun isArabicBase(ch: Char): Boolean =
        ch in 'ء'..'غ' || ch in 'ف'..'ي' ||
        ch in 'ٱ'..'ە'
}
