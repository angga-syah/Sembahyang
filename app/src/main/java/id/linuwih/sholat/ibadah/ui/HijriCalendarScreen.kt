package id.linuwih.sholat.ibadah.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.linuwih.sholat.R
import id.linuwih.sholat.ibadah.data.HijriDate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private data class DayCell(val gregorianDay: Int, val hijri: HijriDate, val isToday: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HijriCalendarScreen(onBack: () -> Unit) {
    var monthOffset by remember { mutableIntStateOf(0) }
    val today = remember { HijriDate.today() }
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val weekdayLabels = listOf(
        stringResource(R.string.hijri_weekday_sun),
        stringResource(R.string.hijri_weekday_mon),
        stringResource(R.string.hijri_weekday_tue),
        stringResource(R.string.hijri_weekday_wed),
        stringResource(R.string.hijri_weekday_thu),
        stringResource(R.string.hijri_weekday_fri),
        stringResource(R.string.hijri_weekday_sat),
    )

    val monthCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            add(Calendar.MONTH, monthOffset)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }
    val weeks = remember(monthOffset) { buildMonthGrid(monthCal) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(monthFormatter.format(monthCal.time)) },
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
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
        ) {
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.hijri_today_label), style = MaterialTheme.typography.labelMedium)
                    Text(today.toString(), style = MaterialTheme.typography.headlineSmall)
                    Text(
                        stringResource(R.string.hijri_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                weekdayLabels.forEach { label ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            weeks.forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { cell ->
                        Box(
                            modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (cell != null) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .then(
                                            if (cell.isToday) {
                                                Modifier.background(MaterialTheme.colorScheme.primary)
                                            } else {
                                                Modifier
                                            },
                                        ),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(
                                        "${cell.gregorianDay}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = if (cell.isToday) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                    Text(
                                        "${cell.hijri.day}",
                                        style = MaterialTheme.typography.labelSmall,
                                        textAlign = TextAlign.Center,
                                        color = if (cell.isToday) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.secondary
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 8.dp))
            Text(
                stringResource(R.string.hijri_legend),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun buildMonthGrid(monthCal: Calendar): List<List<DayCell?>> {
    val firstDay = monthCal.clone() as Calendar
    firstDay.set(Calendar.DAY_OF_MONTH, 1)
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks = firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY // 0..6

    val today = Calendar.getInstance()
    val isSameMonth = today.get(Calendar.YEAR) == firstDay.get(Calendar.YEAR) &&
        today.get(Calendar.MONTH) == firstDay.get(Calendar.MONTH)

    val cells = mutableListOf<DayCell?>()
    repeat(leadingBlanks) { cells += null }
    for (day in 1..daysInMonth) {
        val dayCal = firstDay.clone() as Calendar
        dayCal.set(Calendar.DAY_OF_MONTH, day)
        cells += DayCell(
            gregorianDay = day,
            hijri = HijriDate.from(dayCal),
            isToday = isSameMonth && today.get(Calendar.DAY_OF_MONTH) == day,
        )
    }
    while (cells.size % 7 != 0) cells += null

    return cells.chunked(7)
}
