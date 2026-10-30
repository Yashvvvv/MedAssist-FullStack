package com.medassist.app.ui.pharmacy.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medassist.app.core.auth.Session
import com.medassist.app.core.model.Role
import com.medassist.app.ui.account.SessionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    sessionViewModel: SessionViewModel,
    onReports: () -> Unit,
    onSales: () -> Unit,
    onStaff: () -> Unit,
    onPlan: () -> Unit,
    onNotifications: () -> Unit,
    onAccount: () -> Unit,
) {
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val role = (session as? Session.SignedIn)?.role
    val tokens = (session as? Session.SignedIn)?.tokens
    Scaffold(topBar = { TopAppBar(title = { Text("More") }) }) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            tokens?.let {
                ListItem(
                    headlineContent = { Text(it.fullName, style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text("${it.orgName ?: ""} · ${role?.name?.lowercase()?.replaceFirstChar { c -> c.uppercase() } ?: ""}") },
                    leadingContent = { Icon(Icons.Outlined.AccountCircle, null, Modifier.padding(4.dp)) },
                    modifier = Modifier,
                )
            }
            Entry(Icons.AutoMirrored.Outlined.ReceiptLong, "Sales history", "Every bill, searchable", onSales)
            if (role?.atLeast(Role.ADMIN) == true) {
                Entry(Icons.Outlined.BarChart, "Reports", "Sales, profit and GST", onReports)
                Entry(Icons.Outlined.Group, "Staff", "Invite your team and set roles", onStaff)
            }
            if (role == Role.OWNER) Entry(Icons.Outlined.WorkspacePremium, "Plan & billing", "Your MedAssist subscription", onPlan)
            Entry(Icons.Outlined.Notifications, "Notifications", "Orders, expiry alerts, updates", onNotifications)
            Entry(Icons.Outlined.AccountCircle, "Account & settings", "Profile, theme, switch pharmacy, log out", onAccount)
        }
    }
}

@Composable
private fun Entry(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
