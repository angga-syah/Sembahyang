package id.linuwih.sholat.ibadah.ui

import android.app.Application
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.linuwih.sholat.R
import id.linuwih.sholat.ibadah.data.TASBIH_PRESETS
import id.linuwih.sholat.ibadah.data.TasbihPrefs
import id.linuwih.sholat.ui.components.CompactSelector
import id.linuwih.sholat.util.vibrateOnce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasbihScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { TasbihPrefs(context.applicationContext as Application) }

    var count by remember { mutableIntStateOf(prefs.count) }
    var presetIndex by remember { mutableIntStateOf(prefs.presetIndex.coerceIn(0, TASBIH_PRESETS.lastIndex)) }
    val preset = TASBIH_PRESETS[presetIndex]

    fun persist() {
        prefs.count = count
        prefs.presetIndex = presetIndex
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tasbih_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = {
                        count = 0
                        persist()
                    }) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_reset_from_zero))
                    }
                },
            )
        },
    ) { padding ->
        // Lingkaran tap 228dp + progress ring berisiko overflow di layar kecil atau saat font
        // sistem diperbesar -- ditemukan saat audit scroll/kontras seluruh layar app.
        // verticalScroll tidak mengubah tampilan saat konten muat (tetap center di viewport),
        // cuma jadi bisa digulir kalau ternyata kepanjangan.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PresetDropdown(
                selectedIndex = presetIndex,
                onSelected = { index ->
                    presetIndex = index
                    count = 0
                    persist()
                },
            )

            Spacer(Modifier.height(32.dp))

            val progress = if (preset.target > 0) {
                (count.toFloat() / preset.target).coerceIn(0f, 1f)
            } else {
                0f
            }
            val animatedProgress by animateFloatAsState(
                targetValue = progress,
                animationSpec = tween(220),
                label = "tasbih_progress",
            )
            val ringColor = MaterialTheme.colorScheme.primary
            val trackColor = MaterialTheme.colorScheme.surfaceVariant

            Box(
                modifier = Modifier
                    .size(228.dp)
                    // Ring progres 0 - target mengelilingi angka: kemajuan langsung terbaca
                    // sekilas tanpa harus membandingkan dua angka.
                    .drawBehind {
                        val stroke = 10.dp.toPx()
                        val inset = stroke / 2f
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        if (preset.target > 0) {
                            drawArc(
                                color = trackColor,
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = ringColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                topLeft = Offset(inset, inset),
                                size = arcSize,
                                style = Stroke(width = stroke, cap = StrokeCap.Round),
                            )
                        }
                    }
                    .padding(14.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        count++
                        if (preset.target > 0) {
                            if (count == preset.target) vibrateOnce(context)
                            if (count > preset.target) count = 1
                        }
                        persist()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$count", fontSize = 64.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    if (preset.target > 0) {
                        Text(
                            stringResource(R.string.tasbih_target_format, preset.target),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.tasbih_tap_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (preset.target > 0 && count == preset.target) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.tasbih_complete_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun presetLabel(preset: id.linuwih.sholat.ibadah.data.TasbihPreset): String {
    val name = preset.label ?: stringResource(R.string.tasbih_preset_free)
    return if (preset.target > 0) "$name (${preset.target}x)" else name
}

@Composable
private fun PresetDropdown(selectedIndex: Int, onSelected: (Int) -> Unit) {
    CompactSelector(
        label = stringResource(R.string.tasbih_dzikir_label),
        selected = selectedIndex,
        options = TASBIH_PRESETS.indices.toList(),
        optionLabel = { presetLabel(TASBIH_PRESETS[it]) },
        onSelected = onSelected,
    )
}
