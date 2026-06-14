package com.example.subscription_manager.ui.screens.subscription

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.subscription_manager.domain.model.SubscriptionForm
import com.example.subscription_manager.domain.model.SubscriptionType
import com.example.subscription_manager.domain.utils.DateCalculator
import com.example.subscription_manager.ui.theme.DeepBlue
import com.example.subscription_manager.ui.theme.DeepGreen
import com.example.subscription_manager.ui.theme.DeepOrange
import com.example.subscription_manager.ui.theme.SoftBlue
import com.example.subscription_manager.ui.theme.SoftGreen
import com.example.subscription_manager.ui.theme.SoftOrange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

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
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var amountInput by remember(form.id) {
        mutableStateOf(form.amount.takeIf { it > 0.0 }?.toString() ?: "")
    }
    val validationMessage = validateForm(form)
    val canSave = validationMessage == null
    val nextPaymentDate = DateCalculator.nextPaymentDate(
        paymentDay = form.paymentDay,
        paymentMonth = form.paymentMonth,
        recurrence = form.recurrence,
        endDate = form.endDate
    )

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
        containerColor = ScreenBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (subscriptionId == null) "Add subscription" else "Edit subscription",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = viewModel::save
                    ) {
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
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                FormCard {
                    Text("Loading subscription…", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                validationMessage?.let { message ->
                    FormCard(
                        color = SoftOrange,
                        border = BorderStroke(1.dp, DeepOrange.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = message,
                            color = DeepOrange,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                FormCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(
                            text = "Subscription details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedTextField(
                            value = form.name,
                            onValueChange = { viewModel.updateForm { copy(name = it) } },
                            label = { Text("Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = SmallShape
                        )

                        FormSectionTitle("Type")
                        ChoiceRow(
                            options = SubscriptionType.entries.map { it.displayName },
                            selectedIndex = SubscriptionType.entries.indexOf(form.type),
                            onOptionSelected = { viewModel.updateForm { copy(type = SubscriptionType.entries[it]) } }
                        )

                        OutlinedTextField(
                            value = form.notes,
                            onValueChange = { viewModel.updateForm { copy(notes = it) } },
                            label = { Text("Notes") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = SmallShape
                        )
                    }
                }

                FormCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Amount",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatAmount(form.amount),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }

                        OutlinedTextField(
                            value = amountInput,
                            onValueChange = { value ->
                                amountInput = value
                                viewModel.updateForm { copy(amount = value.trim().toDoubleOrNull() ?: 0.0) }
                            },
                            label = { Text("Subscription cost") },
                            singleLine = true,
                            leadingIcon = { Text("$", color = MutedText) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = SmallShape
                        )

                        FormSectionTitle("Recurrence")
                        ChoiceRow(
                            options = Recurrence.entries.map { it.displayName },
                            selectedIndex = Recurrence.entries.indexOf(form.recurrence),
                            onOptionSelected = { viewModel.updateForm { copy(recurrence = Recurrence.entries[it]) } }
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
                            Text(
                                text = "Annual total: ${formatAmount(form.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MutedText
                            )
                        }

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
                    }
                }

                FormCard {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Next payment",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = nextPaymentDate.format(AmountFormatter),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            StatusChip(
                                text = recurrenceFrequencyLabel(form.recurrence),
                                color = DeepBlue,
                                backgroundColor = SoftBlue
                            )
                        }

                        SwitchRow(
                            label = "Renewal enabled",
                            description = "Turn off to keep this subscription visible without scheduling future payments.",
                            checked = form.renewalEnabled,
                            onCheckedChange = { viewModel.updateForm { copy(renewalEnabled = it) } }
                        )
                    }
                }

                if (subscriptionId != null) {
                    OutlinedButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = SmallShape
                    ) {
                        Text("Delete subscription")
                    }
                }

                Button(
                    enabled = canSave,
                    onClick = viewModel::save,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = SmallShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black,
                        disabledContainerColor = Color.Black.copy(alpha = 0.24f)
                    )
                ) {
                    Text(
                        text = if (subscriptionId == null) "Add subscription" else "Save changes",
                        fontWeight = FontWeight.Bold
                    )
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
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}

@Composable
private fun FormCard(
    color: Color = CardBackground,
    border: BorderStroke? = null,
    content: @Composable () -> Unit
) {
    Surface(
        color = color,
        shape = CardShape,
        border = border,
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
private fun ChoiceRow(
    options: List<String>,
    selectedIndex: Int,
    onOptionSelected: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, option ->
            val selected = index == selectedIndex
            FilterChip(
                selected = selected,
                onClick = { onOptionSelected(index) },
                label = { Text(option) },
                border = BorderStroke(
                    1.dp,
                    if (selected) Color.Transparent else DividerColor
                ),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Color.Black,
                    selectedLabelColor = Color.White
                ),
                shape = SmallShape
            )
        }
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
            modifier = Modifier.weight(1f),
            shape = SmallShape
        ) {
            Text(
                text = if (date == null) "Select $label" else "$label: ${date.format(AmountFormatter)}",
                maxLines = 1
            )
        }
        if (date != null) {
            OutlinedButton(
                onClick = onClear,
                shape = SmallShape
            ) {
                Text("Clear")
            }
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                color = MutedText
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                activeTrackColor = Color.Black,
                inactiveTrackColor = DividerColor,
                thumbColor = Color.Black
            )
        )
    }
}

@Composable
private fun SwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MutedText
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color.Black,
                checkedBorderColor = Color.Black,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = DividerColor,
                uncheckedBorderColor = DividerColor
            )
        )
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
    backgroundColor: Color
) {
    Surface(
        color = backgroundColor,
        shape = SmallShape,
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            color = color,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
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
                Text("Cancel")
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm
            ) {
                Text("Delete")
            }
        }
    )
}

private fun validateForm(form: SubscriptionForm): String? {
    if (form.name.trim().isBlank()) {
        return "Name is required."
    }
    if (form.amount <= 0.0) {
        return "Amount must be greater than 0."
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

private fun formatAmount(amount: Double): String {
    return "$${String.format(Locale.US, "%.2f", amount)}"
}

private fun recurrenceFrequencyLabel(recurrence: Recurrence): String {
    return when (recurrence) {
        Recurrence.MONTHLY -> "Monthly"
        Recurrence.ANNUAL -> "Annual"
    }
}

private fun LocalDate.toEpochMillis(): Long {
    return atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun Long.toLocalDate(): LocalDate {
    return Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
