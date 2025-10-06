package com.example.medassist_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.medassist_android.presentation.auth.AuthViewModel
import com.example.medassist_android.presentation.navigation.MedAssistNavigation
import com.example.medassist_android.presentation.navigation.Screen
import com.example.medassist_android.ui.theme.MedAssistAndroidTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MedAssistAndroidTheme {
                val navController = rememberNavController()
                val authViewModel: AuthViewModel = hiltViewModel()
                val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MedAssistNavigation(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding),
                        startDestination = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                    )
                }
            }
        }
    }
}
