package com.example.smsgateway.domain.model

enum class SmsJobStatus {
    QUEUED,
    FETCHED,
    SUBMITTING,
    SUBMITTED,
    DELIVERED,
    FAILED,
    SUBMISSION_UNKNOWN;

    fun canTransitionTo(next: SmsJobStatus): Boolean {
        if (this == next) return true // Idempotent no-op
        return when (this) {
            QUEUED -> next in setOf(FETCHED, SUBMITTING, FAILED)
            FETCHED -> next in setOf(SUBMITTING, FAILED)
            SUBMITTING -> next in setOf(SUBMITTED, SUBMISSION_UNKNOWN, FAILED)
            SUBMITTED -> next in setOf(DELIVERED, FAILED)
            SUBMISSION_UNKNOWN -> next in setOf(SUBMITTED, DELIVERED, FAILED)
            DELIVERED -> false // Terminal success
            FAILED -> false    // Terminal failure
        }
    }

    val isTerminal: Boolean
        get() = this == DELIVERED || this == FAILED

    val isAmbiguous: Boolean
        get() = this == SUBMISSION_UNKNOWN
}

