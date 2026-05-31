package com.example.testapp004

import android.net.Uri
import com.example.testapp004.data.ContactRepository
import com.example.testapp004.model.ContactInfo

class FakeContactRepository : ContactRepository {
    override suspend fun lookupContactByUri(contactUri: Uri): ContactInfo? = null

    override suspend fun lookupContactByKey(lookupKey: String): ContactInfo? = null
}
