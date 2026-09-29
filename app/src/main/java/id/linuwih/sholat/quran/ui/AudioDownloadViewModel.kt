package id.linuwih.sholat.quran.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.linuwih.sholat.R
import id.linuwih.sholat.quran.audio.AudioRepository
import id.linuwih.sholat.quran.audio.Reciter
import id.linuwih.sholat.quran.data.JuzIndex
import id.linuwih.sholat.quran.data.QuranRepository
import id.linuwih.sholat.quran.data.Surah
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DownloadScope { SURAH, JUZ, ALL }

/** Satu baris pilihan unduh (satu surah / satu juz / seluruh Qur'an). */
data class DownloadTarget(
    val id: Int,
    val title: String,
    val subtitle: String,
    val totalAyahs: Int,
    val downloadedAyahs: Int,
) {
    val isComplete: Boolean get() = totalAyahs > 0 && downloadedAyahs >= totalAyahs
    val isPartial: Boolean get() = downloadedAyahs in 1 until totalAyahs
}

data class AudioDownloadUiState(
    val reciter: Reciter = Reciter.ALAFASY,
    val scope: DownloadScope = DownloadScope.SURAH,
    val targets: List<DownloadTarget> = emptyList(),
    val loadingTargets: Boolean = true,
    val runningTargetId: Int? = null,
    val progressDone: Int = 0,
    val progressTotal: Int = 0,
    val storageUsedBytes: Long = 0,
    val message: String? = null,
)

class AudioDownloadViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRepository = AudioRepository(application)
    private val quranRepository = QuranRepository(application)

    private val _uiState = MutableStateFlow(AudioDownloadUiState())
    val uiState: StateFlow<AudioDownloadUiState> = _uiState.asStateFlow()

    private var surahs: List<Surah> = emptyList()
    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            surahs = quranRepository.getSurahs()
            refreshTargets()
        }
    }

    fun setReciter(reciter: Reciter) {
        _uiState.value = _uiState.value.copy(reciter = reciter, message = null)
        viewModelScope.launch { refreshTargets() }
    }

    fun setScope(scope: DownloadScope) {
        _uiState.value = _uiState.value.copy(scope = scope, message = null)
        viewModelScope.launch { refreshTargets() }
    }

    /** Daftar ayat yang tercakup oleh satu baris pilihan, sesuai mode yang sedang aktif. */
    private fun ayahsFor(scope: DownloadScope, targetId: Int): List<Pair<Int, Int>> = when (scope) {
        DownloadScope.SURAH -> surahs.find { it.number == targetId }?.let { JuzIndex.ayahsInSurah(it) }.orEmpty()
        DownloadScope.JUZ -> JuzIndex.ayahsIn(targetId, surahs)
        DownloadScope.ALL -> JuzIndex.allAyahs(surahs)
    }

    /**
     * Menghitung status "sudah terunduh" berarti men-stat ribuan file (mode ALL = 6.236),
     * jadi selalu di IO dispatcher supaya tidak menahan frame UI.
     */
    private suspend fun refreshTargets() {
        if (surahs.isEmpty()) return
        val state = _uiState.value
        _uiState.value = state.copy(loadingTargets = true)

        val context = getApplication<Application>()
        val computed = withContext(Dispatchers.IO) {
            val reciter = state.reciter
            when (state.scope) {
                DownloadScope.SURAH -> surahs.map { surah ->
                    val ayahs = JuzIndex.ayahsInSurah(surah)
                    DownloadTarget(
                        id = surah.number,
                        title = "${surah.number}. ${surah.transliteration}",
                        subtitle = context.getString(R.string.audio_target_ayah_count, surah.totalVerses),
                        totalAyahs = ayahs.size,
                        downloadedAyahs = audioRepository.downloadedCount(reciter, ayahs),
                    )
                }
                DownloadScope.JUZ -> (1..JuzIndex.TOTAL_JUZ).map { juz ->
                    val ayahs = JuzIndex.ayahsIn(juz, surahs)
                    val startSurah = surahs.find { it.number == JuzIndex.startSurahOf(juz) }
                    DownloadTarget(
                        id = juz,
                        title = context.getString(R.string.audio_target_juz, juz),
                        subtitle = context.getString(
                            R.string.audio_target_juz_subtitle,
                            startSurah?.transliteration.orEmpty(),
                            ayahs.size,
                        ),
                        totalAyahs = ayahs.size,
                        downloadedAyahs = audioRepository.downloadedCount(reciter, ayahs),
                    )
                }
                DownloadScope.ALL -> {
                    val ayahs = JuzIndex.allAyahs(surahs)
                    listOf(
                        DownloadTarget(
                            id = 0,
                            title = context.getString(R.string.audio_target_all),
                            subtitle = context.getString(R.string.audio_target_all_subtitle, ayahs.size),
                            totalAyahs = ayahs.size,
                            downloadedAyahs = audioRepository.downloadedCount(reciter, ayahs),
                        ),
                    )
                }
            }
        }
        val storage = withContext(Dispatchers.IO) { audioRepository.storageUsedBytes() }

        _uiState.value = _uiState.value.copy(
            targets = computed,
            loadingTargets = false,
            storageUsedBytes = storage,
        )
    }

    fun startDownload(targetId: Int) {
        if (downloadJob?.isActive == true) return
        val state = _uiState.value
        val ayahs = ayahsFor(state.scope, targetId)
        if (ayahs.isEmpty()) return

        _uiState.value = state.copy(
            runningTargetId = targetId,
            progressDone = 0,
            progressTotal = ayahs.size,
            message = null,
        )

        downloadJob = viewModelScope.launch {
            val context = getApplication<Application>()
            try {
                val result = audioRepository.downloadAyahs(state.reciter, ayahs) { done, total ->
                    _uiState.value = _uiState.value.copy(progressDone = done, progressTotal = total)
                }
                _uiState.value = _uiState.value.copy(
                    runningTargetId = null,
                    message = if (result.failed == 0) {
                        context.getString(R.string.audio_download_done, result.downloaded)
                    } else {
                        context.getString(R.string.audio_download_partial, result.downloaded, result.failed)
                    },
                )
            } catch (e: CancellationException) {
                _uiState.value = _uiState.value.copy(
                    runningTargetId = null,
                    message = context.getString(R.string.audio_download_cancelled),
                )
                throw e
            } finally {
                refreshTargets()
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    fun deleteTarget(targetId: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            withContext(Dispatchers.IO) {
                when (state.scope) {
                    DownloadScope.SURAH -> audioRepository.deleteSurah(state.reciter, targetId)
                    // Juz bisa memotong surah di tengah, jadi hapus per-file (bukan per-folder
                    // surah) supaya ayat milik juz tetangga di surah yang sama tidak ikut hilang.
                    DownloadScope.JUZ -> JuzIndex.ayahsIn(targetId, surahs).forEach { (s, a) ->
                        audioRepository.localFile(state.reciter, s, a).delete()
                    }
                    DownloadScope.ALL -> audioRepository.deleteAllAudio()
                }
            }
            refreshTargets()
        }
    }

    override fun onCleared() {
        downloadJob?.cancel()
        super.onCleared()
    }
}
