package com.example.testapp004.data.room

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class AcquaintanceWithCategories(
    @Embedded val acquaintance: AcquaintanceEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = AcquaintanceCategoryCrossRef::class,
            parentColumn = "acquaintance_id",
            entityColumn = "category_id",
        ),
    )
    val categories: List<CategoryEntity>,
)
