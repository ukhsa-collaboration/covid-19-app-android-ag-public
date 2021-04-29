package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import com.jeroenmols.featureflag.framework.FeatureFlag.STORE_EXPOSURE_WINDOWS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.PartitionExposureWindowsResult
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EpidemiologyDataManager @Inject constructor(
    private val randomNonRiskyExposureWindowsLimiter: RandomNonRiskyExposureWindowsLimiter,
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val submitEpidemiologyData: SubmitEpidemiologyData,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows,
) {

    private val nonRiskyEpidemiologyEventsSendingLimit = 15

    private val nonRiskyEpidemiologyEventsStorageLimit = 15

    suspend fun storeAndSubmit(partitionedExposureWindowsResult: PartitionExposureWindowsResult) {
        val riskyEpidemiologyEvents =
            partitionedExposureWindowsResult.riskyExposureWindows
                .map { it.toEpidemiologyEvent() }
                .also { storeRiskyEpidemiologyEvents(it) }

        val subsetOfNonRiskyEpidemiologyEvents = if (randomNonRiskyExposureWindowsLimiter.isAllowed()) {
            partitionedExposureWindowsResult.nonRiskyExposureWindows
                .takeLast(nonRiskyEpidemiologyEventsSendingLimit)
                .map { it.toEpidemiologyEvent() }
                .also {
                    storeNonRiskyEpidemiologyEvents(
                        it,
                        storageLimit = nonRiskyEpidemiologyEventsStorageLimit
                    )
                }
        } else {
            emptyList()
        }

        val epidemiologyEvents = riskyEpidemiologyEvents + subsetOfNonRiskyEpidemiologyEvents

        if (epidemiologyEvents.isEmpty()) {
            submitEmptyExposureWindows()
        } else {
            submitEpidemiologyData.submit(epidemiologyEvents)
        }
    }

    suspend fun submitEmptyExposureWindows() {
        withContext(Dispatchers.IO) {
            runSafely {
                submitFakeExposureWindows()
            }
        }
    }

    private fun storeRiskyEpidemiologyEvents(epidemiologyEvents: List<EpidemiologyEvent>) {
        if (RuntimeBehavior.isFeatureEnabled(STORE_EXPOSURE_WINDOWS) && epidemiologyEvents.isNotEmpty()) {
            epidemiologyEventProvider.addRiskyEpidemiologyEvents(epidemiologyEvents)
        }
    }

    private fun storeNonRiskyEpidemiologyEvents(epidemiologyEvents: List<EpidemiologyEvent>, storageLimit: Int) {
        if (RuntimeBehavior.isFeatureEnabled(STORE_EXPOSURE_WINDOWS) && epidemiologyEvents.isNotEmpty()) {
            epidemiologyEventProvider.addNonRiskyEpidemiologyEvents(epidemiologyEvents, storageLimit = storageLimit)
        }
    }
}
