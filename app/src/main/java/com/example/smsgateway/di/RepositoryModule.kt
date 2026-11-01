package com.example.smsgateway.di

import com.example.smsgateway.data.repository.SmsJobRepositoryImpl
import com.example.smsgateway.domain.repository.SmsJobRepository
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
    abstract fun bindSmsJobRepository(
        impl: SmsJobRepositoryImpl,
    ): SmsJobRepository
}

