package com.example.subscription_manager.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscription_manager.domain.model.Recurrence
import com.example.subscription_manager.domain.model.SubscriptionType
import com.example.subscription_manager.ui.theme.DeepBlue
import com.example.subscription_manager.ui.theme.DeepOrange
import com.example.subscription_manager.ui.theme.SoftBlue
import com.example.subscription_manager.ui.theme.SoftOrange
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ScreenBackground = Color(0xFFF9F9F9)
private val CardBackground = Color.White
private val DividerColor = Color(0xFFE5E7EB)
private val MutedText = Color(0xFF6B7280)
private val CardShape = RoundedCornerShape(24.dp)
private val SmallShape = RoundedCornerShape(16.dp)
private val AmountFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    subscriptionId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val form = state.form

    LaunchedEffect(Unit) {
        viewModel.navigateBack.collect {
            onNavigateBack()
        }
    }

    // UI State for pickers
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Amount formatting
    var amountInput by remember(form.id) {
        mutableStateOf(form.amount.takeIf { it > 0.0 }?.toString() ?: "")
    }

    // Existing start date picker
    if (showStartDatePicker) {
        SubscriptionDatePicker(
            initialDate = form.startDate,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = {
                viewModel.updateForm { copy(startDate = it) }
                showStartDatePicker = false
            }
        )
    }

    // ADD THIS BLOCK for the end date picker
    if (showEndDatePicker) {
        SubscriptionDatePicker(
            initialDate = form.endDate,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = {
                viewModel.updateForm { copy(endDate = it) }
                showEndDatePicker = false
            }
        )
    }

    Scaffold(
        containerColor = ScreenBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (subscriptionId == null) "Add" else "Edit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = viewModel::save, enabled = validateForm(form, amountInput) == null) {
                        Text("Save")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenBackground)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Input Section ---
            FormCard {
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { viewModel.updateForm { copy(name = it) } },
                    label = { Text("Subscription Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountInput,
                    onValueChange = {
                        amountInput = it
                        viewModel.updateForm { copy(amount = it.toDoubleOrNull() ?: 0.0) }
                    },
                    label = { Text("Monthly Amount") },
                    prefix = { Text("CHF ") },
                    isError = amountValidationError(amountInput) != null,
                    supportingText = {
                        val amountError = amountValidationError(amountInput)
                        if (amountError != null) {
                            Text(amountError)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Replaced Slider with cleaner Number Input
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    val maxDaysInMonth = YearMonth.of(LocalDate.now().year, form.paymentMonth.coerceIn(1, 12)).lengthOfMonth()

                    // 2. Apply this to your Day TextField
                    OutlinedTextField(
                        value = form.paymentDay.toString(),
                        onValueChange = { input ->
                            val numericInput = input.filter { it.isDigit() }.toIntOrNull()

                            // Clamp based on the dynamic maxDaysInMonth (e.g., 28 for Feb)
                            val clampedValue = numericInput?.coerceIn(1, maxDaysInMonth) ?: 1

                            viewModel.updateForm { copy(paymentDay = clampedValue) }
                        },
                        label = { Text("Day (1-$maxDaysInMonth)") }, // Displays dynamic limit to user
                        modifier = Modifier.weight(1f)
                    )

                    if (form.recurrence == Recurrence.ANNUAL) {
                        OutlinedTextField(
                            value = form.paymentMonth.toString(),
                            onValueChange = { input ->
                                // 1. Only allow numeric input
                                val numericInput = input.filter { it.isDigit() }.toIntOrNull()

                                // 2. Clamp the value between 1 and 31
                                val clampedValue = numericInput?.coerceIn(1, 12) ?: 1

                                viewModel.updateForm { copy(paymentMonth = clampedValue) }
                            },
                            label = { Text("Month") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // --- Selection Section ---
            FormCard {
                FormSectionTitle("Recurrence Type")
                ChoiceRow(
                    options = Recurrence.entries.map { it.displayName },
                    selectedIndex = Recurrence.entries.indexOf(form.recurrence),
                    onOptionSelected = { viewModel.updateForm { copy(recurrence = Recurrence.entries[it]) } }
                )

                DateSelectionRow("Start Date", form.startDate, { showStartDatePicker = true }, { viewModel.updateForm { copy(startDate = null) } })
                DateSelectionRow("End Date", form.endDate, { showEndDatePicker = true }, { viewModel.updateForm { copy(endDate = null) } })
            }

            // --- Toggle Section ---
            FormCard {
                SwitchRow(
                    label = "Renewal Enabled",
                    description = "Keep active in dashboard",
                    checked = form.renewalEnabled,
                    onCheckedChange = { viewModel.updateForm { copy(renewalEnabled = it) } }
                )
            }

            if (subscriptionId != null) {
                OutlinedButton(onClick = { showDeleteDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Delete Subscription", color = Color.Red)
                }
            }
        }
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Surface(
        color = CardBackground,
        shape = CardShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun FormSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MutedText,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ChoiceRow(options: List<String>, selectedIndex: Int, onOptionSelected: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, option ->
            FilterChip(
                selected = index == selectedIndex,
                onClick = { onOptionSelected(index) },
                label = { Text(option) },
                shape = SmallShape
            )
        }
    }
}

@Composable
private fun DateSelectionRow(label: String, date: LocalDate?, onSelect: () -> Unit, onClear: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = onSelect, modifier = Modifier.weight(1f)) {
            Text(text = date?.format(AmountFormatter) ?: "Select $label")
        }
        if (date != null) {
            TextButton(onClick = onClear) { Text("Clear") }
        }
    }
}

@Composable
private fun SwitchRow(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MutedText)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun validateForm(
    form: com.example.subscription_manager.domain.model.SubscriptionForm,
    amountInput: String
): String? {
    return form.name.takeIf { it.isBlank() }
        ?.let { "Name is required" }
        ?: amountValidationError(amountInput)
}

private fun amountValidationError(amountInput: String): String? {
    val amount = amountInput.toDoubleOrNull()
    return when {
        amountInput.isBlank() -> "Amount is required"
        amount == null -> "Enter a valid amount"
        amount <= 0.0 -> "Amount must be greater than 0"
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionDatePicker(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                datePickerState.selectedDateMillis?.let {
                    onDateSelected(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DatePicker(state = datePickerState)
    }
}
