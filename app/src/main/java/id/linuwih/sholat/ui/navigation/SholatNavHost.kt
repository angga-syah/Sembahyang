package id.linuwih.sholat.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import id.linuwih.sholat.ibadah.ui.IbadahScreen
import id.linuwih.sholat.prayer.ui.PrayerScreen
import id.linuwih.sholat.qibla.ui.QiblaScreen
import id.linuwih.sholat.quran.ui.QuranScreen
import id.linuwih.sholat.update.UpdateDialogHost
import id.linuwih.sholat.util.NetworkActivity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.linuwih.sholat.R

@Composable
fun SholatNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination?.route

                Screen.bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.labelRes)) },
                        label = { Text(stringResource(screen.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(
                navController = navController,
                startDestination = Screen.Prayer.route,
            ) {
                composable(Screen.Prayer.route) { PrayerScreen() }
                composable(Screen.Qibla.route) { QiblaScreen() }
                composable(Screen.Quran.route) { QuranScreen() }
                composable(Screen.Ibadah.route) { IbadahScreen() }
            }

            // Tawaran pembaruan dari GitHub Releases — dicek sekali sehari saat aplikasi dibuka.
            UpdateDialogHost()

            // Penanda transparansi: aplikasi ini mengklaim hampir seluruhnya offline, jadi saat
            // benar-benar memakai internet user berhak melihatnya langsung — bukan sekadar
            // percaya pada tulisan di layar Tentang.
            val networkActive by NetworkActivity.isActive.collectAsState()
            AnimatedVisibility(
                visible = networkActive,
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Text(
                            stringResource(R.string.network_in_use),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }
}
