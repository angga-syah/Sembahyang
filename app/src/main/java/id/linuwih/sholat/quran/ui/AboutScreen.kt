package id.linuwih.sholat.quran.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import id.linuwih.sholat.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        // Setiap seksi collapsed by default (cuma header+chevron yang tampak) — sebelumnya
        // semua detail ditumpuk terbuka sekaligus jadi satu dinding teks panjang yang bikin
        // orang malas baca. Sekarang orang cuma perlu baca 4 baris judul; detail lengkap
        // (yang tetap penting untuk transparansi) tersedia sekali ketuk kalau mau digali.
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp),
            )
            Text(
                stringResource(R.string.about_app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            id.linuwih.sholat.update.UpdateSection()

            val context = androidx.compose.ui.platform.LocalContext.current
            val feedbackSubject = stringResource(R.string.about_feedback_email_subject)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
                            data = android.net.Uri.parse("mailto:info@fyris.my.id")
                            putExtra(android.content.Intent.EXTRA_SUBJECT, feedbackSubject)
                        }
                        runCatching { context.startActivity(intent) }
                    }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(R.string.about_feedback_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "info@fyris.my.id",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            HorizontalDivider()

            ExpandableSection(icon = Icons.AutoMirrored.Filled.MenuBook, title = stringResource(R.string.about_section_sources)) {
                Text(
                    stringResource(R.string.about_sources_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                SourceEntry(stringResource(R.string.about_src_quran_text_title), stringResource(R.string.about_src_quran_text_detail))
                SourceEntry(stringResource(R.string.about_src_latin_id_title), stringResource(R.string.about_src_latin_id_detail))
                SourceEntry(stringResource(R.string.about_src_prayer_calc_title), stringResource(R.string.about_src_prayer_calc_detail))
                SourceEntry(stringResource(R.string.about_src_method_title), stringResource(R.string.about_src_method_detail))
                SourceEntry(stringResource(R.string.about_src_audio_title), stringResource(R.string.about_src_audio_detail))
                SourceEntry(stringResource(R.string.about_src_audio_license_title), stringResource(R.string.about_src_audio_license_detail))
                // everyayah.com/VerseByVerseQuran.com mensyaratkan tautan balik dari app yang
                // memakai berkas waktu (timing) mereka -- sebelumnya cuma disebut namanya di
                // teks, TIDAK ADA link aktif di mana pun, jadi syarat itu belum benar-benar
                // terpenuhi. Dua link (bukan cuma satu) karena disclaimer berkasnya ditandatangani
                // "(C) VerseByVerseQuran.com" tapi dihosting di domain everyayah.com -- tidak
                // jelas mana yang dimaksud "situs kami", jadi ditautkan keduanya.
                LinkRow(stringResource(R.string.about_src_everyayah_link_label), "https://everyayah.com")
                LinkRow(stringResource(R.string.about_src_versebyverse_link_label), "https://versebyversequran.com")
                SourceEntry(stringResource(R.string.about_src_adhan_title), stringResource(R.string.about_src_adhan_detail))
                SourceEntry(stringResource(R.string.about_src_dzikir_title), stringResource(R.string.about_src_dzikir_detail))
                SourceEntry(stringResource(R.string.about_src_font_title), stringResource(R.string.about_src_font_detail))
                SourceEntry(stringResource(R.string.about_src_kemenag_title), stringResource(R.string.about_src_kemenag_detail))
                SourceEntry(stringResource(R.string.about_src_tajweed_title), stringResource(R.string.about_src_tajweed_detail))
                SourceEntry(stringResource(R.string.about_src_tafsir_title), stringResource(R.string.about_src_tafsir_detail))
            }

            ExpandableSection(icon = Icons.Filled.CloudOff, title = stringResource(R.string.about_section_resilience)) {
                Text(
                    stringResource(R.string.about_resilience_detail),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            ExpandableSection(icon = Icons.Filled.Shield, title = stringResource(R.string.about_section_permissions)) {
                Text(
                    stringResource(R.string.about_permissions_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                SourceEntry(stringResource(R.string.about_perm_location_title), stringResource(R.string.about_perm_location_detail))
                SourceEntry(stringResource(R.string.about_perm_notification_title), stringResource(R.string.about_perm_notification_detail))
                SourceEntry(stringResource(R.string.about_perm_exact_alarm_title), stringResource(R.string.about_perm_exact_alarm_detail))
                SourceEntry(stringResource(R.string.about_perm_boot_title), stringResource(R.string.about_perm_boot_detail))
                SourceEntry(stringResource(R.string.about_perm_vibrate_title), stringResource(R.string.about_perm_vibrate_detail))
                SourceEntry(stringResource(R.string.about_perm_internet_title), stringResource(R.string.about_perm_internet_detail))
                SourceEntry(stringResource(R.string.about_perm_install_title), stringResource(R.string.about_perm_install_detail))
                SourceEntry(stringResource(R.string.about_perm_none_title), stringResource(R.string.about_perm_none_detail))
            }

            ExpandableSection(icon = Icons.Filled.BatteryChargingFull, title = stringResource(R.string.about_section_battery)) {
                Text(
                    stringResource(R.string.about_battery_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                SourceEntry(stringResource(R.string.about_battery_no_bg_title), stringResource(R.string.about_battery_no_bg_detail))
                SourceEntry(stringResource(R.string.about_battery_compass_title), stringResource(R.string.about_battery_compass_detail))
                SourceEntry(stringResource(R.string.about_battery_memory_title), stringResource(R.string.about_battery_memory_detail))
                SourceEntry(stringResource(R.string.about_battery_storage_title), stringResource(R.string.about_battery_storage_detail))
                SourceEntry(stringResource(R.string.about_battery_manufacturer_title), stringResource(R.string.about_battery_manufacturer_detail))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ExpandableSection(icon: ImageVector, title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(14.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Column(modifier = Modifier.padding(bottom = 20.dp), content = { content() })
        }
        HorizontalDivider()
    }
}

/**
 * Satu butir informasi: label pendek di atas, penjelasan 1-2 baris di bawahnya.
 *
 * Sebelumnya judulnya sendiri berupa kalimat panjang dengan ukuran bodyLarge, sehingga tiap butir
 * terlihat seperti dua paragraf bertumpuk dan mata sulit memindai mana judul mana penjelasan.
 * Sekarang judul dibuat pendek dan tegas, penjelasan lebih redup — perbedaannya bukan cuma
 * ukuran font, tapi juga bobot dan warna, supaya hirarkinya terbaca sekilas.
 */
/** Baris tautan yang benar-benar bisa diklik -- membuka browser lewat Intent.ACTION_VIEW.
 *  Beda dari SourceEntry yang cuma teks statis, ini yang memenuhi syarat "tautan balik" yang
 *  diminta beberapa sumber data (mis. everyayah.com/VerseByVerseQuran.com). */
@Composable
private fun LinkRow(label: String, url: String) {
    val context = LocalContext.current
    Text(
        label,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        textDecoration = TextDecoration.Underline,
        modifier = Modifier
            .padding(top = 6.dp)
            .clickable {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            },
    )
}

@Composable
private fun SourceEntry(title: String, detail: String) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
