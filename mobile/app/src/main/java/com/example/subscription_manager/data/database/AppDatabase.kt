package com.example.subscription_manager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.subscription_manager.data.dao.SubscriptionDao
import com.example.subscription_manager.data.entity.SubscriptionEntity

@Database(
    entities = [SubscriptionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(SubscriptionTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptionDao(): SubscriptionDao
}
