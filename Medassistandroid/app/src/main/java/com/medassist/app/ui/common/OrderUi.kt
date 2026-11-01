package com.medassist.app.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.medassist.app.core.model.Fulfilment
import com.medassist.app.core.model.OrderStatus
import com.medassist.app.core.model.OrderView

fun OrderStatus.label(fulfilment: Fulfilment): String = when (this) {
    OrderStatus.PLACED -> "Waiting for pharmacy"
    OrderStatus.ACCEPTED -> "Being prepared"
    OrderStatus.READY -> if (fulfilment == Fulfilment.PICKUP) "Ready for pickup" else "Out for delivery"
    OrderStatus.COMPLETED -> "Completed"
    OrderStatus.REJECTED -> "Rejected"
    OrderStatus.CANCELLED -> "Cancelled"
}

fun OrderStatus.tone(): Tone = when (this) {
    OrderStatus.PLACED -> Tone.Warning
    OrderStatus.ACCEPTED, OrderStatus.READY -> Tone.Brand
    OrderStatus.COMPLETED -> Tone.Success
    OrderStatus.REJECTED, OrderStatus.CANCELLED -> Tone.Danger
}

@Composable
fun OrderStatusPill(order: OrderView) = StatusPill(order.status.label(order.fulfilment), order.status.tone())

/** Placed → Accepted → Ready → Completed, with the current step highlighted. */
@Composable
fun OrderTimeline(order: OrderView, modifier: Modifier = Modifier) {
    val steps = listOf(OrderStatus.PLACED, OrderStatus.ACCEPTED, OrderStatus.READY, OrderStatus.COMPLETED)
    val reached = steps.indexOf(order.status)
    val stopped = order.status == OrderStatus.REJECTED || order.status == OrderStatus.CANCELLED
    Column(modifier) {
        steps.forEachIndexed { i, step ->
            val done = !stopped && i <= reached
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                Surface(
                    shape = CircleShape,
                    color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(14.dp),
                ) {}
                Spacer(Modifier.width(12.dp))
                Text(
                    when (step) {
                        OrderStatus.PLACED -> "Order placed"
                        else -> step.label(order.fulfilment)
                    },
                    style = if (i == reached && !stopped) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
                    color = if (done) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (stopped) {
            Spacer(Modifier.height(4.dp))
            StatusPill(order.status.label(order.fulfilment), Tone.Danger)
            order.rejectionReason?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp)) }
        }
    }
}
