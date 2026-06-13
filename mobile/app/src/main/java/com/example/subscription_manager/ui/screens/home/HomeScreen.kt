@file:SuppressLint("NewApi")
package com.example.subscription_manager.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.subscription_manager.ui.theme.Orange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscription_manager.domain.model.HomeSubscriptionItem
import com.example.subscription_manager.domain.model.PaymentStatus
import com.example.subscription_manager.domain.model.SortBucket
import java.time.format.DateTimeFormatter

private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")

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
        topBar = {
            TopAppBar(
                title = { Text(text = "Subscriptions") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddSubscription) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add subscription"
                )
            }
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(paddingValues)
                        .fillMaxWidth()
                        .wrapContentSize(Alignment.Center)
                )
            }

            state.items.isEmpty() -> {
                EmptySubscriptions(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(24.dp)
                )
            }

            else -> {
                SubscriptionList(
                    items = state.items,
                    modifier = Modifier.padding(paddingValues),
                    onMarkPaid = viewModel::markPaid,
                    onToggleRenewal = viewModel::toggleRenewal,
                    onEdit = onEditSubscription
                )
            }
        }
    }
}

@Composable
private fun SubscriptionList(
    items: List<HomeSubscriptionItem>,
    modifier: Modifier = Modifier,
    onMarkPaid: (Long) -> Unit,
    onToggleRenewal: (Long) -> Unit,
    onEdit: (Long) -> Unit
) {
    var previousBucket: SortBucket? = null

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = items,
            key = { it.subscription.id }
        ) { item ->
            if (item.sortBucket != previousBucket) {
                SectionHeader(title = item.sortBucket.title)
                previousBucket = item.sortBucket
            }
            SubscriptionCard(
                item = item,
                modifier = Modifier.animateItem()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!item.subscription.isPaid) {
                        Button(
                            onClick = { onMarkPaid(item.subscription.id) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Mark paid")
                        }
                    }
                    OutlinedButton(
                        onClick = { onToggleRenewal(item.subscription.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (item.subscription.renewalEnabled) {
                                "Renewal on"
                            } else {
                                "Renewal off"
                            }
                        )
                    }
                    OutlinedButton(
                        onClick = { onEdit(item.subscription.id) }
                    ) {
                        Text(text = "Edit")
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionCard(
    item: HomeSubscriptionItem,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit
) {
    val statusColor = item.status.color()
    val renewalText = if (item.subscription.willRenew) "Renews" else "No renewal"

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.subscription.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.subscription.type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = item.status.label,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Next payment",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = item.subscription.nextPaymentDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = renewalText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            item.subscription.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            actions()
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun EmptySubscriptions(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "No subscriptions yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Add a subscription or trial to start tracking upcoming payments locally.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val SortBucket.title: String
    get() = when (this) {
        SortBucket.UNPAID_DUE_THIS_MONTH -> "Due now"
        SortBucket.UPCOMING -> "Upcoming"
        SortBucket.PAID -> "Paid"
    }

private val PaymentStatus.label: String
    get() = when (this) {
        PaymentStatus.PAID -> "Paid"
        PaymentStatus.OVERDUE -> "Overdue"
        PaymentStatus.DUE_SOON -> "Due soon"
        PaymentStatus.UPCOMING -> "Upcoming"
    }

@Composable
private fun PaymentStatus.color(): Color {
    return when (this) {
        PaymentStatus.PAID -> MaterialTheme.colorScheme.primary
        PaymentStatus.OVERDUE -> MaterialTheme.colorScheme.error
        PaymentStatus.DUE_SOON -> Orange
        PaymentStatus.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
