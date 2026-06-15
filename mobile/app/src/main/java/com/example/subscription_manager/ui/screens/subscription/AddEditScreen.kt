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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscription_manager.domain.model.Recurrence
import com.example.subscription_manager.domain.model.SubscriptionType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

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
        containerColor = MaterialTheme.colorScheme.background,
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
                    label = { Text("Name") },
                    isError = form.name.isBlank(),
                    supportingText = {
                        if (form.name.isBlank()) {
                            Text("Name is required")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                FormSectionTitle("Type")
                ChoiceRow(
                    options = SubscriptionType.entries.map { it.displayName },
                    selectedIndex = SubscriptionType.entries.indexOf(form.type),
                    onOptionSelected = { viewModel.updateForm { copy(type = SubscriptionType.entries[it]) } }
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
                    val maxDaysInMonth = maxDaysForPaymentDay(form)

                    // 2. Apply this to your Day TextField
                    val dayError = dayValidationError(form.paymentDay, maxDaysInMonth)

                    OutlinedTextField(
                        value = form.paymentDay?.toString().orEmpty(),
                        onValueChange = { input ->
                            val numericInput = input.filter { it.isDigit() }.toIntOrNull()
                            viewModel.updateForm { copy(paymentDay = numericInput) }
                        },
                        label = { Text("Day (1-$maxDaysInMonth)") },
                        isError = dayError != null,
                        supportingText = {
                            if (dayError != null) {
                                Text(dayError)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    if (form.recurrence == Recurrence.ANNUAL) {
                        OutlinedTextField(
                            value = form.paymentMonth?.toString().orEmpty(),
                            onValueChange = { input ->
                                val numericInput = input.filter { it.isDigit() }.toIntOrNull()
                                val monthValue = numericInput?.coerceIn(1, 12)

                                viewModel.updateForm { copy(paymentMonth = monthValue) }
                            },
                            label = { Text("Month") },
                            isError = form.paymentMonth == null,
                            supportingText = {
                                if (form.paymentMonth == null) {
                                    Text("Month is required")
                                }
                            },
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
                    onOptionSelected = { selectedIndex ->
                        val recurrence = Recurrence.entries[selectedIndex]
                        viewModel.updateForm {
                            copy(
                                recurrence = recurrence,
                                paymentMonth = if (recurrence == Recurrence.MONTHLY) null else paymentMonth
                            )
                        }
                    }
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
                    Text("Delete Subscription", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Subscription") },
            text = { Text("This will permanently remove this subscription and its reminders.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    viewModel.delete()
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        ?: annualMonthValidationError(form)
        ?: dayValidationError(form.paymentDay, maxDaysForPaymentDay(form))
        ?: amountValidationError(amountInput)
}

private fun annualMonthValidationError(form: com.example.subscription_manager.domain.model.SubscriptionForm): String? {
    return form.recurrence.takeIf { it == Recurrence.ANNUAL }
        ?.takeIf { form.paymentMonth == null }
        ?.let { "Month is required" }
}

private fun maxDaysForPaymentDay(form: com.example.subscription_manager.domain.model.SubscriptionForm): Int {
    return if (form.recurrence == Recurrence.ANNUAL && form.paymentMonth != null) {
        YearMonth.of(LocalDate.now().year, form.paymentMonth).lengthOfMonth()
    } else {
        31
    }
}

private fun dayValidationError(paymentDay: Int?, maxDay: Int): String? {
    return when {
        paymentDay == null -> "Day is required"
        paymentDay !in 1..maxDay -> "Day must be between 1 and $maxDay"
        else -> null
    }
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
