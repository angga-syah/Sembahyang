package id.linuwih.sholat.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Pelacak "apakah aplikasi ini sedang memakai internet SEKARANG".
 *
 * Aplikasi ini mengklaim hampir seluruh fiturnya berjalan offline, jadi klaim itu sebaiknya bisa
 * dibuktikan user secara langsung — bukan sekadar tulisan di layar Tentang. Setiap jalur yang
 * benar-benar menyentuh jaringan membungkus dirinya lewat [track], dan UI menampilkan penanda
 * kecil selama masih ada permintaan berjalan.
 *
 * Memakai penghitung, bukan boolean: beberapa permintaan bisa berjalan bersamaan (mis. unduhan
 * audio berbarengan dengan pencarian nama lokasi). Boolean akan padam begitu salah satu selesai
 * padahal yang lain masih jalan.
 */
object NetworkActivity {
    private val lock = Any()
    private var count = 0

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    fun begin() = synchronized(lock) {
        count++
        _isActive.value = count > 0
    }

    fun end() = synchronized(lock) {
        if (count > 0) count--
        _isActive.value = count > 0
    }

    /** Bungkus satu permintaan jaringan; penghitung selalu turun lagi walau blok melempar. */
    inline fun <T> track(block: () -> T): T {
        begin()
        return try {
            block()
        } finally {
            end()
        }
    }
}
