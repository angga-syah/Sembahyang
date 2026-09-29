package id.linuwih.sholat.quran.audio

import android.content.Context
import id.linuwih.sholat.util.NetworkActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Hasil unduhan massal — sengaja melaporkan gagal sebagian, bukan sekadar true/false. */
data class BulkDownloadResult(val total: Int, val downloaded: Int, val failed: Int)

/**
 * Download audio per-surah on-demand, disimpan permanen offline (lihat plan: bukan
 * bundle di APK). Satu-satunya bagian app yang bicara ke internet.
 *
 * Ketahanan terhadap pihak ketiga: 3 sumber independen dicoba berurutan per file
 * (CDN Quran.com utama, mirror hostname-nya, lalu everyayah.com) — kalau satu sumber
 * mati, otomatis lanjut ke sumber berikutnya, bukan langsung gagal total.
 */
class AudioRepository(private val context: Context) {

    fun localFile(reciter: Reciter, surah: Int, ayah: Int): File =
        File(audioDir(reciter, surah), "%03d.mp3".format(ayah))

    fun isSurahDownloaded(reciter: Reciter, surah: Int, totalAyahs: Int): Boolean =
        (1..totalAyahs).all { localFile(reciter, surah, it).length() > 0 }

    /** Return true kalau semua ayat berhasil diunduh (atau sudah ada sebelumnya). */
    suspend fun downloadSurah(
        reciter: Reciter,
        surah: Int,
        totalAyahs: Int,
        onProgress: (downloaded: Int, total: Int) -> Unit,
    ): Boolean {
        val result = downloadAyahs(reciter, (1..totalAyahs).map { surah to it }, onProgress)
        return result.failed == 0
    }

    /**
     * Unduh sekumpulan ayat (satu surah, satu juz, atau seluruh Al-Qur'an).
     *
     * Beda penting dari [downloadSurah]: satu file gagal TIDAK membatalkan seluruh job. Untuk
     * unduhan massal (juz = ratusan file, semua = 6.236 file), satu gangguan sinyal di tengah
     * jalan jangan sampai membuang semua progres — file yang berhasil tetap tersimpan, dan
     * karena file yang sudah ada dilewati, mengulang job berikutnya otomatis melanjutkan
     * sisanya saja.
     */
    suspend fun downloadAyahs(
        reciter: Reciter,
        ayahs: List<Pair<Int, Int>>,
        onProgress: (done: Int, total: Int) -> Unit,
    ): BulkDownloadResult = withContext(Dispatchers.IO) {
        var downloaded = 0
        var failed = 0
        ayahs.forEachIndexed { index, (surah, ayah) ->
            // Hormati pembatalan user — tanpa ini job 6.236 file tidak bisa dihentikan.
            currentCoroutineContext().ensureActive()

            val dest = localFile(reciter, surah, ayah)
            when {
                dest.length() > 0 -> downloaded++
                else -> {
                    dest.parentFile?.mkdirs()
                    if (sourceUrls(reciter, surah, ayah).any { downloadFile(it, dest) }) downloaded++ else failed++
                }
            }
            onProgress(index + 1, ayahs.size)
        }
        BulkDownloadResult(total = ayahs.size, downloaded = downloaded, failed = failed)
    }

    /** Berapa dari daftar ayat ini yang sudah ada di penyimpanan lokal. */
    fun downloadedCount(reciter: Reciter, ayahs: List<Pair<Int, Int>>): Int =
        ayahs.count { (surah, ayah) -> localFile(reciter, surah, ayah).length() > 0 }

    fun storageUsedBytes(): Long {
        val root = File(context.filesDir, "audio")
        if (!root.exists()) return 0
        return root.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
    }

    fun deleteSurah(reciter: Reciter, surah: Int) {
        audioDir(reciter, surah).deleteRecursively()
    }

    fun deleteAllAudio() {
        File(context.filesDir, "audio").deleteRecursively()
    }

    // Sengaja TIDAK mkdirs() di sini: fungsi ini juga dipakai jalur baca/hitung status
    // (downloadedCount, isSurahDownloaded) yang tidak boleh punya efek samping bikin folder
    // kosong untuk 114 surah hanya karena layar pengelola audio dibuka.
    private fun audioDir(reciter: Reciter, surah: Int): File =
        File(context.filesDir, "audio/${reciter.name}/$surah")

    private fun sourceUrls(reciter: Reciter, surah: Int, ayah: Int): List<String> {
        val code = "%03d%03d".format(surah, ayah)
        return listOf(
            "https://verses.quran.com/${reciter.cdnPath}/$code.mp3",
            "https://audio.qurancdn.com/${reciter.cdnPath}/$code.mp3",
            "https://everyayah.com/data/${reciter.everyayahFolder}/$code.mp3",
        )
    }

    private fun downloadFile(urlStr: String, dest: File): Boolean = NetworkActivity.track {
        downloadFileInternal(urlStr, dest)
    }

    private fun downloadFileInternal(urlStr: String, dest: File): Boolean {
        var connection: HttpsURLConnection? = null
        val tmp = File(dest.parentFile, dest.name + ".part")
        return try {
            connection = (URL(urlStr).openConnection() as HttpsURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 20_000
                connect()
            }
            if (connection.responseCode != 200) return false

            connection.inputStream.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            }

            // Sanity check: file audio valid tidak akan sekecil ini (mis. halaman error/redirect
            // yang tersimpan sebagai .mp3) — lihat plan bagian validasi download.
            if (tmp.length() < 500) {
                tmp.delete()
                return false
            }
            tmp.renameTo(dest)
        } catch (e: IOException) {
            tmp.delete()
            false
        } finally {
            connection?.disconnect()
        }
    }
}
