package com.example.subscription_manager.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.subscription_manager.data.entity.SubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions ORDER BY name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun observeById(id: Long): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getById(id: Long): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: SubscriptionEntity): Long

    @Delete
    suspend fun delete(entity: SubscriptionEntity)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        """
        UPDATE subscriptions
        SET is_paid = :isPaid,
            paid_cycle_key = :paidCycleKey,
            last_paid_at_epoch_millis = :timestamp,
            last_reminder_cycle_key = NULL,
            updated_at_epoch_millis = :timestamp
        WHERE id = :id
        """
    )
    suspend fun updatePaymentStatus(
        id: Long,
        isPaid: Boolean,
        paidCycleKey: String?,
        timestamp: Long
    )

    @Query("UPDATE subscriptions SET renewal_enabled = :renewalEnabled, updated_at_epoch_millis = :timestamp WHERE id = :id")
    suspend fun updateRenewal(id: Long, renewalEnabled: Boolean, timestamp: Long)

    @Query("UPDATE subscriptions SET last_reminder_cycle_key = :cycleKey WHERE id = :id")
    suspend fun updateLastReminderCycle(id: Long, cycleKey: String)
}
