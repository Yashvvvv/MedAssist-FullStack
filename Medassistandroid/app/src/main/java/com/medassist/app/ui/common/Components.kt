package com.medassist.app.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
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
            OutlinedButton(onClick = onAction) { Text(actionLabel) }
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

/** A dashboard number with a caption. */
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
    val status = LocalStatusColors.current
    val scheme = MaterialTheme.colorScheme
    val (iconBg, iconFg) = when (tone) {
        Tone.Warning -> status.warningContainer to status.onWarningContainer
        Tone.Danger -> scheme.errorContainer to scheme.onErrorContainer
        Tone.Success -> status.successContainer to status.onSuccessContainer
        else -> scheme.primaryContainer to scheme.onPrimaryContainer
    }
    val colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLowest)
    val body: @Composable () -> Unit = {
        Column(Modifier.padding(16.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = iconBg, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconFg, modifier = Modifier.size(20.dp)) }
            }
            Spacer(Modifier.height(12.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall.merge(Numeric), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant, maxLines = 1)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
    if (onClick != null) Card(onClick = onClick, modifier = modifier, colors = colors) { body() }
    else Card(modifier = modifier, colors = colors) { body() }
}

/** A minimal bar chart: no chart library for seven bars. */
@Composable
fun BarChart(values: List<Float>, labels: List<String>, modifier: Modifier = Modifier, highlightLast: Boolean = true) {
    val max = (values.maxOrNull() ?: 0f).coerceAtLeast(1f)
    val barColor = MaterialTheme.colorScheme.primary
    val dimColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    val track = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            values.forEachIndexed { i, v ->
                val fraction by animateFloatAsState(v / max, label = "bar")
                val color = if (highlightLast && i == values.lastIndex) barColor else dimColor
                Canvas(Modifier.weight(1f).fillMaxSize()) {
                    val r = CornerRadius(8.dp.toPx())
                    drawRoundRect(track, size = size, cornerRadius = r)
                    val h = size.height * fraction
                    if (h > 0f) drawRoundRect(color, topLeft = Offset(0f, size.height - h), size = Size(size.width, h), cornerRadius = r)
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            labels.forEach {
                Text(it, Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, loading: Boolean = false, enabled: Boolean = true) {
    Button(onClick = onClick, enabled = enabled && !loading, modifier = modifier.height(52.dp), contentPadding = PaddingValues(horizontal = 24.dp)) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(Modifier.width(12.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
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
