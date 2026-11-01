package com.example.smsgateway.di

import android.content.Context
import androidx.room.Room
import com.example.smsgateway.data.local.AppDatabase
import com.example.smsgateway.data.local.dao.SmsJobDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME,
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSmsJobDao(database: AppDatabase): SmsJobDao {
        return database.smsJobDao()
    }
}

