package com.example.testapp004.data.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "acquaintances")
data class AcquaintanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val bio: String,
    @ColumnInfo(name = "android_contact_lookup_key") val androidContactLookupKey: String?,
    val birthday: String?,
    @ColumnInfo(name = "is_deceased") val isDeceased: Boolean = false,
)
