package com.example.testapp004.data

import com.example.testapp004.model.UpdateCheckOutcome

interface UpdateRepository {
    suspend fun checkForUpdate(currentVersionName: String): UpdateCheckOutcome
}
