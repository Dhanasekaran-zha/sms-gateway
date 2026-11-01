package com.example.smsgateway.platform.sms

object SmsMessageValidator {

    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val trimmed = phoneNumber.trim()
        if (trimmed.length < 5 || trimmed.length > 20) return false
        // Basic international or national format: optional '+' followed by digits
        val regex = Regex("^\\+?[0-9]{5,19}$")
        return regex.matches(trimmed)
    }

    fun isAsciiOnly(text: String): Boolean {
        for (char in text) {
            if (char.code > 127) return false
        }
        return true
    }

    data class MessagePartition(
        val partsCount: Int,
        val charsPerPart: Int,
        val isUnicode: Boolean,
    )

    fun calculatePartition(text: String): MessagePartition {
        if (text.isEmpty()) {
            return MessagePartition(partsCount = 1, charsPerPart = 160, isUnicode = false)
        }
        val isUnicode = !isAsciiOnly(text)
        return if (isUnicode) {
            val length = text.length
            val count = if (length <= 70) 1 else ((length - 1) / 67) + 1
            MessagePartition(partsCount = count, charsPerPart = if (count == 1) 70 else 67, isUnicode = true)
        } else {
            val length = text.length
            val count = if (length <= 160) 1 else ((length - 1) / 153) + 1
            MessagePartition(partsCount = count, charsPerPart = if (count == 1) 160 else 153, isUnicode = false)
        }
    }
}

