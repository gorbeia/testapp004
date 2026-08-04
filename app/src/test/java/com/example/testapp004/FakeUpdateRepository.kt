package com.example.testapp004

import com.example.testapp004.data.UpdateRepository
import com.example.testapp004.model.UpdateCheckOutcome

class FakeUpdateRepository : UpdateRepository {
    var nextOutcome: UpdateCheckOutcome = UpdateCheckOutcome.UpToDate("1.0.0", "1.0.0")

    override suspend fun checkForUpdate(currentVersionName: String): UpdateCheckOutcome = nextOutcome
}
