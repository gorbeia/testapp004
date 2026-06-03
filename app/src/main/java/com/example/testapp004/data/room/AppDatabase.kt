package com.example.testapp004.data.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AcquaintanceEntity::class,
        CategoryEntity::class,
        RelationEntity::class,
        AcquaintanceCategoryCrossRef::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun acquaintanceDao(): AcquaintanceDao

    abstract fun categoryDao(): CategoryDao

    abstract fun relationDao(): RelationDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE acquaintances ADD COLUMN birthday TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE categories ADD COLUMN sort_order INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE relations_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        from_id INTEGER NOT NULL,
                        to_id INTEGER NOT NULL,
                        type_key TEXT NOT NULL,
                        custom_label TEXT,
                        FOREIGN KEY(from_id) REFERENCES acquaintances(id) ON DELETE CASCADE,
                        FOREIGN KEY(to_id) REFERENCES acquaintances(id) ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "INSERT INTO relations_new (id, from_id, to_id, type_key, custom_label) " +
                        "SELECT id, from_id, to_id, 'CUSTOM', label FROM relations",
                )
                db.execSQL("DROP TABLE relations")
                db.execSQL("ALTER TABLE relations_new RENAME TO relations")
                db.execSQL("CREATE INDEX index_relations_from_id ON relations(from_id)")
                db.execSQL("CREATE INDEX index_relations_to_id ON relations(to_id)")
            }
        }
    }
}
