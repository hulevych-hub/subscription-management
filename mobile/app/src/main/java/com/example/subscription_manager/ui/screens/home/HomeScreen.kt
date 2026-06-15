@file:SuppressLint("NewApi")

package com.example.subscription_manager.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.example.subscription_manager.ui.theme.DarkDeepGreen
import com.example.subscription_manager.ui.theme.DarkDeepOrange
import com.example.subscription_manager.ui.theme.DarkSoftBlue
import com.example.subscription_manager.ui.theme.DarkSoftGreen
import com.example.subscription_manager.ui.theme.DarkSoftOrange
import com.example.subscription_manager.ui.theme.DarkSoftPurple
import com.example.subscription_manager.ui.theme.DarkSoftRed
import com.example.subscription_manager.ui.theme.DeepGreen
import com.example.subscription_manager.ui.theme.DeepOrange
import com.example.subscription_manager.ui.theme.LocalUseDarkTheme
import com.example.subscription_manager.ui.theme.SoftBlue
import com.example.subscription_manager.ui.theme.SoftGreen
import com.example.subscription_manager.ui.theme.SoftOrange
import com.example.subscription_manager.ui.theme.SoftPurple
import com.example.subscription_manager.ui.theme.SoftRed
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.absoluteValue

private val CardShape = RoundedCornerShape(24.dp)
private val ChipShape = RoundedCornerShape(16.dp)
private val DateFormatter = DateTimeFormatter.ofPattern("MMM dd", Locale.US)
private val DateRangeFormatter = DateTimeFormatter.ofPattern("dd-MMMM-yyyy", Locale.US)

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
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Subscriptions", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSubscription,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                DashboardHeader(
                    total = state.totalSpend,
                    totalCount = state.totalCount,
                    dueCount = state.dueSoonCount,
                    thisMonthCount = state.thisMonthCount,
                    trialCount = state.trialCount,
                    paidCount = state.paidCount,
                    activeFilter = state.activeFilter,
                    onSubscriptionsClick = { viewModel.toggleFilter(HomeFilter.ALL) },
                    onThisMonthClick = { viewModel.toggleFilter(HomeFilter.THIS_MONTH) },
                    onDueSoonClick = { viewModel.toggleFilter(HomeFilter.DUE_SOON) },
                    onTrialClick = { viewModel.toggleFilter(HomeFilter.TRIAL) },
                    onPaidClick = { viewModel.toggleFilter(HomeFilter.PAID) }
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (state.items.isEmpty()) {
                        item {
                            EmptyState(
                                activeFilter = state.activeFilter,
                                totalCount = state.totalCount,
                                onAddSubscription = onAddSubscription
                            )
                        }
                    }

                    items(state.items, key = { it.subscription.id }) { item ->
                        SubscriptionItemView(
                            item = item,
                            onToggleRenewal = { viewModel.toggleRenewal(item.subscription.id) },
                            onMarkPaid = { viewModel.markPaid(item.subscription.id) },
                            onMarkUnpaid = { viewModel.markUnpaid(item.subscription.id) },
                            onClick = { onEditSubscription(item.subscription.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    total: Double,
    totalCount: Int,
    dueCount: Int,
    thisMonthCount: Int,
    trialCount: Int,
    paidCount: Int,
    activeFilter: HomeFilter,
    onSubscriptionsClick: () -> Unit,
    onThisMonthClick: () -> Unit,
    onDueSoonClick: () -> Unit,
    onTrialClick: () -> Unit,
    onPaidClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Total Monthly Spend",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    label = "All",
                    color = MaterialTheme.colorScheme.primary,
                    backgroundColor = filterBackgroundColor(HomeFilter.ALL),
                    selected = activeFilter == HomeFilter.ALL,
                    onClick = onSubscriptionsClick
                )
                DashboardStat(
                    value = thisMonthCount.toString(),
                    label = "This month",
                    color = MaterialTheme.colorScheme.tertiary,
                    backgroundColor = filterBackgroundColor(HomeFilter.THIS_MONTH),
                    selected = activeFilter == HomeFilter.THIS_MONTH,
                    onClick = onThisMonthClick
                )
                DashboardStat(
                    value = dueCount.toString(),
                    label = "Due soon",
                    color = dueSoonStatusColor(),
                    backgroundColor = filterBackgroundColor(HomeFilter.DUE_SOON),
                    selected = activeFilter == HomeFilter.DUE_SOON,
                    onClick = onDueSoonClick
                )
                DashboardStat(
                    value = trialCount.toString(),
                    label = "Trial",
                    color = MaterialTheme.colorScheme.tertiary,
                    backgroundColor = filterBackgroundColor(HomeFilter.TRIAL),
                    selected = activeFilter == HomeFilter.TRIAL,
                    onClick = onTrialClick
                )
                DashboardStat(
                    value = paidCount.toString(),
                    label = "Paid",
                    color = paidStatusColor(),
                    backgroundColor = filterBackgroundColor(HomeFilter.PAID),
                    selected = activeFilter == HomeFilter.PAID,
                    onClick = onPaidClick
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
    backgroundColor: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = backgroundColor,
        shape = ChipShape,
        border = BorderStroke(1.dp, if (selected) color else color.copy(alpha = 0.25f))
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
private fun EmptyState(
    activeFilter: HomeFilter,
    totalCount: Int,
    onAddSubscription: () -> Unit
) {
    val title = if (totalCount == 0) {
        "No subscriptions yet"
    } else {
        when (activeFilter) {
            HomeFilter.ALL -> "No subscriptions found"
            HomeFilter.THIS_MONTH -> "No subscriptions due this month"
            HomeFilter.DUE_SOON -> "No subscriptions due soon"
            HomeFilter.TRIAL -> "No trial subscriptions"
            HomeFilter.PAID -> "No paid subscriptions"
        }
    }

    val message = if (totalCount == 0) {
        "Add your first subscription to start tracking renewals and reminders."
    } else {
        "Try another filter or update payment status."
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (totalCount == 0) {
                Spacer(modifier = Modifier.height(16.dp))
                androidx.compose.material3.Button(
                    onClick = onAddSubscription,
                    shape = ChipShape,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Add subscription", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SubscriptionItemView(
    item: HomeSubscriptionItem,
    onToggleRenewal: () -> Unit,
    onMarkPaid: () -> Unit,
    onMarkUnpaid: () -> Unit,
    onClick: () -> Unit
) {
    val isPaid = item.status == PaymentStatus.PAID
    val statusColor = item.status.statusColor()
    val statusBackgroundColor = item.status.statusBackgroundColor()
    val daysUntil = DateCalculator.daysUntil(item.subscription.nextPaymentDate)

    Surface(
        onClick = onClick,
        color = when {
            isPaid -> PaymentStatus.PAID.statusBackgroundColor()
            item.status == PaymentStatus.DUE_SOON || item.status == PaymentStatus.OVERDUE -> item.status.statusBackgroundColor()
            else -> MaterialTheme.colorScheme.surface
        },
        shape = CardShape,
        border = if (item.status == PaymentStatus.UPCOMING) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
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
                    Text(
                        text = dateRangeLabel(item.subscription.startDate, item.subscription.endDate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.subscription.type.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                        Text(text = "•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = recurrenceLabel(item.subscription.recurrence),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                StatusBadge(
                    label = item.status.label(),
                    color = statusColor,
                    backgroundColor = statusBackgroundColor,
                    onClick = if (isPaid) onMarkUnpaid else null
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = formatAmount(item.subscription.amount),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = recurrenceAmountSuffix(item.subscription.recurrence),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = daysUntilLabel(daysUntil),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (daysUntil < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Icon(Icons.Default.CheckCircle, contentDescription = "Mark Paid", tint = MaterialTheme.colorScheme.onSurface)
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(
    label: String,
    color: Color,
    backgroundColor: Color,
    onClick: (() -> Unit)?
) {
    Surface(
        enabled = onClick != null,
        onClick = { onClick?.invoke() },
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

@Composable
private fun isDarkTheme(): Boolean {
    return LocalUseDarkTheme.current
}

@Composable
private fun paidStatusColor(): Color {
    return if (isDarkTheme()) DarkDeepGreen else DeepGreen
}

@Composable
private fun dueSoonStatusColor(): Color {
    return if (isDarkTheme()) DarkDeepOrange else DeepOrange
}

@Composable
private fun PaymentStatus.statusColor(): Color {
    return when (this) {
        PaymentStatus.PAID -> paidStatusColor()
        PaymentStatus.OVERDUE -> MaterialTheme.colorScheme.error
        PaymentStatus.DUE_SOON -> dueSoonStatusColor()
        PaymentStatus.UPCOMING -> MaterialTheme.colorScheme.primary
    }
}

@Composable
private fun PaymentStatus.statusBackgroundColor(): Color {
    return when (this) {
        PaymentStatus.PAID -> if (isDarkTheme()) DarkSoftGreen else SoftGreen
        PaymentStatus.OVERDUE -> if (isDarkTheme()) DarkSoftRed else SoftRed
        PaymentStatus.DUE_SOON -> if (isDarkTheme()) DarkSoftOrange else SoftOrange
        PaymentStatus.UPCOMING -> if (isDarkTheme()) DarkSoftBlue else SoftBlue
    }
}

@Composable
private fun filterBackgroundColor(filter: HomeFilter): Color {
    return when (filter) {
        HomeFilter.ALL -> if (isDarkTheme()) DarkSoftBlue else SoftBlue
        HomeFilter.THIS_MONTH -> if (isDarkTheme()) DarkSoftPurple else SoftPurple
        HomeFilter.DUE_SOON -> if (isDarkTheme()) DarkSoftOrange else SoftOrange
        HomeFilter.TRIAL -> if (isDarkTheme()) DarkSoftPurple else SoftPurple
        HomeFilter.PAID -> if (isDarkTheme()) DarkSoftGreen else SoftGreen
    }
}

private fun recurrenceLabel(recurrence: Recurrence): String {
    return when (recurrence) {
        Recurrence.MONTHLY -> "Monthly"
        Recurrence.ANNUAL -> "Annual"
    }
}

private fun recurrenceAmountSuffix(recurrence: Recurrence): String {
    return when (recurrence) {
        Recurrence.MONTHLY -> "/month"
        Recurrence.ANNUAL -> "/year"
    }
}

private fun dateRangeLabel(startDate: java.time.LocalDate?, endDate: java.time.LocalDate?): String {
    return "${startDate?.format(DateRangeFormatter) ?: "—"} / ${endDate?.format(DateRangeFormatter) ?: "—"}"
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
    return "CHF ${String.format(Locale.US, "%.2f", amount)}"
}
