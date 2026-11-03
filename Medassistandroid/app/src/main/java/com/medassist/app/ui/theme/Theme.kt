package com.medassist.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
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
 *  - [AppLook.Staff] "Carbon Grid" (after IBM Carbon): white and grey layers, square corners, IBM Plex Sans + Mono,
 *    one interactive blue, a dark header. Reads as serious B2B software; dense and tabular for the counter.
 *  - [AppLook.Patient] "Wellness Paper" (after Oura): cream paper, a light editorial serif (Newsreader) over a quiet
 *    grotesque (Geist), an earth palette, and one decisive blue for actions. Calm and premium.
 */
enum class AppLook { Patient, Staff }

val LocalAppLook = staticCompositionLocalOf { AppLook.Patient }

// ---------- Fonts (bundled, Latin + ₹ subset, SIL OFL 1.1) ----------
private val PlexSans = FontFamily(
    Font(R.font.plexsans_light, FontWeight.Light),
    Font(R.font.plexsans_regular, FontWeight.Normal),
    Font(R.font.plexsans_medium, FontWeight.Medium),
    Font(R.font.plexsans_semibold, FontWeight.SemiBold),
)
private val PlexMono = FontFamily(
    Font(R.font.plexmono_regular, FontWeight.Normal),
    Font(R.font.plexmono_medium, FontWeight.Medium),
)
private val Newsreader = FontFamily(
    Font(R.font.newsreader_light, FontWeight.Light),
    Font(R.font.newsreader_regular, FontWeight.Normal),
)
private val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold),
)

/** Tabular figures so columns of rupee amounts line up. */
val Numeric = TextStyle(fontFeatureSettings = "tnum")

/** For codes and table figures: invoice/order numbers, batches, SKUs. IBM Plex Mono has the ₹ glyph. */
val Mono = TextStyle(fontFamily = PlexMono, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)

/** The patient look's editorial serif, for the few places that want it outside the type scale. */
val Serif = TextStyle(fontFamily = Newsreader, fontWeight = FontWeight.Light)

// ---------- Staff · Carbon Grid ----------
private val CarbonBlue = Color(0xFF0F62FE)

private val StaffLight = lightColorScheme(
    primary = CarbonBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD0E2FF),
    onPrimaryContainer = Color(0xFF001D6C),
    secondary = Color(0xFF393939),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF161616),
    tertiary = Color(0xFF8A3FFC),
    tertiaryContainer = Color(0xFFE8DAFF),
    onTertiaryContainer = Color(0xFF31135E),
    background = Color.White,
    onBackground = Color(0xFF161616),
    surface = Color.White,
    onSurface = Color(0xFF161616),
    surfaceTint = Color.White,
    surfaceVariant = Color(0xFFE0E0E0),
    onSurfaceVariant = Color(0xFF525252),
    surfaceContainerLowest = Color(0xFFF4F4F4),
    surfaceContainerLow = Color(0xFFF4F4F4),
    surfaceContainer = Color(0xFFF4F4F4),
    surfaceContainerHigh = Color(0xFFE8E8E8),
    surfaceContainerHighest = Color(0xFFE0E0E0),
    inverseSurface = Color(0xFF393939),
    inverseOnSurface = Color(0xFFF4F4F4),
    outline = Color(0xFF8D8D8D),
    outlineVariant = Color(0xFFE0E0E0),
    error = Color(0xFFDA1E28),
    onError = Color.White,
    errorContainer = Color(0xFFFFF1F1),
    onErrorContainer = Color(0xFFA2191F),
)

private val StaffDark = darkColorScheme(
    primary = Color(0xFF4589FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF002D9C),
    onPrimaryContainer = Color(0xFFD0E2FF),
    secondary = Color(0xFFC6C6C6),
    onSecondary = Color(0xFF161616),
    secondaryContainer = Color(0xFF393939),
    onSecondaryContainer = Color(0xFFF4F4F4),
    tertiary = Color(0xFFA56EFF),
    tertiaryContainer = Color(0xFF491D8B),
    onTertiaryContainer = Color(0xFFE8DAFF),
    background = Color(0xFF161616),
    onBackground = Color(0xFFF4F4F4),
    surface = Color(0xFF161616),
    onSurface = Color(0xFFF4F4F4),
    surfaceTint = Color(0xFF161616),
    surfaceVariant = Color(0xFF393939),
    onSurfaceVariant = Color(0xFFC6C6C6),
    surfaceContainerLowest = Color(0xFF262626),
    surfaceContainerLow = Color(0xFF262626),
    surfaceContainer = Color(0xFF262626),
    surfaceContainerHigh = Color(0xFF333333),
    surfaceContainerHighest = Color(0xFF393939),
    inverseSurface = Color(0xFFF4F4F4),
    inverseOnSurface = Color(0xFF161616),
    outline = Color(0xFF6F6F6F),
    outlineVariant = Color(0xFF393939),
    error = Color(0xFFFA4D56),
    onError = Color(0xFF161616),
    errorContainer = Color(0xFF520408),
    onErrorContainer = Color(0xFFFFD7D9),
)

// ---------- Patient · Wellness Paper ----------
private val ActionBlue = Color(0xFF2A72DE)

private val PatientLight = lightColorScheme(
    primary = ActionBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3ECFA),
    onPrimaryContainer = Color(0xFF0B2E66),
    secondary = Color(0xFF4A4741),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECE5D9),
    onSecondaryContainer = Color(0xFF1C1B1A),
    tertiary = Color(0xFF5C6F5D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE4E8D6),
    onTertiaryContainer = Color(0xFF224228),
    background = Color(0xFFF7F1E8),
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFF7F1E8),
    onSurface = Color(0xFF1C1B1A),
    surfaceTint = Color(0xFFF7F1E8),
    surfaceVariant = Color(0xFFECE5D9),
    onSurfaceVariant = Color(0xFF6B6863),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF7F1),
    surfaceContainer = Color(0xFFF1EADF),
    surfaceContainerHigh = Color(0xFFECE5D9),
    surfaceContainerHighest = Color(0xFFE6DED1),
    inverseSurface = Color(0xFF1C1B1A),
    inverseOnSurface = Color(0xFFF7F1E8),
    outline = Color(0xFFCCCAC7),
    outlineVariant = Color(0xFFE1DCD3),
    error = Color(0xFFC4321C),
    onError = Color.White,
    errorContainer = Color(0xFFFBE3DD),
    onErrorContainer = Color(0xFF5E1206),
)

private val PatientDark = darkColorScheme(
    primary = Color(0xFF8AB4F8),
    onPrimary = Color(0xFF0B2E66),
    primaryContainer = Color(0xFF1D3F75),
    onPrimaryContainer = Color(0xFFD9E6FB),
    secondary = Color(0xFFD9D2C6),
    onSecondary = Color(0xFF2B2926),
    secondaryContainer = Color(0xFF3A3733),
    onSecondaryContainer = Color(0xFFF1EADF),
    tertiary = Color(0xFFB6C4A3),
    onTertiary = Color(0xFF223322),
    tertiaryContainer = Color(0xFF34432F),
    onTertiaryContainer = Color(0xFFE4E8D6),
    background = Color(0xFF1A1816),
    onBackground = Color(0xFFF1EADF),
    surface = Color(0xFF1A1816),
    onSurface = Color(0xFFF1EADF),
    surfaceTint = Color(0xFF1A1816),
    surfaceVariant = Color(0xFF3A3733),
    onSurfaceVariant = Color(0xFFB5AEA4),
    surfaceContainerLowest = Color(0xFF24211E),
    surfaceContainerLow = Color(0xFF211F1C),
    surfaceContainer = Color(0xFF2A2724),
    surfaceContainerHigh = Color(0xFF33302C),
    surfaceContainerHighest = Color(0xFF3D3935),
    outline = Color(0xFF6B6863),
    outlineVariant = Color(0xFF3A3733),
    error = Color(0xFFF2876F),
    errorContainer = Color(0xFF5E1206),
    onErrorContainer = Color(0xFFFBE3DD),
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

// Carbon support colours (tags use the 10/20 tint with 70/80 text).
private val StaffLightStatus = StatusColors(
    success = Color(0xFF198038), successContainer = Color(0xFFDEFBE6), onSuccessContainer = Color(0xFF0E6027),
    warning = Color(0xFF8E6A00), warningContainer = Color(0xFFFCF4D6), onWarningContainer = Color(0xFF684E00),
    info = Color(0xFF0043CE), infoContainer = Color(0xFFEDF5FF),
)
private val StaffDarkStatus = StatusColors(
    success = Color(0xFF42BE65), successContainer = Color(0xFF044317), onSuccessContainer = Color(0xFFA7F0BA),
    warning = Color(0xFFF1C21B), warningContainer = Color(0xFF483700), onWarningContainer = Color(0xFFFDDC69),
    info = Color(0xFF78A9FF), infoContainer = Color(0xFF001D6C),
)
// Oura earth tones: gold for "attention", olive for "good", action blue for info.
private val PatientLightStatus = StatusColors(
    success = Color(0xFF3E5A41), successContainer = Color(0xFFE4E8D6), onSuccessContainer = Color(0xFF224228),
    warning = Color(0xFF8A5A10), warningContainer = Color(0xFFF5E6C8), onWarningContainer = Color(0xFF684D07),
    info = ActionBlue, infoContainer = Color(0xFFE3ECFA),
)
private val PatientDarkStatus = StatusColors(
    success = Color(0xFFB6C4A3), successContainer = Color(0xFF34432F), onSuccessContainer = Color(0xFFE4E8D6),
    warning = Color(0xFFDDAA61), warningContainer = Color(0xFF4D3A12), onWarningContainer = Color(0xFFF5E6C8),
    info = Color(0xFF8AB4F8), infoContainer = Color(0xFF1D3F75),
)

val LocalStatusColors = staticCompositionLocalOf { PatientLightStatus }

private val StaffTypography: Typography = Typography().let { b ->
    fun TextStyle.p(w: FontWeight, tracking: Float = 0f) = copy(fontFamily = PlexSans, fontWeight = w, letterSpacing = tracking.sp)
    b.copy(
        displayLarge = b.displayLarge.p(FontWeight.Light),
        displayMedium = b.displayMedium.p(FontWeight.Light),
        displaySmall = b.displaySmall.p(FontWeight.Light),
        headlineLarge = b.headlineLarge.p(FontWeight.Light),
        headlineMedium = b.headlineMedium.p(FontWeight.Light),
        headlineSmall = b.headlineSmall.p(FontWeight.Light),
        titleLarge = b.titleLarge.p(FontWeight.Normal),
        titleMedium = b.titleMedium.p(FontWeight.SemiBold, 0.16f),
        titleSmall = b.titleSmall.p(FontWeight.SemiBold, 0.16f),
        bodyLarge = b.bodyLarge.p(FontWeight.Normal),
        bodyMedium = b.bodyMedium.p(FontWeight.Normal, 0.16f),
        bodySmall = b.bodySmall.p(FontWeight.Normal, 0.32f),
        labelLarge = b.labelLarge.p(FontWeight.Medium, 0.16f),
        labelMedium = b.labelMedium.p(FontWeight.Normal, 0.32f),
        labelSmall = b.labelSmall.p(FontWeight.Normal, 0.32f),
    )
}

private val PatientTypography: Typography = Typography().let { b ->
    fun TextStyle.serif(w: FontWeight, tracking: Float = 0f) = copy(fontFamily = Newsreader, fontWeight = w, letterSpacing = tracking.sp)
    fun TextStyle.sans(w: FontWeight, tracking: Float = 0f) = copy(fontFamily = Geist, fontWeight = w, letterSpacing = tracking.sp)
    b.copy(
        displayLarge = b.displayLarge.serif(FontWeight.Light, -0.5f),
        displayMedium = b.displayMedium.serif(FontWeight.Light, -0.4f),
        displaySmall = b.displaySmall.serif(FontWeight.Light, -0.3f),
        headlineLarge = b.headlineLarge.serif(FontWeight.Light, -0.3f),
        headlineMedium = b.headlineMedium.serif(FontWeight.Light, -0.2f),
        headlineSmall = b.headlineSmall.serif(FontWeight.Light),
        titleLarge = b.titleLarge.serif(FontWeight.Normal).copy(fontSize = 23.sp),
        titleMedium = b.titleMedium.sans(FontWeight.SemiBold),
        titleSmall = b.titleSmall.sans(FontWeight.SemiBold),
        bodyLarge = b.bodyLarge.sans(FontWeight.Normal),
        bodyMedium = b.bodyMedium.sans(FontWeight.Normal),
        bodySmall = b.bodySmall.sans(FontWeight.Normal),
        labelLarge = b.labelLarge.sans(FontWeight.SemiBold),
        labelMedium = b.labelMedium.sans(FontWeight.Medium, 0.2f),
        labelSmall = b.labelSmall.sans(FontWeight.SemiBold, 0.6f),
    )
}

/** Carbon: square everything. */
private val StaffShapes = Shapes(
    extraSmall = RoundedCornerShape(0.dp),
    small = RoundedCornerShape(0.dp),
    medium = RoundedCornerShape(0.dp),
    large = RoundedCornerShape(0.dp),
    extraLarge = RoundedCornerShape(0.dp),
)

/** Paper: soft, generous radii; actions are pills. */
private val PatientShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** M3 buttons ignore the Shapes scale (they're always full pills), so screens pass this explicitly. */
val AppButtonShape: Shape
    @Composable @ReadOnlyComposable
    get() = if (LocalAppLook.current == AppLook.Staff) RectangleShape else CircleShape

/** Carbon's UI-shell header is dark in both light and dark themes. */
val StaffHeader = Color(0xFF161616)
val OnStaffHeader = Color(0xFFF4F4F4)

@Composable
fun MedAssistTheme(look: AppLook = AppLook.Patient, mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
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
                // Staff screens sit under the dark Carbon header, so status-bar icons are always light there.
                isAppearanceLightStatusBars = look == AppLook.Patient && !dark
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
