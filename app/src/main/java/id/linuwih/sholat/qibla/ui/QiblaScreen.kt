package id.linuwih.sholat.qibla.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.linuwih.sholat.R
import id.linuwih.sholat.util.vibrateOnce

@Composable
fun QiblaScreen(modifier: Modifier = Modifier, viewModel: QiblaViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onLocationPermissionResult(granted) }

    LaunchedEffect(Unit) {
        if (state.permissionDenied) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold(modifier = modifier) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && state.qiblaBearing == null -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.permissionDenied && state.qiblaBearing == null -> {
                    QiblaPermissionDenied(
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    )
                }
                else -> {
                    QiblaContent(state = state, onRefresh = viewModel::refreshLocation)
                }
            }
        }
    }
}

@Composable
private fun QiblaPermissionDenied(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.qibla_permission_message),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text(stringResource(R.string.action_allow_location)) }
    }
}

@Composable
private fun QiblaContent(state: QiblaUiState, onRefresh: () -> Unit) {
    // Kompas 260dp + beberapa baris teks status berisiko overflow di layar kecil atau saat
    // font sistem diperbesar (aksesibilitas) -- ditemukan saat audit scroll/kontras seluruh
    // layar app, sama seperti bug serupa yang sudah diperbaiki di AmalHarianScreen.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.location_label, state.locationLabel ?: stringResource(R.string.location_dash)),
            style = MaterialTheme.typography.bodyMedium,
        )
        state.qiblaBearing?.let {
            Text(
                stringResource(R.string.qibla_direction_format, it),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Spacer(Modifier.height(24.dp))

        if (!state.hasCompassSensor) {
            Text(
                stringResource(R.string.qibla_no_sensor_message),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            val context = LocalContext.current
            // Getar sekali tiap kali TRANSISI dari belum-sejajar ke sejajar (key = isAligned),
            // bukan tiap frame sensor — supaya tidak getar terus-menerus selama masih sejajar.
            LaunchedEffect(state.isAligned) {
                if (state.isAligned) vibrateOnce(context, durationMs = 100)
            }

            CompassArrow(
                rotationDegrees = state.needleRotation,
                northRotation = state.northRotation,
                aligned = state.isAligned,
            )
            Spacer(Modifier.height(16.dp))
            if (state.isAligned) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = QIBLA_ALIGNED_COLOR)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.qibla_aligned_message),
                        color = QIBLA_ALIGNED_COLOR,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                Text(
                    stringResource(R.string.qibla_instruction),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.qibla_off_by, state.offByDegrees),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.accuracyLow) {
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.qibla_low_accuracy),
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onRefresh) { Text(stringResource(R.string.action_refresh_location)) }
    }
}

/** Hijau universal untuk "sejajar" — dipakai tetap sama di light/dark supaya tetap dikenali
 *  sebagai isyarat "benar/sukses", tidak ikut skema warna tema yang bisa berubah-ubah. */
private val QIBLA_ALIGNED_COLOR = Color(0xFF2E7D32)

@Composable
private fun CompassArrow(rotationDegrees: Float, northRotation: Float, aligned: Boolean) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val arrowColor = if (aligned) QIBLA_ALIGNED_COLOR else primary
    val ringColor = if (aligned) QIBLA_ALIGNED_COLOR else outline

    // Putaran dihaluskan supaya piringan tidak "gemetar" mengikuti noise sensor mentah.
    val smoothNeedle by animateFloatAsState(
        targetValue = rotationDegrees,
        animationSpec = tween(120),
        label = "qibla_needle",
    )
    val smoothNorth by animateFloatAsState(
        targetValue = northRotation,
        animationSpec = tween(120),
        label = "qibla_north",
    )

    val cardinals = listOf(
        stringResource(R.string.compass_north),
        stringResource(R.string.compass_east),
        stringResource(R.string.compass_south),
        stringResource(R.string.compass_west),
    )
    val textMeasurer = rememberTextMeasurer()
    val cardinalStyle = MaterialTheme.typography.labelMedium.copy(color = onSurfaceVariant)

    Box(modifier = Modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f - 12.dp.toPx()
            val ringWidth = if (aligned) 5.dp.toPx() else 1.5f.dp.toPx()

            drawCircle(
                color = ringColor,
                radius = radius,
                center = center,
                style = Stroke(width = ringWidth),
            )

            // Piringan mata angin: garis skala tiap 15 derajat (yang tiap 90 derajat lebih
            // panjang & tebal), plus huruf U/T/S/B. Sebelumnya kompas cuma lingkaran polos
            // tanpa acuan apa pun, jadi user tidak bisa tahu ini menghadap ke mana.
            rotate(degrees = smoothNorth, pivot = center) {
                for (angle in 0 until 360 step 15) {
                    val isCardinal = angle % 90 == 0
                    val tickLength = if (isCardinal) 14.dp.toPx() else 7.dp.toPx()
                    val radians = Math.toRadians((angle - 90).toDouble())
                    val outerX = center.x + (radius * kotlin.math.cos(radians)).toFloat()
                    val outerY = center.y + (radius * kotlin.math.sin(radians)).toFloat()
                    val innerX = center.x + ((radius - tickLength) * kotlin.math.cos(radians)).toFloat()
                    val innerY = center.y + ((radius - tickLength) * kotlin.math.sin(radians)).toFloat()
                    drawLine(
                        color = if (isCardinal) onSurfaceVariant else outline,
                        start = Offset(innerX, innerY),
                        end = Offset(outerX, outerY),
                        strokeWidth = if (isCardinal) 2.5f.dp.toPx() else 1f.dp.toPx(),
                    )
                }

                cardinals.forEachIndexed { index, label ->
                    val radians = Math.toRadians((index * 90 - 90).toDouble())
                    val labelRadius = radius - 30.dp.toPx()
                    val cx = center.x + (labelRadius * kotlin.math.cos(radians)).toFloat()
                    val cy = center.y + (labelRadius * kotlin.math.sin(radians)).toFloat()
                    val measured = textMeasurer.measure(label, cardinalStyle)
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            cx - measured.size.width / 2f,
                            cy - measured.size.height / 2f,
                        ),
                    )
                }
            }

            // Jarum kiblat.
            rotate(degrees = smoothNeedle, pivot = center) {
                val arrowPath = Path().apply {
                    moveTo(center.x, center.y - radius * 0.78f)
                    lineTo(center.x - radius * 0.13f, center.y + radius * 0.12f)
                    lineTo(center.x, center.y + radius * 0.02f)
                    lineTo(center.x + radius * 0.13f, center.y + radius * 0.12f)
                    close()
                }
                drawPath(path = arrowPath, color = arrowColor)
                drawLine(
                    color = arrowColor.copy(alpha = 0.35f),
                    start = center,
                    end = Offset(center.x, center.y + radius * 0.6f),
                    strokeWidth = 4f.dp.toPx(),
                )
            }

            drawCircle(color = arrowColor, radius = 4f.dp.toPx(), center = center)

            // Penunjuk tetap di puncak: acuan "arah HP menghadap".
            val markerPath = Path().apply {
                moveTo(center.x, center.y - radius - 10.dp.toPx())
                lineTo(center.x - 6.dp.toPx(), center.y - radius - 1.dp.toPx())
                lineTo(center.x + 6.dp.toPx(), center.y - radius - 1.dp.toPx())
                close()
            }
            drawPath(path = markerPath, color = arrowColor)
        }
    }
}
