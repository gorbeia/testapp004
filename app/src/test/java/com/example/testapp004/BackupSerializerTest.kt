package com.example.testapp004

import com.example.testapp004.data.BackupSerializer
import com.example.testapp004.data.BackupSnapshot
import com.example.testapp004.data.InvalidBackupException
import com.example.testapp004.data.room.AcquaintanceCategoryCrossRef
import com.example.testapp004.data.room.AcquaintanceEntity
import com.example.testapp004.data.room.CategoryEntity
import com.example.testapp004.data.room.RelationEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class BackupSerializerTest {
    private val snapshot = BackupSnapshot(
        schemaVersion = 5,
        createdAt = 1234L,
        acquaintances = listOf(
            AcquaintanceEntity(1, "Ann", "bio \"quoted\"\nline", "key", "1990-01-02", true),
            AcquaintanceEntity(2, "Bob", "", null, null, false),
        ),
        categories = listOf(
            CategoryEntity(1, "Family", null, 0),
            CategoryEntity(2, "Cousins", 1, 3),
        ),
        relations = listOf(
            RelationEntity(1, 1, 2, "CUSTOM", "friend"),
            RelationEntity(2, 2, 1, "PARENT", null),
        ),
        crossRefs = listOf(AcquaintanceCategoryCrossRef(1, 2)),
    )

    @Test
    fun `round trip preserves all data`() {
        assertEquals(snapshot, BackupSerializer.fromJson(BackupSerializer.toJson(snapshot)))
    }

    @Test
    fun `empty snapshot round trips`() {
        val empty = BackupSnapshot(5, 0L, emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(empty, BackupSerializer.fromJson(BackupSerializer.toJson(empty)))
    }

    @Test
    fun `missing optional fields from older backups use defaults`() {
        val json = """
            {"format":1,"schemaVersion":3,"createdAt":1,
             "acquaintances":[{"id":1,"name":"Ann"}],
             "categories":[{"id":1,"name":"Family"}],
             "relations":[],"crossRefs":[]}
        """.trimIndent()
        val result = BackupSerializer.fromJson(json)
        assertFalse(result.acquaintances.single().isDeceased)
        assertNull(result.acquaintances.single().birthday)
        assertEquals(0, result.categories.single().sortOrder)
        assertNull(result.categories.single().parentId)
    }

    @Test(expected = InvalidBackupException::class)
    fun `garbage input is rejected`() {
        BackupSerializer.fromJson("not json")
    }

    @Test(expected = InvalidBackupException::class)
    fun `missing required section is rejected`() {
        BackupSerializer.fromJson("""{"format":1,"schemaVersion":5,"createdAt":1}""")
    }

    @Test(expected = InvalidBackupException::class)
    fun `newer format is rejected`() {
        val json = BackupSerializer.toJson(snapshot).replace("\"format\":1", "\"format\":99")
        BackupSerializer.fromJson(json)
    }
}
