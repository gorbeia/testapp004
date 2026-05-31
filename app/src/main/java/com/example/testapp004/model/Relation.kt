package com.example.testapp004.model

data class Relation(
    val id: Long,
    val fromId: Long,
    val toId: Long,
    val label: String,
)
