package com.example.testapp004.model

data class Relation(
    val id: Long,
    val fromId: Long,
    val toId: Long,
    val typeKey: String,
    val customLabel: String? = null,
)

fun Relation.labelFor(currentPersonId: Long): String {
    if (typeKey == RelationTypes.CUSTOM_KEY) return customLabel.orEmpty()
    val type = RelationTypes.findByKey(typeKey) ?: return customLabel.orEmpty()
    return if (fromId == currentPersonId) type.fromLabel else type.toLabel
}
