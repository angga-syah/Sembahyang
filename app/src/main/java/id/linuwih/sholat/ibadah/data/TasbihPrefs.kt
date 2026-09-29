package id.linuwih.sholat.ibadah.data

import android.content.Context

/** label = null berarti preset "Bebas" (hitung tanpa target) — labelnya diambil dari
 *  string resource di UI supaya ikut bahasa sistem, bukan hardcoded di sini. */
data class TasbihPreset(val label: String?, val target: Int)

val TASBIH_PRESETS = listOf(
    TasbihPreset("Subhanallah", 33),
    TasbihPreset("Alhamdulillah", 33),
    TasbihPreset("Allahu Akbar", 34),
    TasbihPreset("Astaghfirullah", 100),
    TasbihPreset("La ilaha illallah", 100),
    TasbihPreset(null, 0),
)

/** Hitungan tasbih disimpan supaya tidak hilang kalau app ditutup di tengah dzikir. */
class TasbihPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("tasbih_prefs", Context.MODE_PRIVATE)

    var count: Int
        get() = prefs.getInt(KEY_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_COUNT, value).apply()

    var presetIndex: Int
        get() = prefs.getInt(KEY_PRESET, 0)
        set(value) = prefs.edit().putInt(KEY_PRESET, value).apply()

    private companion object {
        const val KEY_COUNT = "count"
        const val KEY_PRESET = "preset_index"
    }
}
