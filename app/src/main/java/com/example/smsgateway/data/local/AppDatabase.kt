package com.example.smsgateway.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.smsgateway.data.local.dao.SmsJobDao
import com.example.smsgateway.data.local.entity.SmsJobEntity
import com.example.smsgateway.data.local.entity.StatusHistoryEntity

@Database(
    entities = [
        SmsJobEntity::class,
        StatusHistoryEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun smsJobDao(): SmsJobDao

    companion object {
        const val DATABASE_NAME = "sms_gateway.db"
    }
}

