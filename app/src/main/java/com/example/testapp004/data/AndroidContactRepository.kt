package com.example.testapp004.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.example.testapp004.model.ContactInfo
import com.example.testapp004.model.ImportableContact
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidContactRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : ContactRepository {
    override suspend fun lookupContactByUri(contactUri: Uri): ContactInfo? = withContext(Dispatchers.IO) {
        val cursor = context.contentResolver.query(
            contactUri,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ),
            null, null, null,
        ) ?: return@withContext null

        cursor.use {
            if (!it.moveToFirst()) return@withContext null
            val contactId = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val lookupKey = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY))
                ?: return@withContext null
            val displayName = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY))
                ?: ""
            ContactInfo(
                lookupKey = lookupKey,
                displayName = displayName,
                primaryPhone = fetchPrimaryPhone(contactId),
            )
        }
    }

    override suspend fun lookupContactByKey(lookupKey: String): ContactInfo? = withContext(Dispatchers.IO) {
        val lookupUri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_LOOKUP_URI, lookupKey)
        val resolvedUri = ContactsContract.Contacts.lookupContact(context.contentResolver, lookupUri)
            ?: return@withContext null
        lookupContactByUri(resolvedUri)
    }

    override suspend fun fetchImportableContacts(): List<ImportableContact> = withContext(Dispatchers.IO) {
        val contacts = mutableListOf<Triple<Long, String, String>>() // (id, lookupKey, displayName)
        val cursor = context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
            ),
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} IS NOT NULL",
            null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC",
        ) ?: return@withContext emptyList()

        cursor.use {
            while (it.moveToNext()) {
                val id = it.getLong(it.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                val key = it.getString(it.getColumnIndexOrThrow(ContactsContract.Contacts.LOOKUP_KEY))
                    ?: continue
                val name = it.getString(
                    it.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
                ).orEmpty()
                if (name.isNotBlank()) contacts.add(Triple(id, key, name))
            }
        }

        val birthdayMap = fetchBirthdayMap()
        val orgMap = fetchOrganizationMap()
        val notesMap = fetchNotesMap()

        contacts.map { (id, key, name) ->
            ImportableContact(
                lookupKey = key,
                displayName = name,
                birthday = birthdayMap[id],
                organizationInfo = orgMap[id],
                notes = notesMap[id],
            )
        }
    }

    private fun fetchBirthdayMap(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Event.START_DATE),
            "${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Event.TYPE} = ?",
            arrayOf(
                ContactsContract.CommonDataKinds.Event.CONTENT_ITEM_TYPE,
                ContactsContract.CommonDataKinds.Event.TYPE_BIRTHDAY.toString(),
            ),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))
                val date = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Event.START_DATE),
                ) ?: continue
                result[id] = date
            }
        }
        return result
    }

    private fun fetchOrganizationMap(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Organization.COMPANY,
                ContactsContract.CommonDataKinds.Organization.TITLE,
            ),
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))
                if (id in result) continue
                val company = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.COMPANY),
                )
                val title = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Organization.TITLE),
                )
                val info = listOfNotNull(title, company).joinToString(" @ ").ifBlank { null }
                if (info != null) result[id] = info
            }
        }
        return result
    }

    private fun fetchNotesMap(): Map<Long, String> {
        val result = mutableMapOf<Long, String>()
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.CONTACT_ID, ContactsContract.CommonDataKinds.Note.NOTE),
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE),
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Data.CONTACT_ID))
                val note = cursor.getString(
                    cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Note.NOTE),
                ) ?: continue
                if (note.isNotBlank()) result[id] = note
            }
        }
        return result
    }

    private fun fetchPrimaryPhone(contactId: Long): String? {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null,
        ) ?: return null
        return cursor.use { if (it.moveToFirst()) it.getString(0) else null }
    }
}
