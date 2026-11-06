package id.linuwih.sholat.update

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import id.linuwih.sholat.R
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val info: UpdateInfo) : UpdateState
    data class Downloading(val info: UpdateInfo, val progress: Float) : UpdateState
    data class Ready(val info: UpdateInfo, val apk: File) : UpdateState
    /** APK sudah terunduh, tapi Android belum mengizinkan aplikasi ini memasang aplikasi. */
    data class NeedsPermission(val info: UpdateInfo, val apk: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

/**
 * Satu instance untuk seluruh Activity (dibuat di level navigasi), supaya dialog otomatis dan
 * bagian "Pembaruan" di layar Tentang berbagi status yang sama.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = UpdatePrefs(application)
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    private val _autoCheck = MutableStateFlow(prefs.autoCheck)
    val autoCheck: StateFlow<Boolean> = _autoCheck.asStateFlow()

    /** true = dialog pembaruan sedang ditampilkan di atas layar mana pun. */
    private val _dialogVisible = MutableStateFlow(false)
    val dialogVisible: StateFlow<Boolean> = _dialogVisible.asStateFlow()

    private var job: Job? = null

    val installedVersion: String = AppUpdater.installedVersion(application)

    fun setAutoCheck(value: Boolean) {
        prefs.autoCheck = value
        _autoCheck.value = value
    }

    /** Dipanggil saat aplikasi dibuka: paling sering sekali sehari, diam kalau gagal/offline. */
    fun autoCheckIfDue() {
        if (!prefs.autoCheck || job?.isActive == true) return
        val today = java.time.LocalDate.now().toEpochDay()
        if (prefs.lastCheckDay == today) return
        job = viewModelScope.launch {
            // Gagal (offline / jaringan belum siap saat app baru dibuka) tidak dihitung sebagai
            // "sudah dicek hari ini" — dicoba lagi saat app dibuka berikutnya.
            val result = runCatching { AppUpdater.checkLatest(getApplication()) }
            if (result.isFailure) return@launch
            prefs.lastCheckDay = today
            val info = result.getOrNull()
            if (info != null && info.versionName != prefs.skippedVersion) {
                _state.value = UpdateState.Available(info)
                _dialogVisible.value = true
            }
        }
    }

    fun checkNow() {
        if (job?.isActive == true) return
        _state.value = UpdateState.Checking
        job = viewModelScope.launch {
            _state.value = runCatching { AppUpdater.checkLatest(getApplication()) }.fold(
                onSuccess = { info -> if (info == null) UpdateState.UpToDate else UpdateState.Available(info) },
                onFailure = { UpdateState.Failed(getApplication<Application>().getString(R.string.update_error_check)) },
            )
            prefs.lastCheckDay = java.time.LocalDate.now().toEpochDay()
            if (_state.value is UpdateState.Available) _dialogVisible.value = true
        }
    }

    fun startDownload() {
        val info = when (val s = _state.value) {
            is UpdateState.Available -> s.info
            else -> return
        }
        _dialogVisible.value = true
        job = viewModelScope.launch {
            _state.value = UpdateState.Downloading(info, 0f)
            val apk = runCatching {
                AppUpdater.download(getApplication(), info) { p -> _state.value = UpdateState.Downloading(info, p) }
            }.getOrElse {
                _state.value = UpdateState.Failed(getApplication<Application>().getString(R.string.update_error_download))
                return@launch
            }
            _state.value = UpdateState.Ready(info, apk)
            install()
        }
    }

    fun install() {
        val (info, apk) = when (val s = _state.value) {
            is UpdateState.Ready -> s.info to s.apk
            is UpdateState.NeedsPermission -> s.info to s.apk
            else -> return
        }
        val context = getApplication<Application>()
        if (!AppUpdater.canInstall(context)) {
            _state.value = UpdateState.NeedsPermission(info, apk)
            AppUpdater.openInstallPermissionSettings(context)
            return
        }
        _state.value = UpdateState.Ready(info, apk)
        if (!AppUpdater.install(context, apk)) {
            _state.value = UpdateState.Failed(context.getString(R.string.update_error_install))
        }
    }

    /** Kembali dari layar izin: kalau izin sudah diberikan, langsung lanjut memasang. */
    fun onResume() {
        val s = _state.value
        if (s is UpdateState.NeedsPermission && AppUpdater.canInstall(getApplication())) install()
    }

    fun later() {
        (_state.value as? UpdateState.Available)?.let { prefs.skippedVersion = it.info.versionName }
        dismiss()
    }

    fun dismiss() {
        _dialogVisible.value = false
        if (_state.value !is UpdateState.Downloading) {
            job?.cancel()
            _state.value = UpdateState.Idle
        }
    }
}

/** Dialog pembaruan yang muncul di atas layar mana pun. Dipasang sekali di navigasi utama. */
@Composable
fun UpdateDialogHost(viewModel: UpdateViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)) {
    LaunchedEffect(Unit) { viewModel.autoCheckIfDue() }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.onResume() }
    }

    val visible by viewModel.dialogVisible.collectAsState()
    val state by viewModel.state.collectAsState()
    if (!visible) return

    when (val s = state) {
        is UpdateState.Available -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_available_title, s.info.versionName)) },
            text = {
                Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.update_available_body, viewModel.installedVersion, s.info.versionName))
                    if (s.info.notes.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(s.info.notes, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { Button(onClick = viewModel::startDownload) { Text(stringResource(R.string.update_action_update)) } },
            dismissButton = { TextButton(onClick = viewModel::later) { Text(stringResource(R.string.update_action_later)) } },
        )
        is UpdateState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.update_downloading, s.info.versionName)) },
            text = { LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {},
        )
        is UpdateState.NeedsPermission -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_permission_title)) },
            text = { Text(stringResource(R.string.update_permission_body)) },
            confirmButton = { Button(onClick = viewModel::install) { Text(stringResource(R.string.update_action_install)) } },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_action_later)) } },
        )
        is UpdateState.Ready -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_ready_title, s.info.versionName)) },
            text = { Text(stringResource(R.string.update_ready_body)) },
            confirmButton = { Button(onClick = viewModel::install) { Text(stringResource(R.string.update_action_install)) } },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_action_later)) } },
        )
        is UpdateState.Failed -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_failed_title)) },
            text = { Text(s.message) },
            confirmButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_action_close)) } },
        )
        else -> Unit
    }
}

/** Bagian "Pembaruan aplikasi" di layar Tentang: versi terpasang, cek manual, cek otomatis. */
@Composable
fun UpdateSection(viewModel: UpdateViewModel = viewModel(LocalContext.current as androidx.activity.ComponentActivity)) {
    val state by viewModel.state.collectAsState()
    val autoCheck by viewModel.autoCheck.collectAsState()
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(stringResource(R.string.update_section_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.update_installed_version, viewModel.installedVersion),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = viewModel::checkNow, enabled = state !is UpdateState.Checking && state !is UpdateState.Downloading) {
                Text(stringResource(R.string.update_check_now))
            }
            Spacer(Modifier.padding(start = 12.dp))
            Text(
                when (state) {
                    UpdateState.Checking -> stringResource(R.string.update_checking)
                    UpdateState.UpToDate -> stringResource(R.string.update_up_to_date)
                    is UpdateState.Failed -> (state as UpdateState.Failed).message
                    is UpdateState.Downloading -> stringResource(R.string.update_downloading_short)
                    else -> ""
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Text(
                stringResource(R.string.update_auto_check),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = autoCheck, onCheckedChange = viewModel::setAutoCheck)
        }
        Text(
            stringResource(R.string.update_auto_check_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
