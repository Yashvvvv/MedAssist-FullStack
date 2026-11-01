package com.medassist.app.ui.pharmacy

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PointOfSale
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
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.medassist.app.ui.account.AccountScreen
import com.medassist.app.ui.account.NotificationsScreen
import com.medassist.app.ui.account.SessionViewModel
import com.medassist.app.ui.pharmacy.more.MoreScreen
import com.medassist.app.ui.pharmacy.more.PlanScreen
import com.medassist.app.ui.pharmacy.more.ReportsScreen
import com.medassist.app.ui.pharmacy.more.SalesHistoryScreen
import com.medassist.app.ui.pharmacy.more.StaffScreen
import com.medassist.app.ui.pharmacy.orders.OrderDetailScreen
import com.medassist.app.ui.pharmacy.orders.OrdersScreen
import com.medassist.app.ui.pharmacy.pos.InvoiceScreen
import com.medassist.app.ui.pharmacy.pos.PosScreen
import com.medassist.app.ui.pharmacy.stock.ImportMedicinesScreen
import com.medassist.app.ui.pharmacy.stock.ReceiveStockScreen
import com.medassist.app.ui.pharmacy.stock.StockScreen

private enum class Tab(val route: String, val label: String, val icon: ImageVector, val selected: ImageVector) {
    Home("home", "Home", Icons.Outlined.Dashboard, Icons.Rounded.Dashboard),
    Sell("sell", "Sell", Icons.Outlined.PointOfSale, Icons.Rounded.PointOfSale),
    Orders("orders", "Orders", Icons.Outlined.ShoppingBag, Icons.Rounded.ShoppingBag),
    Stock("stock", "Stock", Icons.Outlined.Inventory2, Icons.Rounded.Inventory2),
    More("more", "More", Icons.Outlined.Menu, Icons.Rounded.Menu),
}

/** The pharmacy staff app: five tabs, everything else pushed on top of them. */
@Composable
fun PharmacyApp(sessionViewModel: SessionViewModel) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val onTab = Tab.entries.any { it.route == route }
    Scaffold(
        bottomBar = {
            if (onTab) {
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
        NavHost(nav, startDestination = Tab.Home.route, modifier = Modifier.padding(bottom = padding.calculateBottomPadding())) {
            composable(Tab.Home.route) {
                DashboardScreen(
                    onOpenOrders = { nav.switchTab(Tab.Orders.route) },
                    onOpenStock = { nav.switchTab(Tab.Stock.route) },
                    onSell = { nav.switchTab(Tab.Sell.route) },
                    onNotifications = { nav.navigate("notifications") },
                    onReports = { nav.navigate("reports") },
                )
            }
            composable(Tab.Sell.route) { PosScreen(onInvoice = { nav.navigate("invoice/$it") }) }
            composable(Tab.Orders.route) { OrdersScreen(onOpen = { nav.navigate("order/$it") }) }
            composable(Tab.Stock.route) {
                StockScreen(onReceive = { nav.navigate("receive") }, onImport = { nav.navigate("import") })
            }
            composable(Tab.More.route) {
                MoreScreen(
                    sessionViewModel = sessionViewModel,
                    onReports = { nav.navigate("reports") },
                    onSales = { nav.navigate("sales") },
                    onStaff = { nav.navigate("staff") },
                    onPlan = { nav.navigate("plan") },
                    onNotifications = { nav.navigate("notifications") },
                    onAccount = { nav.navigate("account") },
                )
            }
            composable("invoice/{id}", listOf(navArgument("id") { type = NavType.StringType })) {
                InvoiceScreen(onBack = nav::popBackStack)
            }
            composable("order/{id}", listOf(navArgument("id") { type = NavType.StringType })) {
                OrderDetailScreen(onBack = nav::popBackStack, onInvoice = { nav.navigate("invoice/$it") })
            }
            composable("receive") { ReceiveStockScreen(onBack = nav::popBackStack) }
            composable("import") { ImportMedicinesScreen(onBack = nav::popBackStack) }
            composable("reports") { ReportsScreen(onBack = nav::popBackStack, onUpgrade = { nav.navigate("plan") }) }
            composable("sales") { SalesHistoryScreen(onBack = nav::popBackStack, onOpen = { nav.navigate("invoice/$it") }) }
            composable("staff") { StaffScreen(onBack = nav::popBackStack, onUpgrade = { nav.navigate("plan") }) }
            composable("plan") { PlanScreen(onBack = nav::popBackStack) }
            composable("notifications") { NotificationsScreen(onBack = nav::popBackStack) }
            composable("account") { AccountScreen(sessionViewModel, onBack = nav::popBackStack) }
        }
    }
}

/** Standard bottom-tab navigation: one copy of each tab, state kept when switching back. */
fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
