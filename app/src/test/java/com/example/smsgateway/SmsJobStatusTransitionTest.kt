package com.example.smsgateway

import com.example.smsgateway.domain.model.SmsJobStatus
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmsJobStatusTransitionTest {

    @Test
    fun `QUEUED can transition to FETCHED, SUBMITTING, and FAILED`() {
        assertThat(SmsJobStatus.QUEUED.canTransitionTo(SmsJobStatus.FETCHED)).isTrue()
        assertThat(SmsJobStatus.QUEUED.canTransitionTo(SmsJobStatus.SUBMITTING)).isTrue()
        assertThat(SmsJobStatus.QUEUED.canTransitionTo(SmsJobStatus.FAILED)).isTrue()
    }

    @Test
    fun `QUEUED cannot transition directly to SUBMITTED, DELIVERED, or SUBMISSION_UNKNOWN`() {
        assertThat(SmsJobStatus.QUEUED.canTransitionTo(SmsJobStatus.SUBMITTED)).isFalse()
        assertThat(SmsJobStatus.QUEUED.canTransitionTo(SmsJobStatus.DELIVERED)).isFalse()
        assertThat(SmsJobStatus.QUEUED.canTransitionTo(SmsJobStatus.SUBMISSION_UNKNOWN)).isFalse()
    }

    @Test
    fun `FETCHED can transition to SUBMITTING and FAILED`() {
        assertThat(SmsJobStatus.FETCHED.canTransitionTo(SmsJobStatus.SUBMITTING)).isTrue()
        assertThat(SmsJobStatus.FETCHED.canTransitionTo(SmsJobStatus.FAILED)).isTrue()
    }

    @Test
    fun `FETCHED cannot transition back to QUEUED or forward directly to SUBMITTED or DELIVERED`() {
        assertThat(SmsJobStatus.FETCHED.canTransitionTo(SmsJobStatus.QUEUED)).isFalse()
        assertThat(SmsJobStatus.FETCHED.canTransitionTo(SmsJobStatus.SUBMITTED)).isFalse()
        assertThat(SmsJobStatus.FETCHED.canTransitionTo(SmsJobStatus.DELIVERED)).isFalse()
    }

    @Test
    fun `SUBMITTING can transition to SUBMITTED, SUBMISSION_UNKNOWN, and FAILED`() {
        assertThat(SmsJobStatus.SUBMITTING.canTransitionTo(SmsJobStatus.SUBMITTED)).isTrue()
        assertThat(SmsJobStatus.SUBMITTING.canTransitionTo(SmsJobStatus.SUBMISSION_UNKNOWN)).isTrue()
        assertThat(SmsJobStatus.SUBMITTING.canTransitionTo(SmsJobStatus.FAILED)).isTrue()
    }

    @Test
    fun `SUBMITTING cannot transition back to QUEUED or FETCHED`() {
        assertThat(SmsJobStatus.SUBMITTING.canTransitionTo(SmsJobStatus.QUEUED)).isFalse()
        assertThat(SmsJobStatus.SUBMITTING.canTransitionTo(SmsJobStatus.FETCHED)).isFalse()
    }

    @Test
    fun `SUBMITTED can transition to DELIVERED and FAILED`() {
        assertThat(SmsJobStatus.SUBMITTED.canTransitionTo(SmsJobStatus.DELIVERED)).isTrue()
        assertThat(SmsJobStatus.SUBMITTED.canTransitionTo(SmsJobStatus.FAILED)).isTrue()
    }

    @Test
    fun `SUBMITTED cannot transition back to SUBMITTING or QUEUED`() {
        assertThat(SmsJobStatus.SUBMITTED.canTransitionTo(SmsJobStatus.SUBMITTING)).isFalse()
        assertThat(SmsJobStatus.SUBMITTED.canTransitionTo(SmsJobStatus.QUEUED)).isFalse()
    }

    @Test
    fun `SUBMISSION_UNKNOWN cannot blind retry to SUBMITTING without reconciliation`() {
        assertThat(SmsJobStatus.SUBMISSION_UNKNOWN.canTransitionTo(SmsJobStatus.SUBMITTING)).isFalse()
        assertThat(SmsJobStatus.SUBMISSION_UNKNOWN.canTransitionTo(SmsJobStatus.QUEUED)).isFalse()
    }

    @Test
    fun `SUBMISSION_UNKNOWN can be reconciled to SUBMITTED, DELIVERED, or FAILED`() {
        assertThat(SmsJobStatus.SUBMISSION_UNKNOWN.canTransitionTo(SmsJobStatus.SUBMITTED)).isTrue()
        assertThat(SmsJobStatus.SUBMISSION_UNKNOWN.canTransitionTo(SmsJobStatus.DELIVERED)).isTrue()
        assertThat(SmsJobStatus.SUBMISSION_UNKNOWN.canTransitionTo(SmsJobStatus.FAILED)).isTrue()
    }

    @Test
    fun `DELIVERED is a terminal state and cannot transition to anything else`() {
        assertThat(SmsJobStatus.DELIVERED.isTerminal).isTrue()
        for (status in SmsJobStatus.entries) {
            if (status != SmsJobStatus.DELIVERED) {
                assertThat(SmsJobStatus.DELIVERED.canTransitionTo(status)).isFalse()
            }
        }
    }

    @Test
    fun `FAILED is a terminal state and cannot transition to anything else`() {
        assertThat(SmsJobStatus.FAILED.isTerminal).isTrue()
        for (status in SmsJobStatus.entries) {
            if (status != SmsJobStatus.FAILED) {
                assertThat(SmsJobStatus.FAILED.canTransitionTo(status)).isFalse()
            }
        }
    }

    @Test
    fun `every status can idempotently transition to itself`() {
        for (status in SmsJobStatus.entries) {
            assertThat(status.canTransitionTo(status)).isTrue()
        }
    }
}

