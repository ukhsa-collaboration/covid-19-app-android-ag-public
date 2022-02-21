package uk.nhs.nhsx.covid19.android.app.scenariodialog

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import uk.nhs.nhsx.covid19.android.app.remote.MockVirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.VirologyTestingApi
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyCtaExchangeResponse
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import java.time.Instant
import javax.inject.Inject

class VirologyTestResultMockViewModel @Inject constructor(
    internal val virologyTestingApi: VirologyTestingApi
) : ViewModel() {

    var virologyCtaExchangeResponse = MutableLiveData<VirologyCtaExchangeResponse>()

    init {
        if (virologyTestingApi is MockVirologyTestingApi)
            virologyCtaExchangeResponse.value = virologyTestingApi.responseMock
    }

    fun mockResponse(
        diagnosisKeySubmissionToken: String?,
        virologyTestResultValue: VirologyTestResult,
        virologyTestKitType: VirologyTestKitType,
        diagnosisKeySubmissionSupported: Boolean,
        requiresConfirmatoryTest: Boolean,
        shouldOfferFollowUpTest: Boolean,
        confirmatoryDayLimit: Int?,
        testEndDate: Instant
    ) {
        if (virologyTestingApi is MockVirologyTestingApi)
            virologyTestingApi.mockResponse(
                diagnosisKeySubmissionToken = diagnosisKeySubmissionToken,
                virologyTestResultValue = virologyTestResultValue,
                virologyTestKitType = virologyTestKitType,
                diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported,
                requiresConfirmatoryTest = requiresConfirmatoryTest,
                confirmatoryDayLimit = confirmatoryDayLimit,
                shouldOfferFollowUpTest = shouldOfferFollowUpTest,
                testEndDate = testEndDate
            )
    }
}
