package id.linuwih.sholat.quran.audio

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PlaybackSnapshot(
    val reciter: Reciter = Reciter.ALAFASY,
    val surah: Int? = null,
    val ayah: Int? = null,
    val isPlaying: Boolean = false,
    /** true selama sesi masih hidup walau sedang dijeda — dipakai UI untuk menampilkan kontrol. */
    val isActive: Boolean = false,
    /** Terisi hanya saat sesi hafalan (ulang ayat) berjalan; null untuk murottal biasa. */
    val hafalan: HafalanSession? = null,
)

data class HafalanSession(val plan: HafalanPlan, val position: HafalanPosition)

/**
 * Jembatan status antara AudioPlaybackService dan UI.
 *
 * Kenapa objek global, bukan state di dalam ViewModel: pemutar sekarang hidup di foreground
 * service supaya murottal tetap jalan walau layar surah ditutup. Service dan ViewModel punya
 * daur hidup yang berbeda — ViewModel bisa mati saat Activity dihancurkan sementara service
 * jalan terus — jadi status pemutaran tidak boleh "dimiliki" ViewModel. ViewModel cukup
 * mengamati aliran ini dan mengirim perintah lewat Intent ke service.
 */
object PlaybackState {
    private val _snapshot = MutableStateFlow(PlaybackSnapshot())
    val snapshot: StateFlow<PlaybackSnapshot> = _snapshot.asStateFlow()

    internal fun update(transform: (PlaybackSnapshot) -> PlaybackSnapshot) {
        _snapshot.value = transform(_snapshot.value)
    }

    internal fun reset() {
        _snapshot.value = PlaybackSnapshot(reciter = _snapshot.value.reciter)
    }
}
