package com.example.testapp004.model

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
)
