package com.example.smsgateway.domain.model

class DuplicateJobException(val jobId: String) :
    IllegalStateException("SMS Job with ID '$jobId' already exists")

class InvalidStateTransitionException(val from: SmsJobStatus, val to: SmsJobStatus) :
    IllegalStateException("Cannot transition SMS job status from $from to $to")

class JobNotFoundException(val jobId: String) :
    IllegalArgumentException("SMS Job with ID '$jobId' was not found")

