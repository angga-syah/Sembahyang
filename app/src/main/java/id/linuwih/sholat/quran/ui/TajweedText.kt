package id.linuwih.sholat.quran.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import id.linuwih.sholat.quran.data.TajweedRule

/** Mn/Mc = harakat, sukun, tasydid, tanda mad — selalu menempel pada huruf SEBELUMNYA. */
internal fun isCombiningMark(codePoint: Int): Boolean {
    val type = Character.getType(codePoint)
    return type == Character.NON_SPACING_MARK.toInt() || type == Character.COMBINING_SPACING_MARK.toInt()
}

/**
 * PENTING: [rules] selalu diisi penuh, juga saat [tajweedEnabled] false. Yang dimatikan adalah
 * WARNANYA, bukan span-nya.
 *
 * Alasannya, dan ini yang dulu saya salah nilai berulang kali: benar bahwa warna bukan atribut
 * shaping — ForegroundColorSpan memang bukan MetricAffectingSpan, jadi lebar teks tidak berubah.
 * Tapi bukan itu satu-satunya jalur yang menentukan posisi glyph di layar. Saat MENGGAMBAR,
 * android.text.TextLine memecah satu baris menjadi beberapa potongan di setiap batas span
 * CharacterStyle — bukan hanya di batas MetricAffectingSpan — lalu menggambar tiap potongan
 * lewat panggilan drawTextRun sendiri-sendiri, dengan offset x yang diakumulasi per potongan.
 *
 * Akibatnya jumlah span ikut menentukan hasil akhir di layar:
 * - tajwid MATI dulu = 0 span  -> satu potongan utuh
 * - tajwid HIDUP     = puluhan span -> puluhan potongan, akumulasi offset yang berbeda
 *
 * Selisihnya kecil (pembulatan sub-pixel), tapi pada teks Arab ia terlihat jelas: harakat yang
 * jatuh di potongan berbeda dari huruf dasarnya tergambar bergeser. Itulah "harakat geser saat
 * tajwid dinyalakan" yang dilaporkan, dan itu nyata — bukan salah lihat.
 *
 * Dengan span yang jumlah dan batasnya identik di kedua mode, pemecahan potongannya identik,
 * akumulasi offsetnya identik, dan posisi tiap glyph identik. Yang berubah cuma warna.
 *
 * Karena itu pula warna span tidak boleh Color.Unspecified: Compose tidak membuat span sama
 * sekali untuk warna Unspecified, dan aturan tajwid yang namanya tidak dikenal akan diam-diam
 * mengurangi jumlah span sehingga memecah kesamaan ini.
 */
internal fun buildTajweedAnnotatedString(
    text: String,
    rules: List<TajweedRule>,
    tajweedEnabled: Boolean,
    plainColor: Color,
): AnnotatedString = buildAnnotatedString {
    append(text)

    // Batas dari dataset sering berhenti tepat SEBELUM harakat: diukur di seluruh berkas,
    // 50,3% dari 32.812 aturan berakhir begitu. Akibatnya huruf dasarnya berwarna sementara
    // harakatnya sendiri tetap putih — terlihat seperti tanda bacanya rusak/lepas.
    //
    // Perbaikan ini pernah ada lalu saya hapus sendiri karena mengira warna tidak berpengaruh
    // apa pun pada tampilan. Itu keliru: warna memang tidak menggeser huruf, tapi batas warna
    // yang memotong satu kesatuan huruf+harakat tetap merusak tampilannya.
    //
    // Pelebaran dibatasi agar tidak mencaplok wilayah aturan tetangga, yang akan membuat warna
    // jatuh di huruf yang salah.
    val sorted = rules.sortedBy { it.start }
    sorted.forEachIndexed { index, rule ->
        var start = rule.start.coerceIn(0, text.length)
        var end = rule.end.coerceIn(start, text.length)
        val prevEnd = if (index > 0) sorted[index - 1].end.coerceIn(0, text.length) else 0
        val nextStart = if (index < sorted.lastIndex) {
            sorted[index + 1].start.coerceIn(0, text.length)
        } else {
            text.length
        }

        // Mundur ke huruf dasar bila batas awal jatuh DI ATAS harakat. Yang diperiksa harus
        // karakter di posisi start itu sendiri, bukan karakter sebelumnya: kalau yang diperiksa
        // karakter sebelumnya, batas yang sudah tepat di huruf dasar justru ditarik mundur
        // melewati harakat milik huruf SEBELUMNYA — mewarnai harakat huruf yang salah.
        while (start > prevEnd && isCombiningMark(text.codePointAt(start))) {
            start -= Character.charCount(text.codePointBefore(start))
        }
        // Maju melewati seluruh harakat yang menempel pada huruf terakhir.
        while (end < nextStart && isCombiningMark(text.codePointAt(end))) {
            end += Character.charCount(text.codePointAt(end))
        }

        if (start < end) {
            val ruleColor = tajweedColorFor(rule.rule)
            val color = if (tajweedEnabled && ruleColor != Color.Unspecified) ruleColor else plainColor
            addStyle(SpanStyle(color = color), start, end)
        }
    }
}
