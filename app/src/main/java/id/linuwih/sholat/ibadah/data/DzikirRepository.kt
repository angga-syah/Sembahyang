package id.linuwih.sholat.ibadah.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Dzikir dibundel offline (JSON asset), sama seperti Qur'an — lihat plan: tidak ada fitur
 * yang butuh internet selain unduhan audio murottal. Sumber: trianbasofi/almatsurat-json
 * (MIT), lihat assets/dzikir/ATTRIBUTION.md.
 */
class DzikirRepository(private val context: Context) {
    private val cache = mutableMapOf<DzikirSet, List<DzikirItem>>()

    suspend fun getItems(set: DzikirSet): List<DzikirItem> {
        cache[set]?.let { return it }
        return withContext(Dispatchers.IO) {
            val json = context.assets.open(set.assetFile).bufferedReader().use { it.readText() }
            val root = JSONObject(json)
            val items = mutableListOf<DzikirItem>()
            flatten(root.getJSONArray("data"), items)
            cache[set] = items
            items
        }
    }

    private fun flatten(array: JSONArray, out: MutableList<DzikirItem>) {
        for (i in 0 until array.length()) {
            val o = array.getJSONObject(i)
            val type = o.optString("type", "single")
            val title = o.optString("title", "")
            if (type == "parent") {
                out += DzikirItem(title = title, arabic = "", latin = "", translation = "", repeat = 1, isHeader = true)
                o.optJSONArray("data")?.let { flatten(it, out) }
            } else {
                out += DzikirItem(
                    title = title,
                    arabic = o.optString("arabic", ""),
                    latin = o.optString("latin", ""),
                    translation = o.optString("translation", ""),
                    repeat = o.optInt("repeat", 1),
                    isHeader = false,
                )
            }
        }
    }
}
