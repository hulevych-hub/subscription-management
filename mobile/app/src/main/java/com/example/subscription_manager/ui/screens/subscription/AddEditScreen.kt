package com.example.subscription_manager.ui.screens.subscription

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.subscription_manager.domain.model.Recurrence
import com.example.subscription_manager.domain.model.SubscriptionForm
import com.example.subscription_manager.domain.model.SubscriptionType
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditScreen(
    subscriptionId: Long?,
    onNavigateBack: () -> Unit,
    viewModel: AddEditViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val form = state.form
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val validationMessage = validateForm(form)
    val canSave = validationMessage == null

    LaunchedEffect(subscriptionId) {
        viewModel.navigateBack.collect {
            onNavigateBack()
        }
    }

    if (showStartDatePicker) {
        SubscriptionDatePicker(
            initialDate = form.startDate,
            onDismiss = { showStartDatePicker = false },
            onDateSelected = { date ->
                viewModel.updateForm { copy(startDate = date) }
                showStartDatePicker = false
            }
        )
    }

    if (showEndDatePicker) {
        SubscriptionDatePicker(
            initialDate = form.endDate,
            onDismiss = { showEndDatePicker = false },
            onDateSelected = { date ->
                viewModel.updateForm { copy(endDate = date) }
                showEndDatePicker = false
            }
        )
    }

    if (showDeleteDialog && subscriptionId != null) {
        DeleteConfirmationDialog(
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                viewModel.delete()
                showDeleteDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (subscriptionId == null) "Add subscription" else "Edit subscription"
                    )
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = viewModel::save
                    ) {
                        Text(text = "Save")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (state.isLoading) {
                Text(text = "Loading subscription…")
            } else {
                validationMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                OutlinedTextField(
                    value = form.name,
                    onValueChange = { viewModel.updateForm { copy(name = it) } },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                FormSection(title = "Type") {
                    SubscriptionType.entries.forEach { type ->
                        FilterChip(
                            selected = form.type == type,
                            onClick = { viewModel.updateForm { copy(type = type) } },
                            label = { Text(type.displayName) }
                        )
                    }
                }

                FormSection(title = "Recurrence") {
                    Recurrence.entries.forEach { recurrence ->
                        FilterChip(
                            selected = form.recurrence == recurrence,
                            onClick = { viewModel.updateForm { copy(recurrence = recurrence) } },
                            label = { Text(recurrence.displayName) }
                        )
                    }
                }

                OutlinedTextField(
                    value = form.notes,
                    onValueChange = { viewModel.updateForm { copy(notes = it) } },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )

                DateSelectionRow(
                    label = "Start date",
                    date = form.startDate,
                    onSelect = { showStartDatePicker = true },
                    onClear = { viewModel.updateForm { copy(startDate = null) } }
                )

                DateSelectionRow(
                    label = "End date",
                    date = form.endDate,
                    onSelect = { showEndDatePicker = true },
                    onClear = { viewModel.updateForm { copy(endDate = null) } }
                )

                SliderField(
                    label = "Payment day",
                    value = form.paymentDay.toFloat(),
                    valueRange = 1f..31f,
                    steps = 29,
                    valueText = form.paymentDay.toString(),
                    onValueChange = { viewModel.updateForm { copy(paymentDay = it.roundToInt()) } }
                )

                if (form.recurrence == Recurrence.ANNUAL) {
                    SliderField(
                        label = "Payment month",
                        value = form.paymentMonth.toFloat(),
                        valueRange = 1f..12f,
                        steps = 10,
                        valueText = form.paymentMonth.toString(),
                        onValueChange = { viewModel.updateForm { copy(paymentMonth = it.roundToInt()) } }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Renewal enabled",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    FilterChip(
                        selected = form.renewalEnabled,
                        onClick = { viewModel.updateForm { copy(renewalEnabled = !renewalEnabled) } },
                        label = { Text(if (form.renewalEnabled) "On" else "Off") }
                    )
                }

                if (subscriptionId != null) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Delete subscription")
                    }
                }

                Button(
                    enabled = canSave,
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (subscriptionId == null) "Add subscription" else "Save changes")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubscriptionDatePicker(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val datePickerState: DatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.toEpochMillis()
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = datePickerState.selectedDateMillis != null,
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(millis.toLocalDate())
                    }
                }
            ) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun DateSelectionRow(
    label: String,
    date: LocalDate?,
    onSelect: () -> Unit,
    onClear: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onSelect,
            modifier = Modifier.weight(1f)
        ) {
            Text(text = if (date == null) "Select $label" else "$label: ${date.format(java.time.format.DateTimeFormatter.ofPattern("MMM d, yyyy"))}")
        }
        if (date != null) {
            OutlinedButton(onClick = onClear) {
                Text(text = "Clear")
            }
        }
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            content()
        }
    }
}

@Composable
private fun SliderField(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Delete subscription?") },
        text = { Text(text = "This removes the subscription and its reminders from this device.") },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Delete")
            }
        }
    )
}

private fun validateForm(form: SubscriptionForm): String? {
    if (form.name.trim().isBlank()) {
        return "Name is required."
    }
    if (form.paymentDay !in 1..31) {
        return "Payment day must be between 1 and 31."
    }
    if (form.paymentMonth !in 1..12) {
        return "Payment month must be between 1 and 12."
    }
    if (form.startDate != null && form.endDate != null && form.endDate < form.startDate) {
        return "End date must be on or after the start date."
    }
    return null
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
