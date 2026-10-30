package com.medassist.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalPharmacy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medassist.app.core.auth.Session
import com.medassist.app.ui.account.SessionViewModel
import com.medassist.app.ui.auth.AuthNavHost
import com.medassist.app.ui.patient.PatientApp
import com.medassist.app.ui.pharmacy.PharmacyApp

/**
 * The top-level switch. The session decides the whole app: signed out → sign-in flow, pharmacy
 * staff → pharmacy app, everyone else → patient app. Changing session swaps the tree, so no
 * screen of one mode can survive into the other.
 */
@Composable
fun AppRoot(sessionViewModel: SessionViewModel) {
    val session by sessionViewModel.session.collectAsStateWithLifecycle()
    val mode = when (val s = session) {
        Session.Unknown -> "splash"
        Session.SignedOut -> "auth"
        is Session.SignedIn -> if (s.isStaff) "pharmacy:${s.tokens.orgId}" else "patient:${s.tokens.userId}"
    }
    AnimatedContent(targetState = mode, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "mode") { m ->
        when {
            m == "splash" -> Splash()
            m == "auth" -> AuthNavHost()
            m.startsWith("pharmacy") -> PharmacyApp(sessionViewModel)
            else -> PatientApp(sessionViewModel)
        }
    }
}

@Composable
private fun Splash() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(88.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.LocalPharmacy, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(48.dp))
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("MedAssist", style = MaterialTheme.typography.headlineMedium)
        }
    }
}
