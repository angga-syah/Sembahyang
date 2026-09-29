package id.linuwih.sholat.quran.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Teks Arab + terjemahan ID/EN dibundel sebagai JSON asset (bukan diambil dari API — lihat
 * plan: baca Qur'an harus 100% offline sejak install). Sumber data: risan/quran-json
 * (CC BY-SA 4.0) — lihat assets/quran/ATTRIBUTION.md.
 *
 * Diparsing sekali lalu di-cache di memori (~6.236 ayat, beberapa ratus KB per bahasa —
 * ringan untuk hp jadul, tidak perlu Room/SQLite untuk data read-only ini).
 */
/** Satu ayat yang cocok dengan kata kunci pencarian, plus cuplikan teks di sekitar kecocokan. */
data class AyahSearchResult(
    val surahNumber: Int,
    val surahName: String,
    val ayahNumber: Int,
    val snippet: String,
)

class QuranRepository(private val context: Context) {
    private val mutex = Mutex()
    private val prefs = QuranPrefs(context)
    private var surahsCache: List<Surah>? = null
    private var ayahsCache: Map<Int, List<Ayah>>? = null

    // Gaya mushaf yang sedang ada di cache. Hanya SATU gaya dimuat pada satu waktu —
    // memuat keduanya sekaligus akan menggandakan pemakaian memori teks Arab + aturan tajwid
    // tanpa guna, karena user cuma membaca satu gaya.
    private var loadedStyle: MushafStyle? = null

    /**
     * Hanya memuat metadata surah (surahs_id.json, ±19 KB) — TIDAK ikut memuat teks ayat.
     * Sebelumnya ini memanggil ensureLoaded() yang mem-parse seluruh berkas ayat (>6 MB) hanya
     * untuk menampilkan daftar surah, dan itu juga membuat service pemutar audio ikut mahal
     * padahal cuma butuh jumlah ayat per surah.
     */
    suspend fun getSurahs(): List<Surah> {
        surahsCache?.let { return it }
        return mutex.withLock {
            surahsCache ?: withContext(Dispatchers.IO) { parseSurahs() }.also { surahsCache = it }
        }
    }

    suspend fun getAyahs(surahNumber: Int): List<Ayah> {
        ensureLoaded()
        // Dibaca ke variabel lokal, bukan ayahsCache!!: invalidateAyahs() bisa mengosongkan cache
        // dari coroutine lain tepat setelah ensureLoaded() selesai (mis. user mengganti gaya
        // mushaf saat surah sedang dimuat), dan !! akan berujung crash.
        return ayahsCache?.get(surahNumber).orEmpty()
    }

    /** Dipanggil setelah user mengganti gaya mushaf — cache ayat dibuang supaya dimuat ulang. */
    suspend fun invalidateAyahs() {
        mutex.withLock {
            ayahsCache = null
            loadedStyle = null
        }
    }

    /**
     * Cari kata di terjemahan (ID & EN) dan transliterasi Latin — bukan di teks Arab, karena
     * mengetik Arab berharakat di keyboard HP praktis tidak mungkin bagi kebanyakan pengguna.
     *
     * Dijalankan di Dispatchers.Default (bukan Main): 6.236 ayat x 4 field = ~25 ribu
     * pemindaian string, cukup berat untuk menahan frame UI kalau dilakukan di main thread.
     * [limit] membatasi hasil supaya kata umum ("Allah") tidak menghasilkan ribuan baris.
     */
    suspend fun searchAyahs(query: String, limit: Int = 300): List<AyahSearchResult> {
        val keyword = query.trim()
        if (keyword.length < MIN_SEARCH_LENGTH) return emptyList()
        ensureLoaded()

        val surahNames = surahsCache?.associate { it.number to it.transliteration }.orEmpty()
        val byNumber = ayahsCache ?: return emptyList()

        return withContext(Dispatchers.Default) {
            val results = mutableListOf<AyahSearchResult>()
            outer@ for (surahNumber in byNumber.keys.sorted()) {
                for (ayah in byNumber[surahNumber].orEmpty()) {
                    val match = sequenceOf(ayah.translationId, ayah.translationEn, ayah.latinTextId, ayah.latinTextEn)
                        .firstOrNull { it.contains(keyword, ignoreCase = true) }
                        ?: continue
                    results += AyahSearchResult(
                        surahNumber = surahNumber,
                        surahName = surahNames[surahNumber].orEmpty(),
                        ayahNumber = ayah.number,
                        snippet = snippetAround(match, keyword),
                    )
                    if (results.size >= limit) break@outer
                }
            }
            results
        }
    }

    /** Potong teks di sekitar kecocokan supaya baris hasil tidak sepanjang satu ayat penuh. */
    private fun snippetAround(text: String, keyword: String, radius: Int = 60): String {
        val index = text.indexOf(keyword, ignoreCase = true)
        if (index < 0) return text.take(radius * 2)
        val start = (index - radius).coerceAtLeast(0)
        val end = (index + keyword.length + radius).coerceAtMost(text.length)
        return buildString {
            if (start > 0) append("…")
            append(text, start, end)
            if (end < text.length) append("…")
        }
    }

    private suspend fun ensureLoaded() {
        val style = prefs.mushafStyle
        if (ayahsCache != null && loadedStyle == style) return
        val surahs = getSurahs()
        mutex.withLock {
            if (ayahsCache != null && loadedStyle == style) return@withLock
            withContext(Dispatchers.IO) {
                ayahsCache = parseAyahs(surahs, style)
                loadedStyle = style
            }
        }
    }

    private fun parseSurahs(): List<Surah> {
        val array = JSONArray(readAsset("quran/surahs_id.json"))
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Surah(
                number = o.getInt("id"),
                arabicName = o.getString("name"),
                transliteration = o.getString("transliteration"),
                translationName = o.getString("translation"),
                type = o.getString("type"),
                totalVerses = o.getInt("total_verses"),
            )
        }.sortedBy { it.number }
    }

    private fun parseAyahs(surahs: List<Surah>, style: MushafStyle): Map<Int, List<Ayah>> {
        val arJson = JSONObject(readAsset("quran/quran_ar.json"))
        val latinEnJson = JSONObject(readAsset("quran/quran_latin.json"))
        val latinIdJson = JSONObject(readAsset("quran/quran_latin_id.json"))
        val idJson = JSONObject(readAsset("quran/quran_id.json"))
        val enJson = JSONObject(readAsset("quran/quran_en.json"))

        // Teks Arab & aturan tajwid SELALU diambil berpasangan dari sumber yang sama, karena
        // index aturan hanya sahih terhadap teks asalnya. Menukar salah satunya saja akan
        // membuat warna tajwid jatuh di huruf yang keliru.
        val (arAsset, rulesAsset) = when (style) {
            MushafStyle.UTSMANI -> "quran/quran_ar_tajweed.json" to "quran/quran_tajweed_rules.json"
            MushafStyle.KEMENAG -> "quran/quran_ar_kemenag.json" to "quran/quran_tajweed_rules_kemenag.json"
        }
        val tajweedArJson = JSONObject(readAsset(arAsset))
        val tajweedRulesJson = JSONObject(readAsset(rulesAsset))

        val result = mutableMapOf<Int, List<Ayah>>()
        for (surah in surahs) {
            val key = surah.number.toString()
            val arArr = arJson.optJSONArray(key) ?: JSONArray()
            val latinEnByVerse = textByVerse(latinEnJson.optJSONArray(key))
            val latinIdByVerse = textByVerse(latinIdJson.optJSONArray(key))
            val idByVerse = textByVerse(idJson.optJSONArray(key))
            val enByVerse = textByVerse(enJson.optJSONArray(key))
            val tajweedArByVerse = tajweedArJson.optJSONObject(key)
            val tajweedRulesByVerse = tajweedRulesJson.optJSONObject(key)

            result[surah.number] = (0 until arArr.length()).map { i ->
                val o = arArr.getJSONObject(i)
                val verse = o.getInt("verse")
                val verseKey = verse.toString()
                Ayah(
                    number = verse,
                    latinTextId = latinIdByVerse[verse].orEmpty(),
                    latinTextEn = latinEnByVerse[verse].orEmpty(),
                    translationId = idByVerse[verse].orEmpty(),
                    translationEn = enByVerse[verse].orEmpty(),
                    arabicTextTajweed = tajweedArByVerse?.optString(verseKey).orEmpty(),
                    tajweedRules = parseTajweedRules(tajweedRulesByVerse?.optJSONArray(verseKey)),
                )
            }
        }
        return result
    }

    private fun textByVerse(array: JSONArray?): Map<Int, String> {
        if (array == null) return emptyMap()
        return (0 until array.length()).associate { i ->
            val o = array.getJSONObject(i)
            o.getInt("verse") to o.getString("text")
        }
    }

    private fun parseTajweedRules(array: JSONArray?): List<TajweedRule> {
        if (array == null) return emptyList()
        return (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            TajweedRule(start = o.getInt("s"), end = o.getInt("e"), rule = o.getString("r"))
        }
    }

    private fun readAsset(path: String): String =
        context.assets.open(path).bufferedReader().use { it.readText() }

    companion object {
        /** Di bawah 3 huruf, hasilnya terlalu banyak untuk berguna. */
        const val MIN_SEARCH_LENGTH = 3
    }
}
