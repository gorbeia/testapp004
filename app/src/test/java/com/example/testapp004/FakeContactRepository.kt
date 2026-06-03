package com.example.testapp004

import android.net.Uri
import com.example.testapp004.data.ContactRepository
import com.example.testapp004.model.ContactInfo
import com.example.testapp004.model.ImportableContact

class FakeContactRepository : ContactRepository {
    var importableContacts: List<ImportableContact> = emptyList()

    override suspend fun lookupContactByUri(contactUri: Uri): ContactInfo? = null

    override suspend fun lookupContactByKey(lookupKey: String): ContactInfo? = null

    override suspend fun fetchImportableContacts(): List<ImportableContact> = importableContacts
}
