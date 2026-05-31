package com.example.testapp004.data

import android.net.Uri
import com.example.testapp004.model.ContactInfo

interface ContactRepository {
    suspend fun lookupContactByUri(contactUri: Uri): ContactInfo?

    suspend fun lookupContactByKey(lookupKey: String): ContactInfo?
}
