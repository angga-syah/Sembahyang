package id.linuwih.sholat.prayer.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import id.linuwih.sholat.R
import java.io.File

/** Dari mana suara notifikasi adzan diambil. */
enum class AdhanSoundSource {
    /** Nada notifikasi bawaan HP. */
    SYSTEM_DEFAULT,

    /** Rekaman adzan berlisensi CC0 yang dibundel di aplikasi. */
    BUNDLED_ADHAN,

    /** Nada dari koleksi nada HP (pemilih nada bawaan Android). */
    SYSTEM_RINGTONE,

    /** Berkas audio milik user sendiri, disalin ke penyimpanan aplikasi. */
    CUSTOM_FILE,
}

/**
 * Menentukan URI suara yang dipakai notification channel.
 *
 * Kenapa berkas custom DISALIN, bukan dipakai langsung dari URI pemilih berkas: URI hasil
 * Storage Access Framework hanya bisa dibaca selama izinnya masih berlaku, sementara suara
 * notifikasi dibunyikan oleh SISTEM (bukan proses aplikasi kita) berjam-jam kemudian, bahkan
 * setelah HP restart. Kalau berkas aslinya dipindah/dihapus user, adzan jadi bisu tanpa
 * peringatan. Menyalinnya sekali membuat suaranya pasti tetap ada.
 */
object AdhanSound {

    private const val CUSTOM_FILE_NAME = "adhan_custom"

    fun customFile(context: Context): File =
        File(context.filesDir, "adhan").apply { mkdirs() }.let { File(it, CUSTOM_FILE_NAME) }

    fun bundledUri(context: Context): Uri =
        Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_default}")

    /** URI final untuk channel notifikasi, sesuai sumber yang dipilih user. */
    fun resolve(context: Context): Uri? {
        val prefs = PrayerPrefs(context)
        return when (prefs.adhanSoundSource) {
            AdhanSoundSource.SYSTEM_DEFAULT -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            AdhanSoundSource.BUNDLED_ADHAN -> bundledUri(context)
            AdhanSoundSource.SYSTEM_RINGTONE -> prefs.notificationSoundUri?.let(Uri::parse)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            AdhanSoundSource.CUSTOM_FILE -> {
                val file = customFile(context)
                if (file.length() > 0) {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )
                } else {
                    // Berkas hilang (mis. data aplikasi dibersihkan) — jangan bisu, kembali ke
                    // adzan bawaan supaya waktu sholat tetap terdengar.
                    bundledUri(context)
                }
            }
        }
    }

    /**
     * Salin berkas pilihan user ke penyimpanan aplikasi. Return true kalau berhasil.
     * Berkas lama ditimpa — hanya satu adzan custom yang disimpan.
     */
    fun importCustomFile(context: Context, source: Uri): Boolean = runCatching {
        val dest = customFile(context)
        context.contentResolver.openInputStream(source)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: return false
        dest.length() > 0
    }.getOrDefault(false)
}
