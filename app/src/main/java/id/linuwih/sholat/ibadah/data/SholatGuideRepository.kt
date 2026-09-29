package id.linuwih.sholat.ibadah.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Satu varian bacaan; [label] kosong berarti langkah ini hanya punya satu bacaan. */
data class SholatReading(
    val label: String,
    val arabic: String,
    val latin: String,
    val translation: String,
)

/**
 * Satu langkah dalam sholat. [readings] boleh kosong untuk langkah yang bacaannya ada di tempat
 * lain (mis. Al-Fatihah, yang teksnya sudah tersedia di tab Qur'an) — langkah itu tetap
 * ditampilkan supaya urutan sholatnya utuh.
 */
data class SholatStep(
    val title: String,
    val note: String,
    val readings: List<SholatReading>,
)

/**
 * Panduan tata cara sholat, dibundel offline seperti Dzikir.
 *
 * Catatan penting soal isi: beberapa langkah memang punya LEBIH DARI SATU bacaan yang sama-sama
 * sahih (mis. dua riwayat doa iftitah), dan sebagian praktik berbeda antar mazhab — qunut Subuh
 * lazim di kalangan Syafi'i tapi tidak dibiasakan mazhab lain. Karena itu varian ditampilkan
 * berdampingan, bukan dipilihkan satu lalu yang lain disembunyikan: perbedaan ini soal ragam
 * praktik yang punya dasar masing-masing, bukan soal benar atau salah.
 */
class SholatGuideRepository(private val context: Context) {

    private var cache: List<SholatStep>? = null

    suspend fun getSteps(): List<SholatStep> {
        cache?.let { return it }
        return withContext(Dispatchers.IO) {
            val root = JSONObject(
                context.assets.open("ibadah/sholat_guide.json").bufferedReader().use { it.readText() },
            )
            val stepsArray = root.getJSONArray("steps")
            val steps = (0 until stepsArray.length()).map { i ->
                val stepObj = stepsArray.getJSONObject(i)
                val readingsArray = stepObj.getJSONArray("readings")
                SholatStep(
                    title = stepObj.getString("title"),
                    note = stepObj.optString("note"),
                    readings = (0 until readingsArray.length()).map { j ->
                        val r = readingsArray.getJSONObject(j)
                        SholatReading(
                            label = r.optString("label"),
                            arabic = r.getString("arabic"),
                            latin = r.getString("latin"),
                            translation = r.getString("translation"),
                        )
                    },
                )
            }
            cache = steps
            steps
        }
    }
}
