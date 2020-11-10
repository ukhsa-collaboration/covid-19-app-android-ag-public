package uk.nhs.nhsx.covid19.android.app.testhelpers

import uk.nhs.nhsx.covid19.android.app.availability.UpdateManager

class TestUpdateManager : UpdateManager {

    var availableUpdateStatus: UpdateManager.AvailableUpdateStatus =
        UpdateManager.AvailableUpdateStatus.NoUpdateAvailable

    override suspend fun getAvailableUpdateVersionCode(): UpdateManager.AvailableUpdateStatus =
        availableUpdateStatus
}
