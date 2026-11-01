package com.example.smsgateway.di

import com.example.smsgateway.platform.sms.AndroidSmsSender
import com.example.smsgateway.platform.sms.SmsSender
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SmsModule {

    @Binds
    @Singleton
    abstract fun bindSmsSender(
        impl: AndroidSmsSender,
    ): SmsSender
}

