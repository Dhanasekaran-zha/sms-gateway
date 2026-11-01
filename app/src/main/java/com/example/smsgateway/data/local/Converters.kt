package com.example.smsgateway.data.local

import androidx.room.TypeConverter
import com.example.smsgateway.domain.model.JobSource
import com.example.smsgateway.domain.model.SmsJobStatus

class Converters {
    @TypeConverter
    fun fromStatus(status: SmsJobStatus?): String? = status?.name

    @TypeConverter
    fun toStatus(value: String?): SmsJobStatus? = value?.let { SmsJobStatus.valueOf(it) }

    @TypeConverter
    fun fromJobSource(source: JobSource?): String? = source?.name

    @TypeConverter
    fun toJobSource(value: String?): JobSource? = value?.let { JobSource.valueOf(it) }
}

