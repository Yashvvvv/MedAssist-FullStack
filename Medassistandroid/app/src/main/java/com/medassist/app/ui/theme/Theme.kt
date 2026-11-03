package com.medassist.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.medassist.app.R
import com.medassist.app.data.ThemeMode

/**
 * Two looks on one theme system, picked by who is signed in:
 *  - [AppLook.Patient] "Clinical Calm": light-first teal, Plus Jakarta Sans, soft white cards. Calm and trustworthy.
 *  - [AppLook.Staff] "Counter Pro": dark-first slate with a green accent, Inter, hairline borders, dense rows.
 *    Built for a busy counter where the screen is glanced at between customers.
 */
enum class AppLook { Patient, Staff }

val LocalAppLook = staticCompositionLocalOf { AppLook.Patient }

// ---------- Fonts (bundled, Latin + ₹ subset, SIL OFL 1.1) ----------
private val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
)
private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)
private val JetBrainsMono = FontFamily(
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
    Font(R.font.jetbrainsmono_bold, FontWeight.Bold),
)

/** Tabular figures so columns of rupee amounts line up. Works with both bundled fonts. */
val Numeric = TextStyle(fontFeatureSettings = "tnum")

/** For codes: invoice and order numbers, batch numbers, SKUs. JetBrains Mono has no ₹ glyph: never use it for money. */
val Mono = TextStyle(fontFamily = JetBrainsMono, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)

// ---------- Patient · Clinical Calm ----------
private val Teal10 = Color(0xFF00201C)
private val Teal30 = Color(0xFF075E55)
private val Teal40 = Color(0xFF0B7A6F)
private val Teal80 = Color(0xFF7FD8C9)
private val Teal90 = Color(0xFFD9F1EB)

private val PatientLight = lightColorScheme(
    primary = Teal40,
    onPrimary = Color.White,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Color(0xFF4A635E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F4F0),
    onSecondaryContainer = Color(0xFF06201B),
    tertiary = Color(0xFF175CD3),
    tertiaryContainer = Color(0xFFDDEBFF),
    onTertiaryContainer = Color(0xFF001F4D),
    background = Color(0xFFF3F7F6),
    onBackground = Color(0xFF10201D),
    surface = Color(0xFFF3F7F6),
    onSurface = Color(0xFF10201D),
    surfaceTint = Color(0xFFF3F7F6),
    surfaceVariant = Color(0xFFE3EBE8),
    onSurfaceVariant = Color(0xFF55655F),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FBFA),
    surfaceContainer = Color(0xFFEDF3F1),
    surfaceContainerHigh = Color(0xFFE6EEEB),
    surfaceContainerHighest = Color(0xFFDFE8E5),
    outline = Color(0xFF7A8884),
    outlineVariant = Color(0xFFE1E9E6),
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFFE1DF),
    onErrorContainer = Color(0xFF410002),
)

private val PatientDark = darkColorScheme(
    primary = Teal80,
    onPrimary = Color(0xFF003731),
    primaryContainer = Teal30,
    onPrimaryContainer = Color(0xFFB8F0E5),
    secondary = Color(0xFFB1CCC5),
    onSecondary = Color(0xFF1C3530),
    secondaryContainer = Color(0xFF334B46),
    onSecondaryContainer = Color(0xFFCCE8E1),
    tertiary = Color(0xFFA6CCE0),
    tertiaryContainer = Color(0xFF264B5C),
    onTertiaryContainer = Color(0xFFC2E8FD),
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDEE4E1),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDEE4E1),
    surfaceTint = Color(0xFF0E1514),
    surfaceVariant = Color(0xFF3F4946),
    onSurfaceVariant = Color(0xFFBEC9C5),
    surfaceContainerLowest = Color(0xFF161E1D),
    surfaceContainerLow = Color(0xFF171D1C),
    surfaceContainer = Color(0xFF1B2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerHighest = Color(0xFF2F3635),
    outline = Color(0xFF899390),
    outlineVariant = Color(0xFF2A3331),
)

// ---------- Staff · Counter Pro ----------
private val StaffDark = darkColorScheme(
    primary = Color(0xFF3DDC97),
    onPrimary = Color(0xFF04170C),
    primaryContainer = Color(0xFF12392A),
    onPrimaryContainer = Color(0xFFA7F3CF),
    secondary = Color(0xFF58A6FF),
    onSecondary = Color(0xFF00213F),
    secondaryContainer = Color(0xFF16283D),
    onSecondaryContainer = Color(0xFFB6D7FF),
    tertiary = Color(0xFFF5B544),
    onTertiary = Color(0xFF2B1800),
    tertiaryContainer = Color(0xFF3A2A0B),
    onTertiaryContainer = Color(0xFFFFE3B5),
    background = Color(0xFF0A0E13),
    onBackground = Color(0xFFE6EDF3),
    surface = Color(0xFF0A0E13),
    onSurface = Color(0xFFE6EDF3),
    surfaceTint = Color(0xFF0A0E13),
    surfaceVariant = Color(0xFF1A2330),
    onSurfaceVariant = Color(0xFF8B98A5),
    surfaceContainerLowest = Color(0xFF0E141B),
    surfaceContainerLow = Color(0xFF0E141B),
    surfaceContainer = Color(0xFF121922),
    surfaceContainerHigh = Color(0xFF18212C),
    surfaceContainerHighest = Color(0xFF1F2A37),
    inverseSurface = Color(0xFFE6EDF3),
    inverseOnSurface = Color(0xFF0A0E13),
    outline = Color(0xFF2E3B4A),
    outlineVariant = Color(0xFF1A2330),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF2D0607),
    errorContainer = Color(0xFF3A1618),
    onErrorContainer = Color(0xFFFFDAD6),
)

private val StaffLight = lightColorScheme(
    primary = Color(0xFF0F8A5F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2F4E4),
    onPrimaryContainer = Color(0xFF05391F),
    secondary = Color(0xFF0969DA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDEBFF),
    onSecondaryContainer = Color(0xFF00214A),
    tertiary = Color(0xFF9A6700),
    tertiaryContainer = Color(0xFFFFF1CC),
    onTertiaryContainer = Color(0xFF2B1800),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF0D1117),
    surface = Color(0xFFF6F8FA),
    onSurface = Color(0xFF0D1117),
    surfaceTint = Color(0xFFF6F8FA),
    surfaceVariant = Color(0xFFEAEEF2),
    onSurfaceVariant = Color(0xFF57606A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEFF2F5),
    surfaceContainerHigh = Color(0xFFE8ECF0),
    surfaceContainerHighest = Color(0xFFDFE4E9),
    outline = Color(0xFFC4CCD4),
    outlineVariant = Color(0xFFE1E6EB),
    error = Color(0xFFCF222E),
    errorContainer = Color(0xFFFFEBE9),
    onErrorContainer = Color(0xFF410002),
)

/** Status colours that M3 doesn't have: success, warning, info. Error comes from the scheme. */
@Immutable
data class StatusColors(
    val success: Color,
    val onSuccessContainer: Color,
    val successContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val info: Color,
    val infoContainer: Color,
)

private val PatientLightStatus = StatusColors(
    success = Color(0xFF1E7B3A), successContainer = Color(0xFFD5F5DC), onSuccessContainer = Color(0xFF05601F),
    warning = Color(0xFF8A5A00), warningContainer = Color(0xFFFFE9C7), onWarningContainer = Color(0xFF5C3B00),
    info = Color(0xFF175CD3), infoContainer = Color(0xFFDDEBFF),
)
private val PatientDarkStatus = StatusColors(
    success = Color(0xFF8CD99E), successContainer = Color(0xFF00531F), onSuccessContainer = Color(0xFFD5F5DC),
    warning = Color(0xFFFFB955), warningContainer = Color(0xFF633F00), onWarningContainer = Color(0xFFFFE3B5),
    info = Color(0xFF9EC5FF), infoContainer = Color(0xFF16283D),
)
private val StaffDarkStatus = StatusColors(
    success = Color(0xFF3DDC97), successContainer = Color(0xFF12392A), onSuccessContainer = Color(0xFFA7F3CF),
    warning = Color(0xFFF5B544), warningContainer = Color(0xFF3A2A0B), onWarningContainer = Color(0xFFFFE3B5),
    info = Color(0xFF58A6FF), infoContainer = Color(0xFF16283D),
)
private val StaffLightStatus = StatusColors(
    success = Color(0xFF1A7F37), successContainer = Color(0xFFDAFBE1), onSuccessContainer = Color(0xFF0A3D1A),
    warning = Color(0xFF9A6700), warningContainer = Color(0xFFFFF1CC), onWarningContainer = Color(0xFF4D3300),
    info = Color(0xFF0969DA), infoContainer = Color(0xFFDDEBFF),
)

val LocalStatusColors = staticCompositionLocalOf { PatientLightStatus }

private fun typography(family: FontFamily, look: AppLook): Typography {
    val base = Typography()
    val display = if (look == AppLook.Patient) FontWeight.ExtraBold else FontWeight.Bold
    fun TextStyle.f(weight: FontWeight? = null, tracking: Float? = null) =
        copy(fontFamily = family, fontWeight = weight ?: fontWeight, letterSpacing = tracking?.sp ?: letterSpacing)
    return base.copy(
        displayLarge = base.displayLarge.f(display, -1f),
        displayMedium = base.displayMedium.f(display, -0.8f),
        displaySmall = base.displaySmall.f(display, -0.6f),
        headlineLarge = base.headlineLarge.f(display, -0.8f),
        headlineMedium = base.headlineMedium.f(display, -0.5f),
        headlineSmall = base.headlineSmall.f(display, -0.4f),
        titleLarge = base.titleLarge.f(FontWeight.Bold, -0.2f),
        titleMedium = base.titleMedium.f(FontWeight.SemiBold, 0f),
        titleSmall = base.titleSmall.f(FontWeight.SemiBold, 0f),
        bodyLarge = base.bodyLarge.f(tracking = 0f),
        bodyMedium = base.bodyMedium.f(tracking = 0f),
        bodySmall = base.bodySmall.f(tracking = 0f),
        labelLarge = base.labelLarge.f(FontWeight.SemiBold, 0f),
        labelMedium = base.labelMedium.f(FontWeight.SemiBold, 0.1f),
        labelSmall = base.labelSmall.f(FontWeight.Medium, 0.2f),
    )
}

private val PatientTypography = typography(Jakarta, AppLook.Patient)
private val StaffTypography = typography(Inter, AppLook.Staff)

private val PatientShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val StaffShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp),
)

/**
 * The staff look is dark-first: "follow system" means dark at the counter. An explicit Light choice in
 * Settings still gets a light Counter Pro palette.
 */
@Composable
fun MedAssistTheme(look: AppLook = AppLook.Patient, mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> look == AppLook.Staff || isSystemInDarkTheme()
    }
    val colors: ColorScheme
    val status: StatusColors
    when (look) {
        AppLook.Patient -> { colors = if (dark) PatientDark else PatientLight; status = if (dark) PatientDarkStatus else PatientLightStatus }
        AppLook.Staff -> { colors = if (dark) StaffDark else StaffLight; status = if (dark) StaffDarkStatus else StaffLightStatus }
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
    }

    CompositionLocalProvider(LocalStatusColors provides status, LocalAppLook provides look) {
        MaterialTheme(
            colorScheme = colors,
            typography = if (look == AppLook.Staff) StaffTypography else PatientTypography,
            shapes = if (look == AppLook.Staff) StaffShapes else PatientShapes,
            content = content,
        )
    }
}
