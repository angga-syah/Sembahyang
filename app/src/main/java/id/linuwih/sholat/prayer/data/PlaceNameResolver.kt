package id.linuwih.sholat.prayer.data

import android.content.Context
import android.location.Geocoder
import id.linuwih.sholat.util.NetworkActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Mengubah koordinat jadi nama tempat ("Bandung, Jawa Barat") supaya layar Jadwal & Kiblat tidak
 * cuma menampilkan angka lintang/bujur yang tidak berarti bagi kebanyakan orang.
 *
 * Beberapa keputusan yang disengaja:
 *
 * - Memakai [Geocoder] bawaan Android, bukan API pihak ketiga: tidak perlu kunci API, tidak
 *   perlu dependensi baru, dan tidak menambah pihak ketiga baru yang harus dipercaya.
 * - Hasilnya DISIMPAN di preferensi. Nama tempat nyaris tidak pernah berubah untuk koordinat
 *   yang sama, jadi tidak ada gunanya menghubungi jaringan berulang kali.
 * - Kalau gagal (tidak ada internet, atau perangkat tanpa layanan geocoder — umum di ROM tanpa
 *   Google), fungsi ini mengembalikan null dan UI tetap menampilkan koordinat. Fitur ini
 *   pelengkap, bukan syarat: jadwal sholat & kiblat tetap benar tanpanya.
 * - Pencarian dibungkus [NetworkActivity.track] supaya penanda "sedang memakai internet" di UI
 *   ikut menyala — ini satu-satunya bagian app selain unduh audio yang menyentuh jaringan.
 */
class PlaceNameResolver(private val context: Context) {

    private val prefs = PrayerPrefs(context)

    suspend fun resolve(latitude: Double, longitude: Double): String? {
        prefs.cachedPlaceNameFor(latitude, longitude)?.let { return it }
        if (!Geocoder.isPresent()) return null

        val name = withContext(Dispatchers.IO) {
            NetworkActivity.track {
                runCatching {
                    @Suppress("DEPRECATION")
                    Geocoder(context, Locale.getDefault())
                        .getFromLocation(latitude, longitude, 1)
                        ?.firstOrNull()
                        ?.let { address ->
                            // Urutan preferensi: kelurahan/kecamatan → kota → kabupaten/provinsi.
                            val locality = address.locality
                                ?: address.subAdminArea
                                ?: address.subLocality
                            val region = address.adminArea
                            listOfNotNull(locality, region).distinct().joinToString(", ")
                                .takeIf { it.isNotBlank() }
                        }
                }.getOrNull()
            }
        }

        if (name != null) prefs.savePlaceName(latitude, longitude, name)
        return name
    }
}
