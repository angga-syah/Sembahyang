package id.linuwih.sholat.ibadah.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import id.linuwih.sholat.ibadah.data.FastingType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
private fun fastingTypeLabel(type: FastingType): String = stringResource(
    when (type) {
        FastingType.SENIN_KAMIS -> R.string.fasting_type_senin_kamis
        FastingType.AYYAMUL_BIDH -> R.string.fasting_type_ayyamul_bidh
        FastingType.DAUD -> R.string.fasting_type_daud
        FastingType.SYAWAL -> R.string.fasting_type_syawal
        FastingType.ARAFAH -> R.string.fasting_type_arafah
        FastingType.LAINNYA -> R.string.fasting_type_lainnya
    },
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuasaTrackerScreen(onBack: () -> Unit) {
    val viewModel: PuasaViewModel = viewModel()
    val logs by viewModel.logs.collectAsState()
    val today = remember { viewModel.todayEpochDay }
    val todayEntry = logs.find { it.epochDay == today }
    val suggestion = remember { viewModel.suggestionForToday() }
    val monthCount = remember(logs) { viewModel.countThisMonth(logs) }
    val dateFormatter = remember { SimpleDateFormat("EEE, d MMMM yyyy", Locale.getDefault()) }

    var typeMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.puasa_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(dateFormatter.format(Calendar.getInstance().time), style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.puasa_month_count_format, monthCount),
                        style = MaterialTheme.typography.titleMedium,
                    )

                    suggestion?.let {
                        Spacer(Modifier.height(8.dp))
                        AssistChip(
                            onClick = { viewModel.markToday(it) },
                            label = { Text(stringResource(R.string.puasa_sunnah_today_format, fastingTypeLabel(it))) },
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                    if (todayEntry != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(
                                    R.string.puasa_marked_today_format,
                                    fastingTypeLabel(FastingType.valueOf(todayEntry.type)),
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            IconButton(onClick = { viewModel.unmarkToday() }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_cancel))
                            }
                        }
                    } else {
                        androidx.compose.material3.ExposedDropdownMenuBox(
                            expanded = typeMenuExpanded,
                            onExpandedChange = { typeMenuExpanded = it },
                        ) {
                            Button(
                                onClick = { typeMenuExpanded = true },
                                modifier = Modifier.menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable),
                            ) { Text(stringResource(R.string.puasa_mark_today)) }
                            DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = { typeMenuExpanded = false }) {
                                FastingType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(fastingTypeLabel(type)) },
                                        onClick = {
                                            viewModel.markToday(type)
                                            typeMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                stringResource(R.string.puasa_history),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp),
            )

            if (logs.isEmpty()) {
                id.linuwih.sholat.quran.ui.EmptyState(
                    icon = Icons.Filled.Close,
                    message = stringResource(R.string.puasa_history_empty),
                )
            }

            LazyColumn {
                items(logs) { log ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(viewModel.formatEpochDay(log.epochDay), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            fastingTypeLabel(FastingType.valueOf(log.type)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
