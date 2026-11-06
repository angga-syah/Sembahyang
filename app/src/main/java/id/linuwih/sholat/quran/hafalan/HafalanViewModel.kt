package id.linuwih.sholat.quran.hafalan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.linuwih.sholat.R
import id.linuwih.sholat.quran.audio.AudioPlaybackService
import id.linuwih.sholat.quran.audio.AudioRepository
import id.linuwih.sholat.quran.audio.HafalanPlan
import id.linuwih.sholat.quran.audio.PlaybackSnapshot
import id.linuwih.sholat.quran.audio.PlaybackState
import id.linuwih.sholat.quran.audio.Reciter
import id.linuwih.sholat.quran.data.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SurahHafalanStatus { BELUM, SEDANG, HAFAL }

data class SurahProgress(val surah: Surah, val memorized: Int) {
    val status: SurahHafalanStatus
        get() = when {
            memorized <= 0 -> SurahHafalanStatus.BELUM
            memorized >= surah.totalVerses -> SurahHafalanStatus.HAFAL
            else -> SurahHafalanStatus.SEDANG
        }
}

data class HafalanSettings(
    val ayahRepeat: Int,
    val rangeRepeat: Int,
    val gap: RepeatGap,
    val speed: Float,
    val reciter: Reciter,
    val showTranslation: Boolean,
    val showLatin: Boolean,
)

data class HafalanAudioState(
    val isDownloaded: Boolean = false,
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val error: String? = null,
)

class HafalanViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = HafalanDatabase.getInstance(application).dao()
    private val prefs = HafalanPrefs(application)
    private val audioRepository = AudioRepository(application)

    private val _scope = MutableStateFlow(prefs.scope)
    val scope: StateFlow<HafalanScope> = _scope.asStateFlow()

    private val _juzAmmaFromEnd = MutableStateFlow(prefs.juzAmmaFromEnd)
    val juzAmmaFromEnd: StateFlow<Boolean> = _juzAmmaFromEnd.asStateFlow()

    /** Jumlah ayat hafal per surah (surah tanpa ayat hafal tidak ada di map). */
    val memorizedCounts: StateFlow<Map<Int, Int>> = dao.observeCounts()
        .map { list -> list.associate { it.surahNumber to it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<HafalanSettings> = _settings.asStateFlow()

    private val _audio = MutableStateFlow(HafalanAudioState())
    val audio: StateFlow<HafalanAudioState> = _audio.asStateFlow()

    val playback: StateFlow<PlaybackSnapshot> = PlaybackState.snapshot

    fun setScope(scope: HafalanScope) {
        prefs.scope = scope
        _scope.value = scope
    }

    fun toggleJuzAmmaOrder() {
        val value = !_juzAmmaFromEnd.value
        prefs.juzAmmaFromEnd = value
        _juzAmmaFromEnd.value = value
    }

    fun memorizedAyahs(surah: Int): Flow<Set<Int>> = dao.observeMemorized(surah).map { it.toSet() }

    fun setMemorized(surah: Int, ayahs: Collection<Int>, memorized: Boolean) {
        if (ayahs.isEmpty()) return
        viewModelScope.launch {
            if (memorized) {
                val now = System.currentTimeMillis()
                dao.insertAll(ayahs.map { HafalanAyahEntity(surah, it, now) })
            } else {
                dao.delete(surah, ayahs.toList())
            }
        }
    }

    /**
     * Rentang awal saat membuka surah: lanjut rentang terakhir kalau ada; kalau belum pernah,
     * mulai dari ayat pertama yang belum hafal, sepanjang 5 ayat.
     */
    fun initialRange(surah: Surah, memorized: Set<Int>): IntRange {
        prefs.lastRange(surah.number)?.let { range ->
            if (range.last <= surah.totalVerses) return range
        }
        val start = (1..surah.totalVerses).firstOrNull { it !in memorized } ?: 1
        val remaining = surah.totalVerses - start + 1
        // Sisa sedikit (surah pendek Juz Amma): ambil sampai akhir, jangan sisakan 1-2 ayat.
        if (remaining <= SHORT_SURAH_LIMIT) return start..surah.totalVerses
        return start..minOf(start + DEFAULT_RANGE_SIZE - 1, surah.totalVerses)
    }

    fun rememberRange(surah: Int, range: IntRange) = prefs.setLastRange(surah, range)

    fun setAyahRepeat(value: Int) = updateSettings { prefs.ayahRepeat = value }
    fun setRangeRepeat(value: Int) = updateSettings { prefs.rangeRepeat = value }
    fun setGap(value: RepeatGap) = updateSettings { prefs.gap = value }
    fun setSpeed(value: Float) = updateSettings { prefs.speed = value }
    fun setShowTranslation(value: Boolean) = updateSettings { prefs.showTranslation = value }
    fun setShowLatin(value: Boolean) = updateSettings { prefs.showLatin = value }

    fun setReciter(reciter: Reciter, surah: Surah) {
        AudioPlaybackService.stop(getApplication())
        updateSettings { prefs.reciterName = reciter.name }
        checkAudio(surah)
    }

    fun checkAudio(surah: Surah) {
        viewModelScope.launch {
            val downloaded = withContext(Dispatchers.IO) {
                audioRepository.isSurahDownloaded(_settings.value.reciter, surah.number, surah.totalVerses)
            }
            _audio.value = _audio.value.copy(isDownloaded = downloaded, error = null)
        }
    }

    fun downloadAudio(surah: Surah) {
        if (_audio.value.isDownloading) return
        viewModelScope.launch {
            _audio.value = HafalanAudioState(isDownloading = true)
            val ok = audioRepository.downloadSurah(_settings.value.reciter, surah.number, surah.totalVerses) { done, total ->
                _audio.value = _audio.value.copy(progress = done.toFloat() / total)
            }
            _audio.value = HafalanAudioState(
                isDownloaded = ok,
                error = if (ok) null else getApplication<Application>().getString(R.string.quran_download_error),
            )
        }
    }

    fun play(surah: Int, range: IntRange) {
        val s = _settings.value
        rememberRange(surah, range)
        AudioPlaybackService.playHafalan(
            getApplication(),
            HafalanPlan(
                surah = surah,
                fromAyah = range.first,
                toAyah = range.last,
                ayahRepeat = s.ayahRepeat,
                rangeRepeat = s.rangeRepeat,
                gapFactor = s.gap.factor,
                speed = s.speed,
            ),
            s.reciter,
        )
    }

    fun togglePause() = AudioPlaybackService.toggle(getApplication())

    fun stop() = AudioPlaybackService.stop(getApplication())

    private fun updateSettings(block: () -> Unit) {
        block()
        _settings.value = readSettings()
    }

    private fun readSettings() = HafalanSettings(
        ayahRepeat = prefs.ayahRepeat,
        rangeRepeat = prefs.rangeRepeat,
        gap = prefs.gap,
        speed = prefs.speed,
        reciter = prefs.reciterName?.let { runCatching { Reciter.valueOf(it) }.getOrNull() } ?: Reciter.ALAFASY,
        showTranslation = prefs.showTranslation,
        showLatin = prefs.showLatin,
    )

    companion object {
        const val DEFAULT_RANGE_SIZE = 5
        const val SHORT_SURAH_LIMIT = 7
    }
}
