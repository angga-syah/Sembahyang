package id.linuwih.sholat.ibadah.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.linuwih.sholat.R
import id.linuwih.sholat.ibadah.data.HijriDate
import id.linuwih.sholat.prayer.ui.NotificationSettingsScreen
import id.linuwih.sholat.quran.ui.AboutScreen
import id.linuwih.sholat.quran.ui.TajweedGuideScreen
import id.linuwih.sholat.ui.theme.GoldAccent
import id.linuwih.sholat.ui.theme.TealPrimary
import id.linuwih.sholat.ui.theme.TealPrimaryDark
import java.text.DateFormat
import java.util.Date

private data class IbadahMenuItem(val route: String, val titleRes: Int, val subtitleRes: Int, val icon: ImageVector)

/** Satu kelompok menu. [tone] memilih pasangan warna ikon supaya tiap kelompok mudah dikenali. */
private data class IbadahSection(val titleRes: Int, val tone: Tone, val items: List<IbadahMenuItem>)

private enum class Tone { PRIMARY, SECONDARY, TERTIARY }

// Dulu semua fitur berjejer di satu daftar panjang "Fitur" — ibadah harian, panduan, dan
// kalkulator bercampur, jadi sulit dipindai. Sekarang dikelompokkan menurut tujuan pemakaian,
// ditampilkan sebagai kisi kartu dua kolom; pengaturan aplikasi dipisah paling bawah sebagai
// daftar ringkas karena bukan fitur ibadah.
private val SECTIONS = listOf(
    IbadahSection(
        R.string.ibadah_section_harian,
        Tone.PRIMARY,
        listOf(
            IbadahMenuItem("amalharian", R.string.ibadah_menu_amalharian_title, R.string.ibadah_menu_amalharian_subtitle, Icons.Filled.TaskAlt),
            IbadahMenuItem("dzikir", R.string.ibadah_menu_dzikir_title, R.string.ibadah_menu_dzikir_subtitle, Icons.Filled.AutoStories),
            IbadahMenuItem("tasbih", R.string.ibadah_menu_tasbih_title, R.string.ibadah_menu_tasbih_subtitle, Icons.Filled.TouchApp),
            IbadahMenuItem("puasa", R.string.ibadah_menu_puasa_title, R.string.ibadah_menu_puasa_subtitle, Icons.Filled.NightsStay),
        ),
    ),
    IbadahSection(
        R.string.ibadah_section_panduan,
        Tone.SECONDARY,
        listOf(
            IbadahMenuItem("panduan", R.string.ibadah_menu_guide_title, R.string.ibadah_menu_guide_subtitle, Icons.Filled.Mosque),
            IbadahMenuItem("tajwid", R.string.ibadah_menu_tajwid_title, R.string.ibadah_menu_tajwid_subtitle, Icons.AutoMirrored.Filled.MenuBook),
            IbadahMenuItem("kalender", R.string.ibadah_menu_kalender_title, R.string.ibadah_menu_kalender_subtitle, Icons.Filled.CalendarMonth),
        ),
    ),
    IbadahSection(
        R.string.ibadah_section_hitung,
        Tone.TERTIARY,
        listOf(
            IbadahMenuItem("zakat", R.string.ibadah_menu_zakat_title, R.string.ibadah_menu_zakat_subtitle, Icons.Filled.Savings),
            IbadahMenuItem("waris", R.string.ibadah_menu_waris_title, R.string.ibadah_menu_waris_subtitle, Icons.Filled.AccountBalance),
        ),
    ),
)

private val SETTINGS_ITEMS = listOf(
    IbadahMenuItem("notif", R.string.ibadah_menu_notif_title, R.string.ibadah_menu_notif_subtitle, Icons.Filled.NotificationsActive),
    IbadahMenuItem("about", R.string.ibadah_menu_about_title, R.string.ibadah_menu_about_subtitle, Icons.Filled.Info),
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun IbadahScreen(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "menu", modifier = modifier) {
        composable("menu") {
            IbadahMenuScreen(onOpen = { route -> navController.navigate(route) })
        }
        composable("amalharian") { AmalHarianScreen(onBack = { navController.popBackStack() }) }
        composable("panduan") { SholatGuideScreen(onBack = { navController.popBackStack() }) }
        composable("tasbih") { TasbihScreen(onBack = { navController.popBackStack() }) }
        composable("kalender") { HijriCalendarScreen(onBack = { navController.popBackStack() }) }
        composable("dzikir") { DzikirListScreen(onBack = { navController.popBackStack() }) }
        composable("puasa") { PuasaTrackerScreen(onBack = { navController.popBackStack() }) }
        composable("zakat") { ZakatScreen(onBack = { navController.popBackStack() }) }
        composable("waris") { WarisScreen(onBack = { navController.popBackStack() }) }
        composable("tajwid") { TajweedGuideScreen(onBack = { navController.popBackStack() }) }
        composable("notif") { NotificationSettingsScreen(onBack = { navController.popBackStack() }) }
        composable("about") { AboutScreen(onBack = { navController.popBackStack() }) }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun IbadahMenuScreen(onOpen: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.ibadah_title)) }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            TodayCard(onClick = { onOpen("kalender") })

            SECTIONS.forEach { section ->
                SectionHeader(stringResource(section.titleRes))
                section.items.chunked(2).forEach { pair ->
                    // IntrinsicSize.Min + fillMaxHeight: dua kartu sebaris selalu sama tinggi
                    // walau judul/subjudulnya beda panjang.
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        pair.forEach { item ->
                            MenuTile(item, section.tone, onOpen, Modifier.weight(1f).fillMaxHeight())
                        }
                        // Jumlah ganjil: sisakan ruang kosong supaya kartu terakhir tetap
                        // selebar kartu lain, bukan melebar memenuhi baris.
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }

            SectionHeader(stringResource(R.string.ibadah_section_pengaturan))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    SETTINGS_ITEMS.forEachIndexed { index, item ->
                        SettingsRow(item, onOpen)
                        if (index < SETTINGS_ITEMS.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 64.dp),
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant,
                            )
                        }
                    }
                }
            }

            // Ruang napas di atas bar navigasi bawah / gesture bar.
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Kartu pembuka: tanggal Hijriyah & Masehi hari ini; diketuk membuka kalender. */
@Composable
private fun TodayCard(onClick: () -> Unit) {
    val hijri = remember { HijriDate.today() }
    val gregorian = remember { DateFormat.getDateInstance(DateFormat.FULL).format(Date()) }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .background(Brush.linearGradient(listOf(TealPrimary, TealPrimaryDark)))
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            // Ruang kanan untuk ikon kalender, supaya tanggal panjang tidak menimpanya.
            Column(modifier = Modifier.padding(end = 64.dp)) {
                Text(
                    stringResource(R.string.ibadah_today_label).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.75f),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    hijri.toString(),
                    style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif),
                    color = GoldAccent,
                )
                Text(
                    gregorian,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.size(64.dp).align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 4.dp, top = 20.dp, bottom = 10.dp),
    )
}

@Composable
private fun MenuTile(item: IbadahMenuItem, tone: Tone, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (tone) {
        Tone.PRIMARY -> scheme.primaryContainer to scheme.onPrimaryContainer
        Tone.SECONDARY -> scheme.secondaryContainer to scheme.onSecondaryContainer
        Tone.TERTIARY -> scheme.tertiaryContainer to scheme.onTertiaryContainer
    }
    Surface(
        onClick = { onOpen(item.route) },
        shape = RoundedCornerShape(20.dp),
        color = scheme.surfaceContainerLow,
        border = BorderStroke(0.5.dp, scheme.outlineVariant),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.heightIn(min = 132.dp).padding(14.dp)) {
            Surface(shape = CircleShape, color = container, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(item.icon, contentDescription = null, tint = content, modifier = Modifier.size(22.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(item.titleRes),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(item.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun SettingsRow(item: IbadahMenuItem, onOpen: (String) -> Unit) {
    Surface(onClick = { onOpen(item.route) }, color = Color.Transparent) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(26.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(item.titleRes), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(item.subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
