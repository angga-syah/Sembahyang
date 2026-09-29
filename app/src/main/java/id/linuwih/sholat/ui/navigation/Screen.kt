package id.linuwih.sholat.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.ui.graphics.vector.ImageVector
import id.linuwih.sholat.R

/**
 * Empat tab utama: Jadwal sholat, Kiblat, Al-Qur'an, dan Ibadah (tasbih, dzikir/doa,
 * kalender Hijriyah, tracker puasa sunnah). Semua bekerja 100% offline (lihat plan doc,
 * bagian "Ketergantungan server & pihak ketiga").
 */
sealed class Screen(val route: String, val labelRes: Int, val icon: ImageVector) {
    data object Prayer : Screen("prayer", R.string.tab_prayer, Icons.Filled.Schedule)
    data object Qibla : Screen("qibla", R.string.tab_qibla, Icons.Filled.Explore)
    data object Quran : Screen("quran", R.string.tab_quran, Icons.AutoMirrored.Filled.MenuBook)
    data object Ibadah : Screen("ibadah", R.string.tab_ibadah, Icons.Filled.VolunteerActivism)

    companion object {
        val bottomNavItems = listOf(Prayer, Qibla, Quran, Ibadah)
    }
}
