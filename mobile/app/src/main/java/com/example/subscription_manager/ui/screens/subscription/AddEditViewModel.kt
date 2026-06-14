package com.example.subscription_manager.ui.screens.subscription

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subscription_manager.domain.model.SubscriptionForm
import com.example.subscription_manager.domain.usecases.DeleteSubscriptionUseCase
import com.example.subscription_manager.domain.usecases.GetSubscriptionUseCase
import com.example.subscription_manager.domain.usecases.SaveSubscriptionUseCase
import com.example.subscription_manager.notification.NotificationScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditUiState(
    val form: SubscriptionForm = SubscriptionForm(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AddEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getSubscriptionUseCase: GetSubscriptionUseCase,
    private val saveSubscriptionUseCase: SaveSubscriptionUseCase,
    private val deleteSubscriptionUseCase: DeleteSubscriptionUseCase,
    private val notificationScheduler: NotificationScheduler
) : ViewModel() {

    private val subscriptionId = savedStateHandle.get<Long>("id")

    private val _state = MutableStateFlow(AddEditUiState())
    val state = _state.asStateFlow()

    private val _navigateBack = MutableSharedFlow<Unit>()
    val navigateBack = _navigateBack.asSharedFlow()

    init {
        subscriptionId?.let { loadSubscription(it) }
    }

    fun updateForm(update: SubscriptionForm.() -> SubscriptionForm) {
        _state.value = _state.value.copy(form = update(_state.value.form))
    }

    fun save() {
        viewModelScope.launch {
            val form = _state.value.form
            val savedId = saveSubscriptionUseCase(form.copy(id = subscriptionId ?: 0L))
            notificationScheduler.scheduleSubscription(savedId)
            _navigateBack.emit(Unit)
        }
    }

    fun delete() {
        val id = subscriptionId ?: return
        viewModelScope.launch {
            deleteSubscriptionUseCase(id)
            notificationScheduler.cancelSubscriptionReminders(id)
            _navigateBack.emit(Unit)
        }
    }

    private fun loadSubscription(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            getSubscriptionUseCase(id).collect { subscription ->
                subscription?.let {
                    _state.value = _state.value.copy(
                        form = SubscriptionForm(
                            id = it.id,
                            name = it.name,
                            amount = it.amount,
                            type = it.type,
                            notes = it.notes.orEmpty(),
                            startDate = it.startDate,
                            endDate = it.endDate,
                            paymentDay = it.paymentDay,
                            paymentMonth = it.paymentMonth,
                            recurrence = it.recurrence,
                            renewalEnabled = it.renewalEnabled
                        ),
                        isLoading = false
                    )
                }
            }
        }
    }
}
