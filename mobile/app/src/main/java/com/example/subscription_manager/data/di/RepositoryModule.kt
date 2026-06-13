package com.example.subscription_manager.data.di

import com.example.subscription_manager.data.repository.SubscriptionRepositoryImpl
import com.example.subscription_manager.domain.repository.SubscriptionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(
        implementation: SubscriptionRepositoryImpl
    ): SubscriptionRepository
}
