package id.linuwih.sholat.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import id.linuwih.sholat.util.NetworkActivity
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class UpdateInfo(
    val versionName: String,
    val notes: String,
    val apkUrl: String,
    val apkSize: Long,
)

/**
 * Pembaruan aplikasi langsung dari GitHub Releases (aplikasi ini tidak lewat Play Store).
 *
 * Alurnya: baca rilis terbaru lewat API publik GitHub -> bandingkan nomor versinya dengan versi
 * terpasang -> unduh berkas .apk rilis itu ke cache -> serahkan ke penginstal bawaan Android.
 * Android sendiri yang menjamin keamanan langkah terakhir: pembaruan hanya bisa dipasang kalau
 * APK-nya ditandatangani kunci yang sama dengan aplikasi terpasang.
 */
object AppUpdater {
    private const val LATEST_RELEASE_URL = "https://api.github.com/repos/angga-syah/Sembahyang/releases/latest"
    const val RELEASES_PAGE = "https://github.com/angga-syah/Sembahyang/releases"
    private const val USER_AGENT = "Sembahyang-Android"

    fun installedVersion(context: Context): String =
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "0"

    /** null = sudah versi terbaru (atau rilis terbaru tidak punya berkas APK). */
    suspend fun checkLatest(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val json = NetworkActivity.track { httpGet(LATEST_RELEASE_URL) }
        val release = JSONObject(json)
        val tag = release.optString("tag_name").removePrefix("v").removePrefix("V")
        if (tag.isBlank() || compareVersions(tag, installedVersion(context)) <= 0) return@withContext null

        val assets = release.optJSONArray("assets") ?: return@withContext null
        for (i in 0 until assets.length()) {
            val asset = assets.getJSONObject(i)
            if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                return@withContext UpdateInfo(
                    versionName = tag,
                    notes = release.optString("body").trim(),
                    apkUrl = asset.getString("browser_download_url"),
                    apkSize = asset.optLong("size", -1),
                )
            }
        }
        null
    }

    /** Unduh APK ke cache. Berkas yang ukurannya tidak sesuai rilis dianggap rusak dan dibuang. */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val dest = File(dir, "Sembahyang-${info.versionName}.apk")
            NetworkActivity.track {
                val connection = (URL(info.apkUrl).openConnection() as HttpsURLConnection).apply {
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                try {
                    if (connection.responseCode != 200) error("HTTP ${connection.responseCode}")
                    val total = info.apkSize.takeIf { it > 0 } ?: connection.contentLengthLong
                    connection.inputStream.use { input ->
                        FileOutputStream(dest).use { output ->
                            val buffer = ByteArray(64 * 1024)
                            var done = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) break
                                output.write(buffer, 0, read)
                                done += read
                                if (total > 0) onProgress((done.toFloat() / total).coerceAtMost(1f))
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }
            if (info.apkSize > 0 && dest.length() != info.apkSize) {
                dest.delete()
                error("ukuran berkas tidak sesuai")
            }
            dest
        }

    /** Android 8+: user harus mengizinkan aplikasi ini memasang aplikasi (sekali saja). */
    fun canInstall(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermissionSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    /** Serahkan APK ke penginstal sistem; user tinggal menekan "Perbarui". */
    fun install(context: Context, apk: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    private fun httpGet(url: String): String {
        val connection = (URL(url).openConnection() as HttpsURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        try {
            if (connection.responseCode != 200) error("HTTP ${connection.responseCode}")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    /** Bandingkan "1.2.10" vs "1.2.9" per angka (bukan per huruf). >0 berarti a lebih baru. */
    fun compareVersions(a: String, b: String): Int {
        val pa = a.split('.', '-').map { it.toIntOrNull() ?: 0 }
        val pb = b.split('.', '-').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val diff = pa.getOrElse(i) { 0 } - pb.getOrElse(i) { 0 }
            if (diff != 0) return diff
        }
        return 0
    }
}

class UpdatePrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

    /** Periksa otomatis sekali sehari saat aplikasi dibuka. Bisa dimatikan di layar Tentang. */
    var autoCheck: Boolean
        get() = prefs.getBoolean(KEY_AUTO, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO, value).apply()

    var lastCheckDay: Long
        get() = prefs.getLong(KEY_LAST_DAY, -1)
        set(value) = prefs.edit().putLong(KEY_LAST_DAY, value).apply()

    /** Versi yang user pilih "Nanti" — tidak ditawarkan otomatis lagi (cek manual tetap bisa). */
    var skippedVersion: String?
        get() = prefs.getString(KEY_SKIPPED, null)
        set(value) = prefs.edit().putString(KEY_SKIPPED, value).apply()

    private companion object {
        const val KEY_AUTO = "auto_check"
        const val KEY_LAST_DAY = "last_check_day"
        const val KEY_SKIPPED = "skipped_version"
    }
}
