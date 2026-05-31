package com.example.testapp004.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "acquaintance_categories",
    primaryKeys = ["acquaintance_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = AcquaintanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["acquaintance_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("acquaintance_id"), Index("category_id")],
)
data class AcquaintanceCategoryCrossRef(
    @ColumnInfo(name = "acquaintance_id") val acquaintanceId: Long,
    @ColumnInfo(name = "category_id") val categoryId: Long,
)
