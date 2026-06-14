package com.example.subscription_manager.data.repository

import com.example.subscription_manager.data.dao.SubscriptionDao
import com.example.subscription_manager.data.mapper.toDomain
import com.example.subscription_manager.data.mapper.toEntity
import com.example.subscription_manager.domain.model.Subscription
import com.example.subscription_manager.domain.model.SubscriptionForm
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import com.example.subscription_manager.domain.utils.DateCalculator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionDao: SubscriptionDao
) : SubscriptionRepository {

    override val subscriptions: Flow<List<Subscription>> = subscriptionDao.observeAll().map { entities ->
        entities.map { it.toDomain() }
    }

    override fun subscription(id: Long): Flow<Subscription?> {
        return subscriptionDao.observeById(id).map { it?.toDomain() }
    }

    override suspend fun saveSubscription(form: SubscriptionForm): Long {
        val now = System.currentTimeMillis()
        val existing = form.id.takeIf { it > 0L }?.let { subscriptionDao.getById(it) }
        val entity = form.toEntity(existing = existing, nowMillis = now)
        return subscriptionDao.upsert(entity)
    }

    override suspend fun deleteSubscription(id: Long) {
        subscriptionDao.deleteById(id)
    }

    override suspend fun markPaid(id: Long) {
        val entity = subscriptionDao.getById(id) ?: return
        val nextPaymentDate = DateCalculator.nextPaymentDate(
            paymentDay = entity.paymentDay,
            paymentMonth = entity.paymentMonth,
            recurrence = entity.recurrence,
            endDate = entity.endDateEpochDay?.let { java.time.LocalDate.ofEpochDay(it) }
        )
        val cycleKey = DateCalculator.cycleKeyForDate(nextPaymentDate, entity.recurrence)
        subscriptionDao.updatePaymentStatus(
            id = id,
            isPaid = true,
            paidCycleKey = cycleKey,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun markUnpaid(id: Long) {
        subscriptionDao.updatePaymentStatus(
            id = id,
            isPaid = false,
            paidCycleKey = null,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun toggleRenewal(id: Long) {
        val entity = subscriptionDao.getById(id) ?: return
        subscriptionDao.updateRenewal(
            id = id,
            renewalEnabled = !entity.renewalEnabled,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun markReminderSent(id: Long, cycleKey: String) {
        subscriptionDao.updateLastReminderCycle(id = id, cycleKey = cycleKey)
    }

    override suspend fun clearReminderSent(id: Long, cycleKey: String) {
        subscriptionDao.clearLastReminderCycle(id = id, cycleKey = cycleKey)
    }

    override suspend fun clearAllReminderSent() {
        subscriptionDao.clearAllLastReminderCycles()
    }
}
