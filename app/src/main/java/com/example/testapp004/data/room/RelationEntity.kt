package com.example.testapp004.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "relations",
    foreignKeys = [
        ForeignKey(
            entity = AcquaintanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = AcquaintanceEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("from_id"), Index("to_id")],
)
data class RelationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "from_id") val fromId: Long,
    @ColumnInfo(name = "to_id") val toId: Long,
    val label: String,
)
