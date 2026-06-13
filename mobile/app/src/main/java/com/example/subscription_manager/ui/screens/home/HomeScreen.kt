@file:SuppressLint("NewApi")
package com.example.subscription_manager.ui.screens.home

import android.annotation.SuppressLint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
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
import com.example.subscription_manager.ui.theme.SoftGreen
import com.example.subscription_manager.ui.theme.SoftOrange
import java.time.format.DateTimeFormatter

// Add these missing definitions to match the UI style
private val DeepOrange = Color(0xFFE69A59)
private val DeepGreen = Color(0xFF65B38B)

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
        containerColor = Color(0xFFF9F9F9),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Subscriptions", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
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
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Using the pre-calculated total if available, otherwise sum it here
                val total = state.items.sumOf { it.subscription.amount }
                DashboardHeader(total)
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
private fun DashboardHeader(total: Double) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Total Monthly Spend", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Text(
            text = "$${"%.2f".format(total)}",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black
        )
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
    val isOverdue = item.status == PaymentStatus.OVERDUE || item.status == PaymentStatus.DUE_SOON

    Surface(
        onClick = onClick,
        color = when {
            isPaid -> SoftGreen
            isOverdue -> SoftOrange
            else -> Color.White
        },
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(text = item.subscription.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    // Using your new formattedAmount field
                    Text(text = "$${item.formattedAmount} / mo", style = MaterialTheme.typography.bodyMedium, color = Color.DarkGray)
                }
                StatusBadge(
                    label = item.status.name.lowercase().replace("_", " "),
                    color = if (isPaid) DeepGreen else DeepOrange,
                    isPaid = isPaid
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = item.subscription.nextPaymentDate.format(DateTimeFormatter.ofPattern("MMM dd")), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (!isPaid) {
                    IconButton(onClick = onMarkPaid) { Icon(Icons.Default.CheckCircle, contentDescription = "Mark Paid", tint = DeepOrange) }
                } else {
                    Switch(checked = item.subscription.renewalEnabled, onCheckedChange = { onToggleRenewal() })
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(label: String, color: Color, isPaid: Boolean) {
    Surface(color = Color.White.copy(alpha = 0.6f), shape = CircleShape, border = BorderStroke(1.dp, color.copy(alpha = 0.3f))) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isPaid) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp), tint = color)
                Spacer(Modifier.width(4.dp))
            }
            Text(label.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelSmall, color = color, fontWeight = FontWeight.Bold)
        }
    }
}