package com.medassist.app.ui.patient

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medassist.app.ui.account.AccountScreen
import com.medassist.app.ui.account.NotificationsScreen
import com.medassist.app.ui.account.SessionViewModel
import com.medassist.app.ui.pharmacy.switchTab

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val selected: ImageVector) {
    Explore("explore", "Explore", Icons.Outlined.Explore, Icons.Rounded.Explore),
    Orders("my-orders", "Orders", Icons.Outlined.ShoppingBag, Icons.Rounded.ShoppingBag),
    Scan("scan", "Scan", Icons.Outlined.DocumentScanner, Icons.Rounded.DocumentScanner),
    Account("me", "Account", Icons.Outlined.AccountCircle, Icons.Rounded.AccountCircle),
}

/** The customer app: find a pharmacy, order, track, and AI help with medicines. */
@Composable
fun PatientApp(sessionViewModel: SessionViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    Scaffold(
        bottomBar = {
            if (Tab.entries.any { it.route == route }) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        val selected = route == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(if (selected) tab.selected else tab.icon, null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Tab.Explore.route, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable(Tab.Explore.route) {
                ExploreScreen(
                    onPharmacy = { nav.navigate("pharmacy/$it") },
                    onMedicine = { nav.navigate("medicine/$it") },
                    onNotifications = { nav.navigate("notifications") },
                )
            }
            composable(Tab.Orders.route) { MyOrdersScreen(onOpen = { nav.navigate("track/$it") }, onExplore = { nav.switchTab(Tab.Explore.route) }) }
            composable(Tab.Scan.route) { ScanScreen(onMedicine = { nav.navigate("medicine/$it") }) }
            composable(Tab.Account.route) { AccountScreen(sessionViewModel, onBack = null) }
            composable("pharmacy/{id}", listOf(navArgument("id") { type = NavType.StringType })) {
                PharmacyScreen(onBack = nav::popBackStack, onOrdered = { orderId ->
                    nav.navigate("track/$orderId") { popUpTo(Tab.Explore.route) }
                })
            }
            composable("medicine/{id}", listOf(navArgument("id") { type = NavType.StringType })) {
                MedicineScreen(onBack = nav::popBackStack, onPharmacy = { nav.navigate("pharmacy/$it") }, onMedicine = { nav.navigate("medicine/$it") })
            }
            composable("track/{id}", listOf(navArgument("id") { type = NavType.StringType })) { OrderTrackScreen(onBack = nav::popBackStack) }
            composable("notifications") { NotificationsScreen(onBack = nav::popBackStack) }
        }
    }
}
