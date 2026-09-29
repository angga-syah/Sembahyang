package id.linuwih.sholat.quran.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import id.linuwih.sholat.R
import id.linuwih.sholat.quran.audio.Reciter
import id.linuwih.sholat.ui.components.CompactSelector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDownloadScreen(onBack: () -> Unit, viewModel: AudioDownloadViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.audio_manager_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            ReciterDropdown(
                selected = state.reciter,
                onSelected = viewModel::setReciter,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ScopeChip(R.string.audio_scope_surah, DownloadScope.SURAH, state.scope, viewModel::setScope)
                ScopeChip(R.string.audio_scope_juz, DownloadScope.JUZ, state.scope, viewModel::setScope)
                ScopeChip(R.string.audio_scope_all, DownloadScope.ALL, state.scope, viewModel::setScope)
            }

            Text(
                stringResource(R.string.audio_storage_used, formatSize(state.storageUsedBytes)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            state.message?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (state.runningTargetId != null) {
                // Tanpa Card: progres ini status sementara, bukan kartu yang perlu menonjol.
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.audio_downloading, state.progressDone, state.progressTotal),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = viewModel::cancelDownload) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                    LinearProgressIndicator(
                        progress = {
                            if (state.progressTotal == 0) 0f
                            else state.progressDone.toFloat() / state.progressTotal
                        },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (state.loadingTargets) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(state.targets, key = { it.id }) { target ->
                        TargetRow(
                            target = target,
                            busy = state.runningTargetId != null,
                            onDownload = { viewModel.startDownload(target.id) },
                            onDelete = { viewModel.deleteTarget(target.id) },
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 16.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeChip(
    labelRes: Int,
    scope: DownloadScope,
    current: DownloadScope,
    onSelect: (DownloadScope) -> Unit,
) {
    FilterChip(
        selected = scope == current,
        onClick = { onSelect(scope) },
        label = { Text(stringResource(labelRes)) },
    )
}

@Composable
private fun TargetRow(
    target: DownloadTarget,
    busy: Boolean,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(target.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                if (target.isPartial) {
                    stringResource(R.string.audio_target_partial, target.downloadedAyahs, target.totalAyahs)
                } else {
                    target.subtitle
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (target.isComplete) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = stringResource(R.string.audio_state_downloaded),
                tint = MaterialTheme.colorScheme.primary,
            )
            IconButton(onClick = onDelete, enabled = !busy) {
                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_audio))
            }
        } else {
            IconButton(onClick = onDownload, enabled = !busy) {
                Icon(Icons.Filled.Download, contentDescription = stringResource(R.string.action_download_audio))
            }
            if (target.isPartial) {
                IconButton(onClick = onDelete, enabled = !busy) {
                    Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete_audio))
                }
            }
        }
    }
}

@Composable
private fun ReciterDropdown(selected: Reciter, onSelected: (Reciter) -> Unit, modifier: Modifier = Modifier) {
    CompactSelector(
        label = stringResource(R.string.quran_reciter_label),
        selected = selected,
        options = Reciter.entries,
        optionLabel = { it.displayName },
        onSelected = onSelected,
        modifier = modifier,
    )
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.1f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.0f KB".format(bytes / 1_000.0)
    else -> "$bytes B"
}
