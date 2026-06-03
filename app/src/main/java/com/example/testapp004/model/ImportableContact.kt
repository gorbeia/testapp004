package com.example.testapp004.model

data class ImportableContact(
    val lookupKey: String,
    val displayName: String,
    val birthday: String?,
    val organizationInfo: String?,
    val notes: String?,
)
