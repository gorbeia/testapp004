package com.example.testapp004.data.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AcquaintanceEntity::class,
        CategoryEntity::class,
        RelationEntity::class,
        AcquaintanceCategoryCrossRef::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun acquaintanceDao(): AcquaintanceDao

    abstract fun categoryDao(): CategoryDao

    abstract fun relationDao(): RelationDao
}
