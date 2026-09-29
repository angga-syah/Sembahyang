package id.linuwih.sholat.prayer.ui

import android.Manifest
import com.batoulapps.adhan2.Prayer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.linuwih.sholat.R
import id.linuwih.sholat.prayer.data.PrayerCalculationMethod
import id.linuwih.sholat.prayer.data.PrayerLabels
import id.linuwih.sholat.ui.components.CompactSelector

@Composable
fun PrayerScreen(modifier: Modifier = Modifier, viewModel: PrayerViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "today", modifier = modifier) {
        composable("today") {
            TodayScreen(viewModel = viewModel, onOpenMonthly = { navController.navigate("monthly") })
        }
        composable("monthly") {
            MonthlyPrayerScreen(viewModel = viewModel, onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun TodayScreen(viewModel: PrayerViewModel, onOpenMonthly: () -> Unit) {
    val state by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onLocationPermissionResult(granted) }

    LaunchedEffect(Unit) {
        if (state.permissionDenied) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Scaffold { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && state.rows.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.permissionDenied && state.rows.isEmpty() -> {
                    PermissionDeniedContent(
                        onRequestPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                        onManualLocation = viewModel::setManualLocation,
                    )
                }
                else -> {
                    PrayerContent(state = state, viewModel = viewModel, onOpenMonthly = onOpenMonthly)
                }
            }
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit,
    onManualLocation: (Double, Double) -> Unit,
) {
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(R.string.prayer_permission_message),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermission) { Text(stringResource(R.string.action_allow_location)) }

        Spacer(Modifier.height(24.dp))
        Text(stringResource(R.string.manual_location_hint), style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = lat, onValueChange = { lat = it }, label = { Text(stringResource(R.string.label_latitude)) })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = lng, onValueChange = { lng = it }, label = { Text(stringResource(R.string.label_longitude)) })
        Spacer(Modifier.height(8.dp))
        Button(onClick = {
            val latitude = lat.toDoubleOrNull()
            val longitude = lng.toDoubleOrNull()
            if (latitude != null && longitude != null) onManualLocation(latitude, longitude)
        }) { Text(stringResource(R.string.action_save_location)) }
    }
}

@Composable
private fun PrayerContent(state: PrayerUiState, viewModel: PrayerViewModel, onOpenMonthly: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        // Hirarki dibalik. Dulu semua ditumpuk dalam satu Card dengan urutan tanggal - lokasi -
        // sholat berikutnya - hitung mundur, sehingga informasi terpenting justru paling bawah
        // dan paling kecil. Sekarang hitung mundur jadi fokus utama, sisanya jadi konteks kecil
        // di sekitarnya, dan Card dihapus supaya tidak ada kotak di dalam kotak.
        Spacer(Modifier.height(20.dp))

        if (state.gregorianDate.isNotBlank()) {
            Text(
                state.gregorianDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                state.hijriDate,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        state.nextPrayerLabel?.let { label ->
            Spacer(Modifier.height(20.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            state.nextPrayerCountdown?.let { countdown ->
                Text(
                    countdown,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Spacer(Modifier.height(6.dp))
        val locationText = state.locationLabel ?: stringResource(R.string.location_dash)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.LocationOn,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp),
            )
            Text(
                if (state.isManualLocation) {
                    stringResource(R.string.location_label_manual, locationText)
                } else {
                    locationText
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        state.errorMessage?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.height(24.dp))

        // Daftar waktu tanpa kotak & tanpa garis penuh antar baris — waktu yang sedang berjalan
        // ditandai dengan latar lembut, jadi mata langsung tertuju ke sana tanpa perlu garis.
        state.rows.forEach { row ->
            val highlighted = row.isNext
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        if (highlighted) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        },
                    )
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    row.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    row.timeText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                // Terbit bukan waktu sholat — diberi ruang kosong selebar tombol supaya kolom
                // jam tetap lurus antar baris.
                if (row.prayer != Prayer.SUNRISE) {
                    val enabled = state.notificationsEnabled[row.prayer] ?: true
                    IconButton(onClick = { viewModel.setNotificationEnabled(row.prayer, !enabled) }) {
                        Icon(
                            if (enabled) Icons.Filled.Notifications else Icons.Filled.NotificationsOff,
                            contentDescription = stringResource(
                                if (enabled) R.string.prayer_notif_toggle_on else R.string.prayer_notif_toggle_off,
                                row.label,
                            ),
                            tint = if (enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                            modifier = Modifier.size(20.dp),
                        )
                    }
                } else {
                    Spacer(Modifier.width(48.dp))
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        MethodDropdown(selected = state.method, onSelected = viewModel::setCalculationMethod)

        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = viewModel::refreshLocation) { Text(stringResource(R.string.action_refresh_location)) }
            TextButton(onClick = onOpenMonthly) { Text(stringResource(R.string.prayer_monthly_schedule)) }
        }
        Spacer(Modifier.height(16.dp))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MonthlyPrayerScreen(viewModel: PrayerViewModel, onBack: () -> Unit) {
    var monthOffset by remember { mutableIntStateOf(0) }
    // Menghitung sebulan penuh berarti perhitungan astronomis 28-31 kali. Dulu dikerjakan
    // langsung di dalam composition, jadi thread UI tertahan setiap kali layar dibuka atau
    // bulan diganti — itu yang terasa seperti nge-lag saat menggulir.
    var schedule by remember { mutableStateOf<MonthlySchedule?>(null) }
    LaunchedEffect(monthOffset) {
        schedule = withContext(Dispatchers.Default) { viewModel.monthlySchedule(monthOffset) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(schedule?.monthLabel ?: stringResource(R.string.prayer_monthly_schedule)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                actions = {
                    IconButton(onClick = { monthOffset-- }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.action_previous_month))
                    }
                    IconButton(onClick = { monthOffset++ }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.action_next_month))
                    }
                },
            )
        },
    ) { padding ->
        val currentSchedule = schedule
        if (currentSchedule == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                // Selama masih dihitung, tampilkan indikator — bukan pesan "belum ada lokasi",
                // karena keduanya kondisi yang berbeda.
                if (viewModel.hasSavedLocation()) {
                    CircularProgressIndicator()
                } else {
                    Text(
                        stringResource(R.string.prayer_monthly_no_location),
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.prayer_monthly_date_header),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1.4f),
                )
                listOf(Prayer.FAJR, Prayer.DHUHR, Prayer.ASR, Prayer.MAGHRIB, Prayer.ISHA).forEach { prayer ->
                    Text(
                        PrayerLabels.label(androidx.compose.ui.platform.LocalContext.current, prayer),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
            androidx.compose.material3.HorizontalDivider()

            LazyColumn {
                items(currentSchedule.rows, key = { it.dateLabel }) { row ->
                    // Baris hari ini diberi latar & huruf tebal supaya langsung ketemu di
                    // daftar sepanjang satu bulan.
                    val rowColor = if (row.isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                    val rowWeight = if (row.isToday) FontWeight.Bold else FontWeight.Normal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (row.isToday) {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                } else {
                                    androidx.compose.ui.graphics.Color.Transparent
                                },
                            )
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            row.dateLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = rowColor,
                            fontWeight = rowWeight,
                            modifier = Modifier.weight(1.4f),
                        )
                        listOf(row.fajr, row.dhuhr, row.asr, row.maghrib, row.isha).forEach {
                            Text(
                                it,
                                style = MaterialTheme.typography.bodySmall,
                                color = rowColor,
                                fontWeight = rowWeight,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun calculationMethodLabel(method: PrayerCalculationMethod): String = stringResource(
    when (method) {
        PrayerCalculationMethod.KEMENAG_RI -> R.string.prayer_method_kemenag
        PrayerCalculationMethod.MUSLIM_WORLD_LEAGUE -> R.string.prayer_method_mwl
        PrayerCalculationMethod.EGYPTIAN -> R.string.prayer_method_egyptian
        PrayerCalculationMethod.KARACHI -> R.string.prayer_method_karachi
        PrayerCalculationMethod.UMM_AL_QURA -> R.string.prayer_method_umm_al_qura
        PrayerCalculationMethod.NORTH_AMERICA -> R.string.prayer_method_isna
        PrayerCalculationMethod.SINGAPORE -> R.string.prayer_method_singapore
        PrayerCalculationMethod.TURKEY -> R.string.prayer_method_turkey
        PrayerCalculationMethod.DUBAI -> R.string.prayer_method_dubai
        PrayerCalculationMethod.KUWAIT -> R.string.prayer_method_kuwait
        PrayerCalculationMethod.QATAR -> R.string.prayer_method_qatar
        PrayerCalculationMethod.MOON_SIGHTING_COMMITTEE -> R.string.prayer_method_moonsighting
    },
)

@Composable
private fun MethodDropdown(selected: PrayerCalculationMethod, onSelected: (PrayerCalculationMethod) -> Unit) {
    CompactSelector(
        label = stringResource(R.string.prayer_calculation_method),
        selected = selected,
        options = PrayerCalculationMethod.entries,
        optionLabel = { calculationMethodLabel(it) },
        onSelected = onSelected,
    )
}
