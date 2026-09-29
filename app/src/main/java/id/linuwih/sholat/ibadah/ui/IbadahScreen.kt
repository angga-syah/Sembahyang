package id.linuwih.sholat.ibadah.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import id.linuwih.sholat.R
import id.linuwih.sholat.prayer.ui.NotificationSettingsScreen
import id.linuwih.sholat.quran.ui.AboutScreen
import id.linuwih.sholat.quran.ui.TajweedGuideScreen

private data class IbadahMenuItem(val route: String, val titleRes: Int, val subtitleRes: Int, val icon: ImageVector)

// Dipisah dari SETTINGS_ITEMS SENGAJA: daftar ini tumbuh dari 7 jadi 11 baris seiring fitur
// baru ditambahkan (amal harian, zakat, waris, panduan tajwid), dan "Setelan Notifikasi" /
// "Tentang Aplikasi" -- yang bukan ibadah/fitur, melainkan pengaturan aplikasi -- jadi
// tenggelam di tengah daftar tanpa pembeda visual. Sekarang keduanya ditampilkan sebagai
// dua grup terpisah dengan judul seksi masing-masing, bukan satu daftar rata.
private val IBADAH_ITEMS = listOf(
    IbadahMenuItem("amalharian", R.string.ibadah_menu_amalharian_title, R.string.ibadah_menu_amalharian_subtitle, Icons.Filled.Checklist),
    IbadahMenuItem("panduan", R.string.ibadah_menu_guide_title, R.string.ibadah_menu_guide_subtitle, Icons.Filled.Schedule),
    IbadahMenuItem("tasbih", R.string.ibadah_menu_tasbih_title, R.string.ibadah_menu_tasbih_subtitle, Icons.Filled.Radar),
    IbadahMenuItem("dzikir", R.string.ibadah_menu_dzikir_title, R.string.ibadah_menu_dzikir_subtitle, Icons.AutoMirrored.Filled.MenuBook),
    IbadahMenuItem("puasa", R.string.ibadah_menu_puasa_title, R.string.ibadah_menu_puasa_subtitle, Icons.Filled.FavoriteBorder),
    IbadahMenuItem("zakat", R.string.ibadah_menu_zakat_title, R.string.ibadah_menu_zakat_subtitle, Icons.Filled.Savings),
    IbadahMenuItem("waris", R.string.ibadah_menu_waris_title, R.string.ibadah_menu_waris_subtitle, Icons.Filled.AccountBalance),
    IbadahMenuItem("tajwid", R.string.ibadah_menu_tajwid_title, R.string.ibadah_menu_tajwid_subtitle, Icons.Filled.School),
    IbadahMenuItem("kalender", R.string.ibadah_menu_kalender_title, R.string.ibadah_menu_kalender_subtitle, Icons.Filled.CalendarMonth),
)

private val SETTINGS_ITEMS = listOf(
    IbadahMenuItem("notif", R.string.ibadah_menu_notif_title, R.string.ibadah_menu_notif_subtitle, Icons.Filled.Notifications),
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
                .verticalScroll(rememberScrollState()),
        ) {
            SectionHeader(stringResource(R.string.ibadah_section_fitur))
            IBADAH_ITEMS.forEach { item -> IbadahMenuRow(item, onOpen) }

            SectionHeader(stringResource(R.string.ibadah_section_pengaturan))
            SETTINGS_ITEMS.forEach { item -> IbadahMenuRow(item, onOpen) }

            // Tanpa ini baris terakhir mepet ke tepi layar / gesture nav bar di HP tanpa
            // tombol fisik -- sepele tapi mengganggu terutama untuk item paling akhir.
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}

@Composable
private fun IbadahMenuRow(item: IbadahMenuItem, onOpen: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen(item.route) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Ikon diberi wadah bulat lembut supaya tiap baris punya titik fokus,
        // bukan sekadar deretan teks yang semuanya terlihat sama berat.
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(stringResource(item.titleRes), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(item.subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
    )
}
