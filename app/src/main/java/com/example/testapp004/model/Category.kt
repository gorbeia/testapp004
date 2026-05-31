package com.example.testapp004.model

data class Category(
    val id: Long,
    val name: String,
    val parentId: Long? = null,
)
