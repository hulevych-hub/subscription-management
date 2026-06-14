@file:SuppressLint("NewApi")

package com.example.subscription_manager.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscription_manager.domain.model.HomeSubscriptionItem
import com.example.subscription_manager.domain.model.PaymentStatus
import com.example.subscription_manager.domain.model.Recurrence
import com.example.subscription_manager.domain.utils.DateCalculator
import com.example.subscription_manager.ui.theme.DeepBlue
import com.example.subscription_manager.ui.theme.DeepGreen
import com.example.subscription_manager.ui.theme.DeepOrange
import com.example.subscription_manager.ui.theme.SoftBlue
import com.example.subscription_manager.ui.theme.SoftGreen
import com.example.subscription_manager.ui.theme.SoftOrange
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

private val ScreenBackground = Color(0xFFF9F9F9)
private val CardShape = RoundedCornerShape(24.dp)
private val ChipShape = RoundedCornerShape(16.dp)
private val MutedText = Color(0xFF6B7280)
private val DateFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddSubscription: () -> Unit,
    onEditSubscription: (Long) -> Unit,
    onSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Subscriptions", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSubscription,
                containerColor = Color.Black,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (state.isLoading) {
                    CircularProgressIndicator()
                } else {
                    DashboardHeader(
                        total = state.totalSpend,
                        totalCount = state.items.size,
                        dueCount = state.items.count { it.status == PaymentStatus.DUE_SOON || it.status == PaymentStatus.OVERDUE },
                        paidCount = state.items.count { it.status == PaymentStatus.PAID }
                    )
                }
            }

            if (state.items.isEmpty() && !state.isLoading) {
                item {
                    EmptyState(onAddSubscription = onAddSubscription)
                }
            }

            items(state.items, key = { it.subscription.id }) { item ->
                SubscriptionItemView(
                    item = item,
                    onToggleRenewal = { viewModel.toggleRenewal(item.subscription.id) },
                    onMarkPaid = { viewModel.markPaid(item.subscription.id) },
                    onClick = { onEditSubscription(item.subscription.id) }
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    total: Double,
    totalCount: Int,
    dueCount: Int,
    paidCount: Int
) {
    Surface(
        color = Color.White,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Total Monthly Spend",
                style = MaterialTheme.typography.labelMedium,
                color = MutedText,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatAmount(total),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DashboardStat(
                    value = totalCount.toString(),
                    label = "Subscriptions",
                    color = DeepBlue,
                    backgroundColor = SoftBlue
                )
                DashboardStat(
                    value = dueCount.toString(),
                    label = "Due soon",
                    color = DeepOrange,
                    backgroundColor = SoftOrange
                )
                DashboardStat(
                    value = paidCount.toString(),
                    label = "Paid",
                    color = DeepGreen,
                    backgroundColor = SoftGreen
                )
            }
        }
    }
}

@Composable
private fun DashboardStat(
    value: String,
    label: String,
    color: Color,
    backgroundColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = ChipShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun EmptyState(onAddSubscription: () -> Unit) {
    Surface(
        color = Color.White,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "No subscriptions yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add your first subscription to start tracking renewals and reminders.",
                style = MaterialTheme.typography.bodyMedium,
                color = MutedText
            )
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.Button(
                onClick = onAddSubscription,
                shape = ChipShape,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("Add subscription", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SubscriptionItemView(
    item: HomeSubscriptionItem,
    onToggleRenewal: () -> Unit,
    onMarkPaid: () -> Unit,
    onClick: () -> Unit
) {
    val isPaid = item.status == PaymentStatus.PAID
    val statusColor = item.status.color()
    val statusBackgroundColor = item.status.backgroundColor()
    val daysUntil = DateCalculator.daysUntil(item.subscription.nextPaymentDate)

    Surface(
        onClick = onClick,
        color = when {
            isPaid -> SoftGreen
            item.status == PaymentStatus.DUE_SOON || item.status == PaymentStatus.OVERDUE -> SoftOrange
            else -> Color.White
        },
        shape = CardShape,
        border = if (item.status == PaymentStatus.UPCOMING) BorderStroke(1.dp, Color(0xFFE5E7EB)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subscription.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.subscription.type.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedText,
                            fontWeight = FontWeight.Medium
                        )
                        Text(text = "•", color = MutedText)
                        Text(
                            text = recurrenceLabel(item.subscription.recurrence),
                            style = MaterialTheme.typography.labelMedium,
                            color = MutedText,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                StatusBadge(
                    label = item.status.label(),
                    color = statusColor,
                    backgroundColor = statusBackgroundColor
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = formatAmount(item.subscription.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "per ${recurrenceShortLabel(item.subscription.recurrence)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText
                    )
                }
                Text(
                    text = daysUntilLabel(daysUntil),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (daysUntil < 0) DeepOrange else MutedText,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Next payment",
                        style = MaterialTheme.typography.labelMedium,
                        color = MutedText
                    )
                    Text(
                        text = item.subscription.nextPaymentDate.format(DateFormatter),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (!isPaid) {
                    IconButton(
                        onClick = onMarkPaid,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Mark Paid", tint = DeepOrange)
                    }
                } else {
                    Switch(
                        checked = item.subscription.renewalEnabled,
                        onCheckedChange = { onToggleRenewal() }
                    )
                }
            }

            if (!item.subscription.willRenew) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Renewal is disabled",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: Color, backgroundColor: Color) {
    Surface(
        color = backgroundColor,
        shape = CircleShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (label == "paid") {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = color
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun PaymentStatus.label(): String {
    return name.lowercase().replace("_", " ")
}

private fun PaymentStatus.color(): Color {
    return when (this) {
        PaymentStatus.PAID -> DeepGreen
        PaymentStatus.OVERDUE -> DeepOrange
        PaymentStatus.DUE_SOON -> DeepOrange
        PaymentStatus.UPCOMING -> DeepBlue
    }
}

private fun PaymentStatus.backgroundColor(): Color {
    return when (this) {
        PaymentStatus.PAID -> SoftGreen
        PaymentStatus.OVERDUE -> SoftOrange
        PaymentStatus.DUE_SOON -> SoftOrange
        PaymentStatus.UPCOMING -> SoftBlue
    }
}

private fun recurrenceLabel(recurrence: Recurrence): String {
    return when (recurrence) {
        Recurrence.MONTHLY -> "Monthly"
        Recurrence.ANNUAL -> "Annual"
    }
}

private fun recurrenceShortLabel(recurrence: Recurrence): String {
    return when (recurrence) {
        Recurrence.MONTHLY -> "month"
        Recurrence.ANNUAL -> "year"
    }
}

private fun daysUntilLabel(daysUntil: Long): String {
    return when {
        daysUntil < 0 -> "${daysUntil.absoluteValue}d overdue"
        daysUntil == 0L -> "Due today"
        daysUntil == 1L -> "Tomorrow"
        else -> "in ${daysUntil}d"
    }
}

private fun formatAmount(amount: Double): String {
    return "$${String.format(Locale.US, "%.2f", amount)}"
}
