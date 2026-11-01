package com.example.smsgateway.di

import com.example.smsgateway.data.backend.StubBackendJobFetcher
import com.example.smsgateway.domain.backend.BackendJobFetcher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BackendModule {

    @Binds
    @Singleton
    abstract fun bindBackendJobFetcher(
        impl: StubBackendJobFetcher,
    ): BackendJobFetcher
}

