package id.linuwih.sholat.quran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.linuwih.sholat.R
import id.linuwih.sholat.quran.audio.Reciter
import id.linuwih.sholat.quran.data.Ayah
import id.linuwih.sholat.quran.data.Surah
import id.linuwih.sholat.quran.data.TranslationLanguage
import id.linuwih.sholat.quran.hafalan.AYAH_REPEAT_OPTIONS
import id.linuwih.sholat.quran.hafalan.HafalanScope
import id.linuwih.sholat.quran.hafalan.HafalanViewModel
import id.linuwih.sholat.quran.hafalan.JUZ_AMMA_FIRST_SURAH
import id.linuwih.sholat.quran.hafalan.RANGE_REPEAT_OPTIONS
import id.linuwih.sholat.quran.hafalan.RepeatGap
import id.linuwih.sholat.quran.hafalan.SPEED_OPTIONS
import id.linuwih.sholat.quran.hafalan.SurahHafalanStatus
import id.linuwih.sholat.quran.hafalan.SurahProgress
import id.linuwih.sholat.ui.components.CompactSelector

// ---------------------------------------------------------------------------------------------
// Beranda hafalan: cakupan (Juz Amma / semua surah), ringkasan progres, daftar surah + status.
// ---------------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HafalanHomeScreen(
    quranViewModel: QuranViewModel,
    viewModel: HafalanViewModel,
    onBack: () -> Unit,
    onOpenSurah: (Int) -> Unit,
) {
    val surahs by quranViewModel.surahs.collectAsState()
    val counts by viewModel.memorizedCounts.collectAsState()
    val scope by viewModel.scope.collectAsState()
    val fromEnd by viewModel.juzAmmaFromEnd.collectAsState()

    val progress = remember(surahs, counts, scope, fromEnd) {
        val inScope = surahs.filter { scope == HafalanScope.SEMUA || it.number >= JUZ_AMMA_FIRST_SURAH }
        val ordered = if (scope == HafalanScope.JUZ_AMMA && fromEnd) inScope.sortedByDescending { it.number } else inScope
        ordered.map { SurahProgress(it, counts[it.number] ?: 0) }
    }
    val totalAyahs = progress.sumOf { it.surah.totalVerses }
    val memorizedAyahs = progress.sumOf { minOf(it.memorized, it.surah.totalVerses) }
    val memorizedSurahs = progress.count { it.status == SurahHafalanStatus.HAFAL }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hafalan_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    if (scope == HafalanScope.JUZ_AMMA) {
                        IconButton(onClick = viewModel::toggleJuzAmmaOrder) {
                            Icon(Icons.Filled.SwapVert, contentDescription = stringResource(R.string.hafalan_toggle_order))
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (surahs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = scope == HafalanScope.JUZ_AMMA,
                        onClick = { viewModel.setScope(HafalanScope.JUZ_AMMA) },
                        label = { Text(stringResource(R.string.quran_juz_amma_chip)) },
                    )
                    FilterChip(
                        selected = scope == HafalanScope.SEMUA,
                        onClick = { viewModel.setScope(HafalanScope.SEMUA) },
                        label = { Text(stringResource(R.string.hafalan_scope_all)) },
                    )
                }
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            stringResource(R.string.hafalan_summary_ayahs, memorizedAyahs, totalAyahs),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (totalAyahs == 0) 0f else memorizedAyahs.toFloat() / totalAyahs },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.hafalan_summary_surahs, memorizedSurahs, progress.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(progress, key = { it.surah.number }) { item ->
                HafalanSurahRow(item, onClick = { onOpenSurah(item.surah.number) })
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun HafalanSurahRow(item: SurahProgress, onClick: () -> Unit) {
    val surah = item.surah
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            Text("${surah.number}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(surah.transliteration, style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.hafalan_surah_progress, minOf(item.memorized, surah.totalVerses), surah.totalVerses),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.status == SurahHafalanStatus.SEDANG) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { item.memorized.toFloat() / surah.totalVerses },
                    modifier = Modifier.fillMaxWidth(0.6f).height(3.dp),
                )
            }
        }
        StatusLabel(item.status)
    }
}

@Composable
private fun StatusLabel(status: SurahHafalanStatus) {
    val (text, container, content) = when (status) {
        SurahHafalanStatus.HAFAL -> Triple(
            R.string.hafalan_status_memorized,
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.onPrimary,
        )
        SurahHafalanStatus.SEDANG -> Triple(
            R.string.hafalan_status_in_progress,
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        SurahHafalanStatus.BELUM -> Triple(
            R.string.hafalan_status_not_started,
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(shape = MaterialTheme.shapes.small, color = container) {
        Text(
            stringResource(text),
            style = MaterialTheme.typography.labelSmall,
            color = content,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// Sesi hafalan satu surah: pilih rentang, atur ulangan, dengar-tirukan, tandai hafal.
// ---------------------------------------------------------------------------------------------

/** Jumlah item di atas daftar ayat dalam LazyColumn sesi — dipakai untuk auto-scroll. */
private const val SESSION_HEADER_ITEMS = 1

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HafalanSessionScreen(
    surahNumber: Int,
    quranViewModel: QuranViewModel,
    viewModel: HafalanViewModel,
    onBack: () -> Unit,
) {
    val surahs by quranViewModel.surahs.collectAsState()
    val surah = surahs.find { it.number == surahNumber }
    var ayahs by remember(surahNumber) { mutableStateOf<List<Ayah>?>(null) }
    val reloadTrigger by quranViewModel.ayahReloadTrigger.collectAsState()
    LaunchedEffect(surahNumber, reloadTrigger) { ayahs = quranViewModel.ayahsFor(surahNumber) }

    val memorized by remember(surahNumber) { viewModel.memorizedAyahs(surahNumber) }.collectAsState(initial = null)
    val settings by viewModel.settings.collectAsState()
    val audio by viewModel.audio.collectAsState()
    val playback by viewModel.playback.collectAsState()

    var range by remember(surahNumber) { mutableStateOf<IntRange?>(null) }
    LaunchedEffect(surah, memorized) {
        val s = surah ?: return@LaunchedEffect
        val m = memorized ?: return@LaunchedEffect
        if (range == null) range = viewModel.initialRange(s, m)
    }
    LaunchedEffect(surah, settings.reciter) { surah?.let { viewModel.checkAudio(it) } }

    val session = playback.hafalan?.takeIf { playback.isActive && it.plan.surah == surahNumber }
    val listState = rememberLazyListState()
    val currentRange = range

    // Ikuti ayat yang sedang diputar supaya teksnya selalu terlihat tanpa perlu menggulir.
    LaunchedEffect(session?.position?.ayah, currentRange) {
        val ayah = session?.position?.ayah ?: return@LaunchedEffect
        val r = currentRange ?: return@LaunchedEffect
        if (ayah in r) listState.animateScrollToItem(SESSION_HEADER_ITEMS + (ayah - r.first))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(surah?.transliteration ?: stringResource(R.string.quran_surah_fallback, surahNumber)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        // Kontrol putar di bawah, selalu terlihat — daftar ayat ikut menggulir mengikuti
        // ayat yang sedang diputar, jadi kontrol di dalam daftar akan cepat hilang dari layar.
        bottomBar = {
            val r = range
            if (surah != null && r != null) {
                Surface(tonalElevation = 3.dp) {
                    val currentRange = r
                    PlayerBar(
                        audioDownloaded = audio.isDownloaded,
                        downloading = audio.isDownloading,
                        downloadProgress = audio.progress,
                        error = audio.error,
                        sessionActive = session != null,
                        isPlaying = playback.isPlaying,
                        statusText = session?.let { s ->
                            val p = s.position
                            when {
                                p.inGap -> stringResource(R.string.hafalan_your_turn, p.ayah)
                                else -> stringResource(R.string.hafalan_playing_status, p.ayah, p.ayahRepeatIndex, s.plan.ayahRepeat) +
                                    if (s.plan.rangeRepeat != 1) {
                                        " · " + if (s.plan.rangeRepeat == 0) {
                                            stringResource(R.string.hafalan_round_forever, p.rangeRepeatIndex)
                                        } else {
                                            stringResource(R.string.hafalan_round, p.rangeRepeatIndex, s.plan.rangeRepeat)
                                        }
                                    } else {
                                        ""
                                    }
                            }
                        },
                        onDownload = { viewModel.downloadAudio(surah) },
                        onPlay = { viewModel.play(surahNumber, currentRange) },
                        onTogglePause = viewModel::togglePause,
                        onStop = viewModel::stop,
                    )
                }
            }
        },
    ) { padding ->
        val list = ayahs
        val mem = memorized
        if (surah == null || list == null || mem == null || currentRange == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        fun changeRange(newRange: IntRange) {
            range = newRange
            viewModel.rememberRange(surahNumber, newRange)
        }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding)) {
            item {
                RangeSelector(
                    surah = surah,
                    range = currentRange,
                    onRangeChange = ::changeRange,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    maxItemsInEachRow = 3,
                ) {
                    CompactSelector(
                        label = stringResource(R.string.hafalan_ayah_repeat),
                        selected = settings.ayahRepeat,
                        options = AYAH_REPEAT_OPTIONS,
                        optionLabel = { stringResource(R.string.hafalan_times, it) },
                        onSelected = viewModel::setAyahRepeat,
                        modifier = Modifier.weight(1f),
                    )
                    CompactSelector(
                        label = stringResource(R.string.hafalan_range_repeat),
                        selected = settings.rangeRepeat,
                        options = RANGE_REPEAT_OPTIONS,
                        optionLabel = {
                            if (it == 0) stringResource(R.string.hafalan_repeat_forever) else stringResource(R.string.hafalan_times, it)
                        },
                        onSelected = viewModel::setRangeRepeat,
                        modifier = Modifier.weight(1f),
                    )
                    CompactSelector(
                        label = stringResource(R.string.hafalan_gap),
                        selected = settings.gap,
                        options = RepeatGap.entries,
                        optionLabel = { gapLabel(it) },
                        onSelected = viewModel::setGap,
                        modifier = Modifier.weight(1f),
                    )
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                        CompactSelector(
                            label = stringResource(R.string.hafalan_speed),
                            selected = settings.speed,
                            options = SPEED_OPTIONS,
                            optionLabel = { speedLabel(it) },
                            onSelected = viewModel::setSpeed,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    CompactSelector(
                        label = stringResource(R.string.quran_reciter_label),
                        selected = settings.reciter,
                        options = Reciter.entries,
                        optionLabel = { it.displayName },
                        onSelected = { viewModel.setReciter(it, surah) },
                        modifier = Modifier.weight(2f),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = settings.showTranslation,
                        onClick = { viewModel.setShowTranslation(!settings.showTranslation) },
                        label = { Text(stringResource(R.string.hafalan_show_translation)) },
                    )
                    FilterChip(
                        selected = settings.showLatin,
                        onClick = { viewModel.setShowLatin(!settings.showLatin) },
                        label = { Text(stringResource(R.string.hafalan_show_latin)) },
                    )
                }
            }
            val rangeAyahs = list.filter { it.number in currentRange }
            items(rangeAyahs, key = { it.number }) { ayah ->
                HafalanAyahRow(
                    ayah = ayah,
                    quranViewModel = quranViewModel,
                    showTranslation = settings.showTranslation,
                    showLatin = settings.showLatin,
                    highlighted = session?.position?.ayah == ayah.number,
                    memorized = ayah.number in mem,
                    onToggleMemorized = { viewModel.setMemorized(surahNumber, listOf(ayah.number), ayah.number !in mem) },
                )
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
            }
            item {
                val allMemorized = currentRange.all { it in mem }
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!allMemorized) {
                        Button(
                            onClick = { viewModel.setMemorized(surahNumber, currentRange.toList(), true) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.hafalan_mark_range)) }
                    }
                    if (currentRange.last < surah.totalVerses) {
                        OutlinedButton(
                            onClick = {
                                viewModel.stop()
                                val size = currentRange.last - currentRange.first + 1
                                val start = currentRange.last + 1
                                changeRange(start..minOf(start + size - 1, surah.totalVerses))
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(R.string.hafalan_next_range))
                            Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RangeSelector(surah: Surah, range: IntRange, onRangeChange: (IntRange) -> Unit) {
    val total = surah.totalVerses
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.hafalan_range_label), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Stepper(
            value = range.first,
            canDecrease = range.first > 1,
            canIncrease = range.first < range.last,
            onChange = { onRangeChange(it..range.last) },
        )
        Text("–", modifier = Modifier.padding(horizontal = 4.dp))
        Stepper(
            value = range.last,
            canDecrease = range.last > range.first,
            canIncrease = range.last < total,
            onChange = { onRangeChange(range.first..it) },
        )
    }
    Text(
        stringResource(R.string.hafalan_range_hint, range.last - range.first + 1, total),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun Stepper(value: Int, canDecrease: Boolean, canIncrease: Boolean, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onChange(value - 1) }, enabled = canDecrease, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.hafalan_decrease))
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(32.dp),
        )
        IconButton(onClick = { onChange(value + 1) }, enabled = canIncrease, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.hafalan_increase))
        }
    }
}

@Composable
private fun PlayerBar(
    audioDownloaded: Boolean,
    downloading: Boolean,
    downloadProgress: Float,
    error: String?,
    sessionActive: Boolean,
    isPlaying: Boolean,
    statusText: String?,
    onDownload: () -> Unit,
    onPlay: () -> Unit,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            when {
                downloading -> Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(progress = { downloadProgress }, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                !audioDownloaded -> FilledIconButton(onClick = onDownload) {
                    Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.action_download_audio))
                }
                else -> FilledIconButton(onClick = if (sessionActive) onTogglePause else onPlay) {
                    Icon(
                        if (sessionActive && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = stringResource(if (sessionActive && isPlaying) R.string.action_pause else R.string.action_play),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                when {
                    statusText != null -> statusText
                    downloading -> stringResource(R.string.hafalan_downloading)
                    !audioDownloaded -> stringResource(R.string.hafalan_need_download)
                    else -> stringResource(R.string.hafalan_ready)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = if (statusText != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (sessionActive) {
                IconButton(onClick = onStop) {
                    Icon(Icons.Filled.Stop, contentDescription = stringResource(R.string.action_stop))
                }
            }
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HafalanAyahRow(
    ayah: Ayah,
    quranViewModel: QuranViewModel,
    showTranslation: Boolean,
    showLatin: Boolean,
    highlighted: Boolean,
    memorized: Boolean,
    onToggleMemorized: () -> Unit,
) {
    val arabicFont by quranViewModel.arabicFont.collectAsState()
    val mushafStyle by quranViewModel.mushafStyle.collectAsState()
    val fontSize by quranViewModel.fontSize.collectAsState()
    val tajweedEnabled by quranViewModel.tajweedEnabled.collectAsState()
    val latinStyle by quranViewModel.latinStyle.collectAsState()
    val translationLanguage by quranViewModel.translationLanguage.collectAsState()
    val scale = fontSize.scale

    Surface(
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = androidx.compose.foundation.shape.CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        "${ayah.number}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Row(
                    modifier = Modifier.clickable(onClick = onToggleMemorized).padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        stringResource(if (memorized) R.string.hafalan_status_memorized else R.string.hafalan_mark_ayah),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (memorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (memorized) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (memorized) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val arabicBase = MaterialTheme.typography.headlineSmall
            val arabicFontSize = arabicBase.fontSize * scale
            val plainColor = LocalContentColor.current
            val arabicText = remember(ayah.number, ayah.arabicTextTajweed, tajweedEnabled, plainColor) {
                buildTajweedAnnotatedString(
                    text = ayah.arabicTextTajweed,
                    rules = TajweedEngine.compute(ayah.arabicTextTajweed),
                    tajweedEnabled = tajweedEnabled,
                    plainColor = plainColor,
                )
            }
            Text(
                arabicText,
                style = arabicBase.copy(
                    fontSize = arabicFontSize,
                    lineHeight = arabicFontSize * 1.9f,
                    fontFamily = arabicFont.toFontFamily(mushafStyle),
                ),
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth(),
            )
            if (showLatin) {
                ayah.latinText(latinStyle)?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            if (showTranslation) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (translationLanguage == TranslationLanguage.INGGRIS) ayah.translationEn else ayah.translationId,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

private fun speedLabel(speed: Float): String =
    if (speed % 1f == 0f) "${speed.toInt()}x" else "${speed}x".replace('.', ',')

@Composable
private fun gapLabel(gap: RepeatGap): String = stringResource(
    when (gap) {
        RepeatGap.MATI -> R.string.hafalan_gap_off
        RepeatGap.SATU_KALI -> R.string.hafalan_gap_1x
        RepeatGap.SATU_SETENGAH -> R.string.hafalan_gap_15x
    },
)
