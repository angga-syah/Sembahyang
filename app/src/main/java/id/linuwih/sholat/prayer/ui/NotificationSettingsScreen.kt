package id.linuwih.sholat.prayer.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.batoulapps.adhan2.Prayer
import id.linuwih.sholat.R
import id.linuwih.sholat.prayer.data.AdhanSound
import id.linuwih.sholat.prayer.data.AdhanSoundSource
import id.linuwih.sholat.prayer.data.NotificationStyle
import id.linuwih.sholat.prayer.data.PrayerLabels
import id.linuwih.sholat.prayer.data.PrayerPrefs
import id.linuwih.sholat.prayer.notification.AdhanPlaybackService
import id.linuwih.sholat.prayer.notification.NotificationHelper
import id.linuwih.sholat.prayer.notification.PrayerAlarmScheduler
import id.linuwih.sholat.util.HapticPrefs
import id.linuwih.sholat.util.vibrateOnce

private val REMINDER_OPTIONS = listOf(0, 5, 10, 15, 20)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotificationSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { PrayerPrefs(context.applicationContext) }

    var style by remember { mutableStateOf(prefs.notificationStyle) }
    var soundUri by remember { mutableStateOf(prefs.notificationSoundUri) }
    var reminderMinutes by remember { mutableIntStateOf(prefs.reminderMinutesBefore) }
    var adhanSource by remember { mutableStateOf(prefs.adhanSoundSource) }
    var customFileReady by remember { mutableStateOf(AdhanSound.customFile(context).length() > 0) }
    val hapticPrefs = remember { HapticPrefs(context.applicationContext) }
    var hapticEnabled by remember { mutableStateOf(hapticPrefs.enabled) }
    var offsets by remember {
        mutableStateOf(PrayerAlarmScheduler.prayers.associateWith { prefs.offsetMinutes(it) })
    }
    var perPrayer by remember {
        mutableStateOf(PrayerAlarmScheduler.prayers.associateWith { prefs.isNotificationEnabled(it) })
    }

    // Sejak Android 13 izin notifikasi harus diminta saat aplikasi berjalan. Selama ini izin itu
    // hanya dideklarasikan di manifest dan DIPERIKSA sebelum menampilkan notifikasi, tapi tidak
    // pernah diminta — sehingga tidak pernah diberikan, dan semua notifikasi adzan berhenti
    // diam-diam tanpa jejak apa pun di layar.
    fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    var allowed by remember { mutableStateOf(notificationsAllowed()) }
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        allowed = granted
        if (granted) PrayerAlarmScheduler.scheduleForToday(context)
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        allowed = notificationsAllowed()
        if (!allowed) requestNotificationPermission()
    }

    // Alarm presisi punya masalah yang sama seperti izin notifikasi: dicek di kode, lalu diam-diam
    // turun ke alarm tidak presisi kalau tidak diizinkan. Bedanya, ini tidak bisa diminta lewat
    // dialog — user harus mengaktifkannya sendiri di Setelan sistem.
    var exactAlarmAllowed by remember { mutableStateOf(PrayerAlarmScheduler.canScheduleExactAlarms(context)) }

    // Penghemat baterai adalah penyebab paling umum alarm Android tidak berbunyi, dan selama ini
    // aplikasi sama sekali tidak memeriksanya — layar Tentang cuma memperingatkan soal Autostart
    // tanpa membantu memperbaikinya.
    fun batteryUnrestricted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        val power = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return power.isIgnoringBatteryOptimizations(context.packageName)
    }

    var batteryUnrestricted by remember { mutableStateOf(batteryUnrestricted()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // Diperiksa ulang saat kembali dari Setelan sistem, supaya peringatannya langsung hilang.
            if (event == Lifecycle.Event.ON_RESUME) {
                allowed = notificationsAllowed()
                exactAlarmAllowed = PrayerAlarmScheduler.canScheduleExactAlarms(context)
                batteryUnrestricted = batteryUnrestricted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Setelan apa pun yang mengubah suara/getar mengharuskan channel dibuat ulang (Android 8+),
    // lalu alarm dijadwalkan ulang supaya perubahan langsung berlaku hari ini juga.
    fun applyAndReschedule() {
        NotificationHelper.applySettingsChange(context)
        PrayerAlarmScheduler.scheduleForToday(context)
    }

    // Berkas disalin ke penyimpanan aplikasi, bukan dipakai langsung dari URI pemilih:
    // suara notifikasi dibunyikan oleh sistem berjam-jam kemudian (bahkan setelah restart),
    // saat izin baca URI itu bisa sudah tidak berlaku atau berkasnya sudah dipindah user.
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (AdhanSound.importCustomFile(context, uri)) {
            customFileReady = true
            adhanSource = AdhanSoundSource.CUSTOM_FILE
            prefs.adhanSoundSource = AdhanSoundSource.CUSTOM_FILE
            applyAndReschedule()
        }
    }

    val ringtonePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val uri = result.data?.let {
            IntentCompat.getParcelableExtra(it, RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        }
        soundUri = uri?.toString()
        prefs.notificationSoundUri = soundUri
        adhanSource = AdhanSoundSource.SYSTEM_RINGTONE
        prefs.adhanSoundSource = AdhanSoundSource.SYSTEM_RINGTONE
        applyAndReschedule()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.notif_settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Pilihan singkat dan saling eksklusif seperti ini lebih cocok jadi chip sebaris
            // daripada daftar radio memanjang: ketiganya terlihat sekaligus, dan layar ini
            // jadi jauh lebih pendek untuk digulir.
            // Tiga pembatasan sistem yang sama-sama bisa membuat adzan diam tanpa jejak apa pun.
            // Ditampilkan bersama di puncak layar karena setelan apa pun di bawahnya tidak ada
            // gunanya selama salah satunya masih memblokir.
            if (!allowed) {
                RestrictionBanner(
                    title = stringResource(R.string.notif_permission_missing_title),
                    body = stringResource(R.string.notif_permission_missing_body),
                    actionLabel = stringResource(R.string.notif_permission_grant),
                    severe = true,
                    onAction = { requestNotificationPermission() },
                )
            }

            if (!exactAlarmAllowed) {
                RestrictionBanner(
                    title = stringResource(R.string.notif_exact_alarm_title),
                    body = stringResource(R.string.notif_exact_alarm_body),
                    actionLabel = stringResource(R.string.notif_exact_alarm_open),
                    onAction = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    },
                                )
                            }
                        }
                    },
                )
            }

            if (!batteryUnrestricted) {
                RestrictionBanner(
                    title = stringResource(R.string.notif_battery_title),
                    body = stringResource(R.string.notif_battery_body),
                    actionLabel = stringResource(R.string.notif_battery_open),
                    onAction = {
                        // Dialog langsung lebih dulu; kalau ditolak sistem, buka daftar setelannya
                        // supaya user tetap punya jalan, bukan tombol yang diam saja.
                        val direct = runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                },
                            )
                        }.isSuccess
                        if (!direct) {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                        }
                    },
                )
            }

            Text(stringResource(R.string.notif_settings_style), style = MaterialTheme.typography.titleMedium)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                NotificationStyle.entries.forEach { option ->
                    FilterChip(
                        selected = style == option,
                        onClick = {
                            style = option
                            prefs.notificationStyle = option
                            applyAndReschedule()
                        },
                        label = {
                            Text(
                                stringResource(
                                    when (option) {
                                        NotificationStyle.SOUND -> R.string.notif_style_sound
                                        NotificationStyle.VIBRATE -> R.string.notif_style_vibrate
                                        NotificationStyle.SILENT -> R.string.notif_style_silent
                                    },
                                ),
                            )
                        },
                    )
                }
            }

            if (style == NotificationStyle.SOUND) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.adhan_source_title), style = MaterialTheme.typography.titleSmall)

                AdhanSoundSource.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                when (option) {
                                    AdhanSoundSource.SYSTEM_RINGTONE -> {
                                        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                                            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_TITLE,
                                                context.getString(R.string.notif_pick_sound),
                                            )
                                            putExtra(
                                                RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
                                                soundUri?.let(Uri::parse)
                                                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                                            )
                                        }
                                        ringtonePicker.launch(intent)
                                    }
                                    AdhanSoundSource.CUSTOM_FILE -> filePicker.launch(arrayOf("audio/*"))
                                    else -> {
                                        adhanSource = option
                                        prefs.adhanSoundSource = option
                                        applyAndReschedule()
                                    }
                                }
                            }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = adhanSource == option, onClick = null)
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                stringResource(
                                    when (option) {
                                        AdhanSoundSource.SYSTEM_DEFAULT -> R.string.adhan_source_system
                                        AdhanSoundSource.BUNDLED_ADHAN -> R.string.adhan_source_bundled
                                        AdhanSoundSource.SYSTEM_RINGTONE -> R.string.adhan_source_ringtone
                                        AdhanSoundSource.CUSTOM_FILE -> R.string.adhan_source_file
                                    },
                                ),
                            )
                            val subtitle = when (option) {
                                AdhanSoundSource.BUNDLED_ADHAN -> stringResource(R.string.adhan_source_bundled_note)
                                AdhanSoundSource.SYSTEM_RINGTONE -> soundUri?.let { uri ->
                                    runCatching {
                                        RingtoneManager.getRingtone(context, Uri.parse(uri))?.getTitle(context)
                                    }.getOrNull()
                                }
                                AdhanSoundSource.CUSTOM_FILE -> if (customFileReady) {
                                    stringResource(R.string.adhan_source_file_ready)
                                } else {
                                    stringResource(R.string.adhan_source_file_hint)
                                }
                                else -> null
                            }
                            subtitle?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }

                Text(
                    stringResource(R.string.notif_sound_silent_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // Tanpa tombol ini, satu-satunya cara memastikan setelan suara benar-benar berlaku
            // adalah menunggu waktu sholat berikutnya — dan kalau ternyata salah, baru ketahuan
            // setelah terlewat.
            OutlinedButton(
                onClick = {
                    // Kalau izinnya belum ada, minta dulu — jangan memanggil notifikasi yang
                    // pasti berhenti diam-diam dan membuat tombol ini seolah rusak.
                    when {
                        !allowed -> requestNotificationPermission()
                        // Tes harus melewati jalur yang SAMA dengan adzan sungguhan, kalau tidak
                        // hasil tesnya tidak membuktikan apa-apa.
                        style == NotificationStyle.SOUND ->
                            AdhanPlaybackService.play(context, context.getString(R.string.notif_test_title))
                        else -> NotificationHelper.showTestNotification(context)
                    }
                },
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(stringResource(R.string.notif_test_button))
            }

            Spacer(Modifier.height(28.dp))

            Text(stringResource(R.string.notif_settings_reminder), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.notif_settings_reminder_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            // FlowRow, bukan Row: lima chip berjajar bisa terpotong di layar sempit atau saat
            // ukuran font sistem diperbesar.
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                REMINDER_OPTIONS.forEach { minutes ->
                    FilterChip(
                        selected = reminderMinutes == minutes,
                        onClick = {
                            reminderMinutes = minutes
                            prefs.reminderMinutesBefore = minutes
                            PrayerAlarmScheduler.scheduleForToday(context)
                        },
                        label = {
                            Text(
                                if (minutes == 0) stringResource(R.string.notif_reminder_off)
                                else stringResource(R.string.notif_reminder_minutes, minutes),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(stringResource(R.string.haptic_settings_title), style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        hapticEnabled = !hapticEnabled
                        hapticPrefs.enabled = hapticEnabled
                        // Getarkan sekali begitu dinyalakan supaya user langsung tahu ini bekerja
                        // (atau tahu kalau ROM-nya memang meredam getaran).
                        if (hapticEnabled) vibrateOnce(context)
                    }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.haptic_toggle_label))
                    Text(
                        stringResource(R.string.haptic_toggle_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = hapticEnabled, onCheckedChange = null)
            }

            Spacer(Modifier.height(28.dp))

            Text(stringResource(R.string.prayer_offset_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.prayer_offset_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PrayerAlarmScheduler.prayers.forEach { prayer ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(PrayerLabels.label(context, prayer), modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            val next = (offsets[prayer] ?: 0) - 1
                            prefs.setOffsetMinutes(prayer, next)
                            offsets = offsets + (prayer to next)
                            PrayerAlarmScheduler.scheduleForToday(context)
                        },
                    ) { Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.prayer_offset_minus)) }
                    Text(
                        stringResource(R.string.prayer_offset_value, offsets[prayer] ?: 0),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(56.dp),
                        textAlign = TextAlign.Center,
                    )
                    IconButton(
                        onClick = {
                            val next = (offsets[prayer] ?: 0) + 1
                            prefs.setOffsetMinutes(prayer, next)
                            offsets = offsets + (prayer to next)
                            PrayerAlarmScheduler.scheduleForToday(context)
                        },
                    ) { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.prayer_offset_plus)) }
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(stringResource(R.string.notif_settings_per_prayer), style = MaterialTheme.typography.titleMedium)
            PrayerAlarmScheduler.prayers.forEach { prayer ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(PrayerLabels.label(context, prayer), modifier = Modifier.weight(1f))
                    Switch(
                        checked = perPrayer[prayer] ?: true,
                        onCheckedChange = { enabled ->
                            prefs.setNotificationEnabled(prayer, enabled)
                            perPrayer = perPrayer + (prayer to enabled)
                            PrayerAlarmScheduler.scheduleForToday(context)
                        },
                    )
                }
            }
        }
    }
}

/**
 * Peringatan pembatasan sistem: hal-hal yang membuat adzan diam tanpa jejak, dan tidak bisa
 * diperbaiki dari dalam aplikasi — user harus mengizinkannya sendiri.
 *
 * [severe] menandai yang benar-benar memblokir semuanya (izin notifikasi); sisanya hanya
 * menurunkan keandalan, jadi warnanya dibedakan supaya tingkat kegawatannya kebaca.
 */
@Composable
private fun RestrictionBanner(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    severe: Boolean = false,
) {
    val container = if (severe) {
        MaterialTheme.colorScheme.errorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val onContainer = if (severe) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    Surface(
        shape = MaterialTheme.shapes.large,
        color = container,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = onContainer)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = onContainer,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(onClick = onAction, modifier = Modifier.padding(top = 12.dp)) {
                Text(actionLabel)
            }
        }
    }
}
