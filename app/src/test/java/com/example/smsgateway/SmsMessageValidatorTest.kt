package com.example.smsgateway

import com.example.smsgateway.platform.sms.SmsMessageValidator
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SmsMessageValidatorTest {

    @Test
    fun `isValidPhoneNumber accepts valid international and domestic numbers`() {
        assertThat(SmsMessageValidator.isValidPhoneNumber("+919876543210")).isTrue()
        assertThat(SmsMessageValidator.isValidPhoneNumber("9876543210")).isTrue()
        assertThat(SmsMessageValidator.isValidPhoneNumber("+14155552671")).isTrue()
        assertThat(SmsMessageValidator.isValidPhoneNumber("12345")).isTrue()
    }

    @Test
    fun `isValidPhoneNumber rejects invalid formats`() {
        assertThat(SmsMessageValidator.isValidPhoneNumber("")).isFalse()
        assertThat(SmsMessageValidator.isValidPhoneNumber("   ")).isFalse()
        assertThat(SmsMessageValidator.isValidPhoneNumber("abc12345")).isFalse()
        assertThat(SmsMessageValidator.isValidPhoneNumber("+91 9876 543210")).isFalse()
        assertThat(SmsMessageValidator.isValidPhoneNumber("+")).isFalse()
        assertThat(SmsMessageValidator.isValidPhoneNumber("123")).isFalse() // Too short
    }

    @Test
    fun `isAsciiOnly correctly identifies non-ASCII unicode characters`() {
        assertThat(SmsMessageValidator.isAsciiOnly("Hello World 123!")).isTrue()
        assertThat(SmsMessageValidator.isAsciiOnly("Hello \uD83D\uDE00")).isFalse() // Emoji
        assertThat(SmsMessageValidator.isAsciiOnly("नमस्ते")).isFalse() // Devanagari Hindi
    }

    @Test
    fun `calculatePartition correctly calculates GSM 7-bit parts`() {
        val shortAscii = "A".repeat(160)
        val shortResult = SmsMessageValidator.calculatePartition(shortAscii)
        assertThat(shortResult.partsCount).isEqualTo(1)
        assertThat(shortResult.isUnicode).isFalse()

        val multiAscii = "A".repeat(161)
        val multiResult = SmsMessageValidator.calculatePartition(multiAscii)
        assertThat(multiResult.partsCount).isEqualTo(2)
        assertThat(multiResult.charsPerPart).isEqualTo(153)
        assertThat(multiResult.isUnicode).isFalse()
    }

    @Test
    fun `calculatePartition correctly calculates Unicode UCS-2 parts`() {
        val shortUnicode = "नमस्ते " + "A".repeat(60) // <= 70 chars
        val shortResult = SmsMessageValidator.calculatePartition(shortUnicode)
        assertThat(shortResult.partsCount).isEqualTo(1)
        assertThat(shortResult.isUnicode).isTrue()

        val longUnicode = "नमस्ते " + "A".repeat(70) // > 70 chars
        val longResult = SmsMessageValidator.calculatePartition(longUnicode)
        assertThat(longResult.partsCount).isEqualTo(2)
        assertThat(longResult.charsPerPart).isEqualTo(67)
        assertThat(longResult.isUnicode).isTrue()
    }
}

