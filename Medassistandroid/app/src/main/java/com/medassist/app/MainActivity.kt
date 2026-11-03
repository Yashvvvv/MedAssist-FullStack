package com.medassist.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.medassist.app.ui.AppRoot
import com.medassist.app.ui.account.SessionViewModel
import com.medassist.app.ui.theme.AppLook
import com.medassist.app.ui.theme.MedAssistTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val sessionViewModel: SessionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val theme by sessionViewModel.theme.collectAsStateWithLifecycle()
            // Sign-in and splash use the patient look; AppRoot switches staff to the Counter Pro look.
            MedAssistTheme(look = AppLook.Patient, mode = theme) { AppRoot(sessionViewModel) }
        }
    }
}
