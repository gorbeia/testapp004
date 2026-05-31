package com.example.testapp004.data

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import com.example.testapp004.model.ContactInfo
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
