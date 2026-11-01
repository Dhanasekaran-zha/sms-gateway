package com.example.smsgateway.data.local.entity

import com.example.smsgateway.domain.model.SmsJob
import com.example.smsgateway.domain.model.StatusHistoryEntry

fun SmsJobEntity.toDomain(): SmsJob =
    SmsJob(
        jobId = jobId,
        recipient = recipient,
        messageBody = messageBody,
        simSlot = simSlot,
        subscriptionId = subscriptionId,
        status = status,
        source = source,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sentAt = sentAt,
        deliveredAt = deliveredAt,
        errorMessage = errorMessage,
        retryCount = retryCount,
    )

fun SmsJob.toEntity(): SmsJobEntity =
    SmsJobEntity(
        jobId = jobId,
        recipient = recipient,
        messageBody = messageBody,
        simSlot = simSlot,
        subscriptionId = subscriptionId,
        status = status,
        source = source,
        createdAt = createdAt,
        updatedAt = updatedAt,
        sentAt = sentAt,
        deliveredAt = deliveredAt,
        errorMessage = errorMessage,
        retryCount = retryCount,
    )

fun StatusHistoryEntity.toDomain(): StatusHistoryEntry =
    StatusHistoryEntry(
        id = id,
        jobId = jobId,
        fromStatus = fromStatus,
        toStatus = toStatus,
        timestamp = timestamp,
        detail = detail,
    )

fun StatusHistoryEntry.toEntity(): StatusHistoryEntity =
    StatusHistoryEntity(
        id = id,
        jobId = jobId,
        fromStatus = fromStatus,
        toStatus = toStatus,
        timestamp = timestamp,
        detail = detail,
    )

