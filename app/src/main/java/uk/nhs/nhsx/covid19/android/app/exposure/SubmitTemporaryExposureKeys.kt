package uk.nhs.nhsx.covid19.android.app.exposure

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.Result
import uk.nhs.nhsx.covid19.android.app.remote.KeysSubmissionApi
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.TemporaryExposureKeysPayload
import javax.inject.Inject

class SubmitTemporaryExposureKeys @Inject constructor(
    private val keysSubmissionApi: KeysSubmissionApi
) {
    suspend operator fun invoke(
        exposureKeys: List<NHSTemporaryExposureKey>,
        diagnosisKeySubmissionToken: String = DEFAULT_DIAGNOSIS_KEY_SUBMISSION_TOKEN,
        isPrivateJourney: Boolean = false,
        testKit: String = "LAB_RESULT"
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                keysSubmissionApi.submitGeneratedKeys(
                    TemporaryExposureKeysPayload(
                        diagnosisKeySubmissionToken = diagnosisKeySubmissionToken,
                        temporaryExposureKeys = exposureKeys,
                        isPrivateJourney = isPrivateJourney,
                        testKit = testKit
                    )
                )
                return@withContext Result.Success(Unit)
            } catch (e: Exception) {
                return@withContext Result.Failure(e)
            }
        }
}

const val DEFAULT_DIAGNOSIS_KEY_SUBMISSION_TOKEN = "00000000-0000-0000-0000-000000000000"
