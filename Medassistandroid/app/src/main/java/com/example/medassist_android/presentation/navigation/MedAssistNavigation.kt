package com.example.medassist_android.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.medassist_android.presentation.auth.AuthViewModel
import com.example.medassist_android.presentation.auth.LoginScreen
import com.example.medassist_android.presentation.auth.RegisterScreen
import com.example.medassist_android.presentation.auth.ForgotPasswordScreen
import com.example.medassist_android.presentation.home.HomeScreen
import com.example.medassist_android.presentation.medicine.MedicineDetailScreen
import com.example.medassist_android.presentation.medicine.MedicineSearchScreen
import com.example.medassist_android.presentation.medicine.CameraScreen
import com.example.medassist_android.presentation.pharmacy.PharmacyListScreen
import com.example.medassist_android.presentation.pharmacy.PharmacyDetailScreen
import com.example.medassist_android.presentation.pharmacy.PharmacyMapScreen
import com.example.medassist_android.presentation.profile.ProfileScreen
import com.example.medassist_android.presentation.profile.EditProfileScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object Home : Screen("home")
    object MedicineSearch : Screen("medicine_search")
    object MedicineDetail : Screen("medicine_detail/{medicineId}") {
        fun createRoute(medicineId: Long) = "medicine_detail/$medicineId"
    }
    object Camera : Screen("camera")
    object PharmacyList : Screen("pharmacy_list")
    object PharmacyDetail : Screen("pharmacy_detail/{pharmacyId}") {
        fun createRoute(pharmacyId: Long) = "pharmacy_detail/$pharmacyId"
    }
    object PharmacyMap : Screen("pharmacy_map")
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
}

@Composable
fun MedAssistNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Home.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            LoginScreen(
                authViewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        composable(Screen.Register.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            RegisterScreen(
                authViewModel = authViewModel,
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToMedicineSearch = {
                    navController.navigate(Screen.MedicineSearch.route)
                },
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                },
                onNavigateToPharmacyList = {
                    navController.navigate(Screen.PharmacyList.route)
                },
                onNavigateToPharmacyMap = {
                    navController.navigate(Screen.PharmacyMap.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToMedicineDetail = { medicineId ->
                    navController.navigate(Screen.MedicineDetail.createRoute(medicineId))
                }
            )
        }

        composable(Screen.MedicineSearch.route) {
            MedicineSearchScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { medicineId ->
                    navController.navigate(Screen.MedicineDetail.createRoute(medicineId))
                },
                onNavigateToCamera = {
                    navController.navigate(Screen.Camera.route)
                }
            )
        }

        composable(Screen.MedicineDetail.route) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("medicineId")?.toLongOrNull() ?: 0L
            MedicineDetailScreen(
                medicineId = medicineId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToMedicineDetail = { medicineId ->
                    navController.navigate(Screen.MedicineDetail.createRoute(medicineId))
                }
            )
        }

        composable(Screen.PharmacyList.route) {
            PharmacyListScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { pharmacyId ->
                    navController.navigate(Screen.PharmacyDetail.createRoute(pharmacyId))
                },
                onNavigateToMap = {
                    navController.navigate(Screen.PharmacyMap.route)
                }
            )
        }

        composable(Screen.PharmacyDetail.route) { backStackEntry ->
            val pharmacyId = backStackEntry.arguments?.getString("pharmacyId")?.toLongOrNull() ?: 0L
            PharmacyDetailScreen(
                pharmacyId = pharmacyId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.PharmacyMap.route) {
            PharmacyMapScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { pharmacyId ->
                    navController.navigate(Screen.PharmacyDetail.createRoute(pharmacyId))
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onNavigateToEditProfile = {
                    navController.navigate(Screen.EditProfile.route)
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            ForgotPasswordScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
