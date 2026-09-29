package id.linuwih.sholat.quran.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Tafsir Ringkas Kemenag per ayat — lihat assets/quran/ATTRIBUTION.md untuk sumber & lisensi.
 *
 * Repository TERPISAH dari QuranRepository, sengaja: tafsir tidak terikat pada gaya mushaf
 * (Kemenag/Utsmani) seperti teks Arab & tajwid — satu berkas berlaku untuk kedua gaya, jadi
 * tidak perlu ikut logic invalidateAyahs()/loadedStyle di sana. Dimuat MALAS (baru saat ayat
 * pertama diminta), bukan digabung ke parseAyahs() yang sudah eager — kebanyakan user tidak
 * membuka tafsir di setiap sesi baca, jadi 3+ MB ini sebaiknya tidak selalu ikut termuat.
 */
class TafsirRepository(private val context: Context) {
    private val mutex = Mutex()
    private var cache: Map<Int, Map<Int, String>>? = null

    suspend fun getTafsir(surahNumber: Int, ayahNumber: Int): String? {
        val loaded = cache ?: mutex.withLock {
            cache ?: withContext(Dispatchers.IO) { parse() }.also { cache = it }
        }
        return loaded[surahNumber]?.get(ayahNumber)
    }

    private fun parse(): Map<Int, Map<Int, String>> {
        val json = JSONObject(
            context.assets.open("quran/tafsir_ringkas_kemenag.json").bufferedReader().use { it.readText() },
        )
        val result = mutableMapOf<Int, Map<Int, String>>()
        json.keys().forEach { key ->
            val surahNumber = key.toIntOrNull() ?: return@forEach
            val array: JSONArray = json.getJSONArray(key)
            val byVerse = mutableMapOf<Int, String>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                byVerse[o.getInt("verse")] = o.getString("text")
            }
            result[surahNumber] = byVerse
        }
        return result
    }
}
