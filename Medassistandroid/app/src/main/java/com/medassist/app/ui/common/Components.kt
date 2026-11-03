package com.medassist.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.Role
import com.medassist.app.ui.theme.AppButtonShape
import com.medassist.app.ui.theme.OnStaffHeader
import com.medassist.app.ui.theme.StaffHeader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.medassist.app.core.model.ApiError
import com.medassist.app.ui.theme.AppLook
import com.medassist.app.ui.theme.LocalAppLook
import com.medassist.app.ui.theme.LocalStatusColors
import com.medassist.app.ui.theme.Numeric

@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun ErrorBox(error: ApiError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    EmptyState(
        icon = if (error == ApiError.Network) Icons.Outlined.CloudOff else Icons.Outlined.ErrorOutline,
        title = if (error == ApiError.Network) "You're offline" else "Couldn't load this",
        message = error.message(),
        actionLabel = onRetry?.let { "Try again" },
        onAction = onRetry,
        modifier = modifier,
    )
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(shape = if (LocalAppLook.current == AppLook.Staff) RectangleShape else CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(36.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            OutlinedButton(onClick = onAction, shape = AppButtonShape) { Text(actionLabel) }
        }
    }
}

/** Renders a UiState: spinner, error with retry, or [content]. */
@Composable
fun <T> StateContent(state: UiState<T>, onRetry: () -> Unit, content: @Composable (T) -> Unit) {
    when (state) {
        UiState.Loading -> LoadingBox()
        is UiState.Error -> ErrorBox(state.error, onRetry)
        is UiState.Success -> content(state.data)
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, trailing: @Composable (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

enum class Tone { Neutral, Brand, Success, Warning, Danger }

@Composable
fun StatusPill(text: String, tone: Tone, modifier: Modifier = Modifier) {
    val status = LocalStatusColors.current
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (tone) {
        Tone.Neutral -> scheme.surfaceVariant to scheme.onSurfaceVariant
        Tone.Brand -> scheme.primaryContainer to scheme.onPrimaryContainer
        Tone.Success -> status.successContainer to status.onSuccessContainer
        Tone.Warning -> status.warningContainer to status.onWarningContainer
        Tone.Danger -> scheme.errorContainer to scheme.onErrorContainer
    }
    Text(
        text,
        modifier = modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 3.dp),
        color = fg,
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
    )
}

/**
 * A dashboard number with a caption. Staff (Carbon): a flat grey tile, light-weight figure, tone shown as a tag.
 * Patient (Paper): a white card with the figure in the serif.
 */
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tone: Tone = Tone.Brand,
    caption: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val scheme = MaterialTheme.colorScheme
    AppCard(modifier, onClick) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = scheme.onSurfaceVariant, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Icon(icon, null, tint = scheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall.merge(Numeric), maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1)
            }
            if (tone == Tone.Warning || tone == Tone.Danger) {
                Spacer(Modifier.height(4.dp))
                StatusPill(if (tone == Tone.Danger) "Act now" else "Attention", tone)
            }
        }
    }
}

/**
 * The app's card. Staff (Carbon): a flat grey layer, square, no border or shadow; depth comes from the layer colour.
 * Patient (Paper): a white sheet on cream, generous radius, no shadow.
 */
@Composable
fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val staff = LocalAppLook.current == AppLook.Staff
    val colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
    val elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    val shape = if (staff) RectangleShape else MaterialTheme.shapes.large
    if (onClick != null) {
        Card(onClick = onClick, modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = content)
    } else {
        Card(modifier = modifier, shape = shape, colors = colors, elevation = elevation, content = content)
    }
}

/** Top-bar colours: the dark Carbon header for staff, the page colour for patients. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun appBarColors(): TopAppBarColors =
    if (LocalAppLook.current == AppLook.Staff) {
        TopAppBarDefaults.topAppBarColors(
            containerColor = StaffHeader,
            scrolledContainerColor = StaffHeader,
            titleContentColor = OnStaffHeader,
            navigationIconContentColor = OnStaffHeader,
            actionIconContentColor = OnStaffHeader,
        )
    } else {
        TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
            scrolledContainerColor = MaterialTheme.colorScheme.background,
        )
    }

/** Small spaced label above a group: Carbon's 12sp helper text, Paper's tracked eyebrow. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    Text(text, modifier, style = MaterialTheme.typography.labelSmall, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

/**
 * One-of-N picker. Staff: Carbon content switcher (joined square cells, selected cell filled dark).
 * Patient: separate pills, selected pill filled stone.
 */
@Composable
fun ChoiceRow(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val staff = LocalAppLook.current == AppLook.Staff
    val scheme = MaterialTheme.colorScheme
    if (staff) {
        Row(modifier.fillMaxWidth().height(40.dp).border(1.dp, scheme.outline)) {
            options.forEachIndexed { i, label ->
                if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(scheme.outline))
                val on = i == selected
                Box(
                    Modifier.weight(1f).fillMaxHeight().background(if (on) scheme.onSurface else Color.Transparent)
                        .selectable(on, role = Role.RadioButton) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) scheme.surface else scheme.onSurface, maxLines = 1)
                }
            }
        }
    } else {
        Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEachIndexed { i, label ->
                val on = i == selected
                Text(
                    label,
                    Modifier.clip(CircleShape)
                        .then(if (on) Modifier.background(scheme.secondary) else Modifier.border(1.dp, scheme.outline, CircleShape))
                        .selectable(on, role = Role.RadioButton) { onSelect(i) }
                        .padding(horizontal = 16.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) scheme.onSecondary else scheme.onSurface,
                    maxLines = 1,
                )
            }
        }
    }
}

/** One bottom-bar destination. */
data class NavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)

/**
 * Bottom navigation. Staff (Carbon): grey bar, the selected tab gets a 2dp blue rule on top and semibold label.
 * Patient (Paper): cream bar on a hairline, selected tab in ink with a small blue dot under the label.
 */
@Composable
fun AppNavigationBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit) {
    val staff = LocalAppLook.current == AppLook.Staff
    val scheme = MaterialTheme.colorScheme
    val bar = if (staff) scheme.surfaceContainerLowest else scheme.background
    Column(Modifier.fillMaxWidth().background(bar).windowInsetsPadding(WindowInsets.navigationBars)) {
        HorizontalDivider(color = scheme.outlineVariant)
        Row(Modifier.fillMaxWidth().height(64.dp).selectableGroup()) {
            items.forEachIndexed { i, item ->
                val on = i == selected
                val fg = if (on) scheme.onSurface else scheme.onSurfaceVariant
                Column(
                    Modifier.weight(1f).fillMaxHeight()
                        .selectable(on, role = Role.Tab) { onSelect(i) }
                        .then(if (staff && on) Modifier.drawBehind { drawRect(scheme.primary, size = Size(size.width, 2.dp.toPx())) } else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(if (on) item.selectedIcon else item.icon, null, tint = fg, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        item.label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal),
                        color = fg,
                        maxLines = 1,
                    )
                    if (!staff) {
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.size(4.dp).clip(CircleShape).background(if (on) scheme.primary else Color.Transparent))
                    }
                }
            }
        }
    }
}

/**
 * A minimal bar chart, no chart library. Staff: square Carbon bars on solid gridlines. Patient: rounded bars on tracks.
 * A month gets thin bars with no tracks, so 30 mostly-empty days don't read as a barcode. [axis] labels the first
 * and last bar instead of every bar.
 */
@Composable
fun BarChart(
    values: List<Float>,
    labels: List<String>,
    modifier: Modifier = Modifier,
    highlightLast: Boolean = true,
    axis: Pair<String, String>? = null,
) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val staff = LocalAppLook.current == AppLook.Staff
    val dense = values.size > 12 || staff
    val gap = if (values.size > 12) 3.dp else if (staff) 8.dp else 10.dp
    val barColor = MaterialTheme.colorScheme.primary
    val dimColor = if (staff) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary.copy(alpha = if (dense) 0.55f else 0.35f)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    val grid = MaterialTheme.colorScheme.outlineVariant
    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(120.dp)) {
            if (staff) {
                Canvas(Modifier.matchParentSize()) {
                    listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { f ->
                        val y = size.height * (1f - f)
                        drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                    }
                }
            }
            Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(gap)) {
                values.forEachIndexed { i, v ->
                    val fraction by animateFloatAsState(v / max, label = "bar")
                    val color = if (highlightLast && i == values.lastIndex) barColor else dimColor
                    Canvas(Modifier.weight(1f).fillMaxSize()) {
                        val r = CornerRadius((if (staff) 0.dp else if (dense) 2.dp else 8.dp).toPx())
                        if (!dense) drawRoundRect(track, size = size, cornerRadius = r)
                        // A day with no sales still gets a stub on the baseline, so the gap reads as "zero", not "missing".
                        val h = if (dense) (size.height * fraction).coerceAtLeast(2.dp.toPx()) else size.height * fraction
                        val c = if (dense && v == 0f) track else color
                        if (h > 0f) drawRoundRect(c, topLeft = Offset(0f, size.height - h), size = Size(size.width, h), cornerRadius = r)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        val labelStyle = MaterialTheme.typography.labelSmall
        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        if (axis != null) {
            Row(Modifier.fillMaxWidth()) {
                Text(axis.first, Modifier.weight(1f), style = labelStyle, color = labelColor)
                Text(axis.second, style = labelStyle, color = labelColor)
            }
        } else if (labels.any { it.isNotBlank() }) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                labels.forEach {
                    Text(it, Modifier.weight(1f), style = labelStyle, textAlign = TextAlign.Center, color = labelColor, maxLines = 1)
                }
            }
        }
    }
}

/** Staff: a square Carbon button, 48dp. Patient: a 52dp pill. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false, enabled: Boolean = true) {
    val staff = LocalAppLook.current == AppLook.Staff
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = modifier.height(if (staff) 48.dp else 52.dp),
        shape = AppButtonShape,
        contentPadding = PaddingValues(horizontal = 24.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, fontWeight = if (staff) FontWeight.Normal else FontWeight.SemiBold)
    }
}

/** Small "Rx" badge for prescription-only medicines. */
@Composable
fun RxBadge(modifier: Modifier = Modifier) {
    StatusPill("Rx", Tone.Danger, modifier)
}

@Composable
fun KeyValueRow(key: String, value: String, modifier: Modifier = Modifier, strong: Boolean = false, color: Color = Color.Unspecified) {
    Row(modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(key, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(
            value,
            style = (if (strong) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium).merge(Numeric),
            color = color,
        )
    }
}
