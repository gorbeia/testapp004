package com.example.testapp004.model

data class Category(
    val id: Long,
    val name: String,
    val parentId: Long? = null,
    val sortOrder: Int = 0,
)

fun List<Category>.descendantsAndSelf(id: Long): Set<Long> {
    val result = mutableSetOf(id)
    val queue = ArrayDeque<Long>()
    queue.add(id)
    while (queue.isNotEmpty()) {
        val current = queue.removeFirst()
        filter { it.parentId == current }.forEach { child ->
            if (result.add(child.id)) queue.add(child.id)
        }
    }
    return result
}
