package com.example.testapp004.data

import com.example.testapp004.data.room.AcquaintanceCategoryCrossRef
import com.example.testapp004.data.room.AcquaintanceEntity
import com.example.testapp004.data.room.CategoryEntity
import com.example.testapp004.data.room.RelationEntity
import org.json.JSONArray
import org.json.JSONObject

/** A complete copy of the user's data, independent of where it is stored. */
data class BackupSnapshot(
    val schemaVersion: Int,
    val createdAt: Long,
    val acquaintances: List<AcquaintanceEntity>,
    val categories: List<CategoryEntity>,
    val relations: List<RelationEntity>,
    val crossRefs: List<AcquaintanceCategoryCrossRef>,
)

/** Thrown when a backup file cannot be parsed. */
class InvalidBackupException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Converts a [BackupSnapshot] to and from the versioned JSON backup format. */
object BackupSerializer {
    const val FORMAT_VERSION = 1

    fun toJson(snapshot: BackupSnapshot): String =
        JSONObject()
            .put("format", FORMAT_VERSION)
            .put("schemaVersion", snapshot.schemaVersion)
            .put("createdAt", snapshot.createdAt)
            .put("acquaintances", snapshot.acquaintances.toJsonArray { it.toJson() })
            .put("categories", snapshot.categories.toJsonArray { it.toJson() })
            .put("relations", snapshot.relations.toJsonArray { it.toJson() })
            .put("crossRefs", snapshot.crossRefs.toJsonArray { it.toJson() })
            .toString()

    fun fromJson(json: String): BackupSnapshot =
        try {
            val root = JSONObject(json)
            val format = root.getInt("format")
            if (format > FORMAT_VERSION) {
                throw InvalidBackupException("Backup format $format is newer than this app supports")
            }
            BackupSnapshot(
                schemaVersion = root.getInt("schemaVersion"),
                createdAt = root.getLong("createdAt"),
                acquaintances = root.getJSONArray("acquaintances").map { it.toAcquaintance() },
                categories = root.getJSONArray("categories").map { it.toCategory() },
                relations = root.getJSONArray("relations").map { it.toRelation() },
                crossRefs = root.getJSONArray("crossRefs").map { it.toCrossRef() },
            )
        } catch (e: InvalidBackupException) {
            throw e
        } catch (e: Exception) {
            throw InvalidBackupException("Backup file is corrupt", e)
        }

    private fun <T> List<T>.toJsonArray(transform: (T) -> JSONObject): JSONArray =
        JSONArray().also { array -> forEach { array.put(transform(it)) } }

    private fun <T> JSONArray.map(transform: (JSONObject) -> T): List<T> =
        (0 until length()).map { transform(getJSONObject(it)) }

    private fun JSONObject.optNullableString(key: String): String? = if (isNull(key)) null else getString(key)

    private fun JSONObject.optNullableLong(key: String): Long? = if (isNull(key)) null else getLong(key)

    private fun AcquaintanceEntity.toJson() =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("bio", bio)
            .put("androidContactLookupKey", androidContactLookupKey ?: JSONObject.NULL)
            .put("birthday", birthday ?: JSONObject.NULL)
            .put("isDeceased", isDeceased)

    // Optional fields tolerate their absence so backups from older schema versions stay restorable.
    private fun JSONObject.toAcquaintance() =
        AcquaintanceEntity(
            id = getLong("id"),
            name = getString("name"),
            bio = optString("bio", ""),
            androidContactLookupKey = optNullableString("androidContactLookupKey"),
            birthday = optNullableString("birthday"),
            isDeceased = optBoolean("isDeceased", false),
        )

    private fun CategoryEntity.toJson() =
        JSONObject()
            .put("id", id)
            .put("name", name)
            .put("parentId", parentId ?: JSONObject.NULL)
            .put("sortOrder", sortOrder)

    private fun JSONObject.toCategory() =
        CategoryEntity(
            id = getLong("id"),
            name = getString("name"),
            parentId = optNullableLong("parentId"),
            sortOrder = optInt("sortOrder", 0),
        )

    private fun RelationEntity.toJson() =
        JSONObject()
            .put("id", id)
            .put("fromId", fromId)
            .put("toId", toId)
            .put("typeKey", typeKey)
            .put("customLabel", customLabel ?: JSONObject.NULL)

    private fun JSONObject.toRelation() =
        RelationEntity(
            id = getLong("id"),
            fromId = getLong("fromId"),
            toId = getLong("toId"),
            typeKey = getString("typeKey"),
            customLabel = optNullableString("customLabel"),
        )

    private fun AcquaintanceCategoryCrossRef.toJson() =
        JSONObject()
            .put("acquaintanceId", acquaintanceId)
            .put("categoryId", categoryId)

    private fun JSONObject.toCrossRef() =
        AcquaintanceCategoryCrossRef(
            acquaintanceId = getLong("acquaintanceId"),
            categoryId = getLong("categoryId"),
        )
}
