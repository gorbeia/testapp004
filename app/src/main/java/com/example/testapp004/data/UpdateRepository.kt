package com.example.testapp004.data

import com.example.testapp004.model.AppRelease

interface UpdateRepository {
    suspend fun checkForUpdate(currentVersionName: String): AppRelease?
}
