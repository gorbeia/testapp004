package com.example.testapp004.model

data class Acquaintance(
    val id: Long,
    val name: String,
    val bio: String = "",
    val categoryIds: Set<Long> = emptySet(),
    val androidContactLookupKey: String? = null,
)
