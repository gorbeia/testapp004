package com.example.testapp004.data

import android.net.Uri
import com.example.testapp004.model.ContactInfo
import com.example.testapp004.model.ImportableContact

interface ContactRepository {
    suspend fun lookupContactByUri(contactUri: Uri): ContactInfo?

    suspend fun lookupContactByKey(lookupKey: String): ContactInfo?

    suspend fun fetchImportableContacts(): List<ImportableContact>
}
