package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import uk.nhs.nhsx.covid19.android.app.analytics.MetadataProvider
import uk.nhs.nhsx.covid19.android.app.common.runSafely
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEvent
import uk.nhs.nhsx.covid19.android.app.remote.EpidemiologyDataApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource.EXPOSURE_WINDOW_AFTER_POSITIVE
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventType.EXPOSURE_WINDOW_POSITIVE_TEST
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyEventWithType
import uk.nhs.nhsx.covid19.android.app.remote.data.EpidemiologyRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.testordering.SubmitFakeExposureWindows
import javax.inject.Inject

class SubmitEpidemiologyData constructor(
    private val metadataProvider: MetadataProvider,
    private val epidemiologyDataApi: EpidemiologyDataApi,
    private val submitFakeExposureWindows: SubmitFakeExposureWindows,
    private val submitEpidemiologyDataScope: CoroutineScope,
    private val submitEpidemiologyDataDispatcher: CoroutineDispatcher
) {

    @Inject
    constructor(
        metadataProvider: MetadataProvider,
        epidemiologyDataApi: EpidemiologyDataApi,
        submitFakeExposureWindows: SubmitFakeExposureWindows
    ) : this(
        metadataProvider,
        epidemiologyDataApi,
        submitFakeExposureWindows,
        GlobalScope,
        Dispatchers.IO
    )

    fun submit(epidemiologyEventList: List<EpidemiologyEvent>) {
        submit(
            epidemiologyEventList,
            epidemiologyEventType = EXPOSURE_WINDOW,
            epidemiologyEventVersion = 1,
            testKitType = null,
            requiresConfirmatoryTest = null
        )
    }

    fun submitAfterPositiveTest(
        epidemiologyEventList: List<EpidemiologyEvent>,
        testKitType: VirologyTestKitType?,
        requiresConfirmatoryTest: Boolean?
    ) {
        submit(
            epidemiologyEventList,
            epidemiologyEventType = EXPOSURE_WINDOW_POSITIVE_TEST,
            epidemiologyEventVersion = 2,
            testKitType,
            requiresConfirmatoryTest
        )
    }

    private fun submit(
        epidemiologyEventList: List<EpidemiologyEvent>,
        epidemiologyEventType: EpidemiologyEventType,
        epidemiologyEventVersion: Int,
        testKitType: VirologyTestKitType?,
        requiresConfirmatoryTest: Boolean?
    ) {
        submitEpidemiologyDataScope.launch(submitEpidemiologyDataDispatcher) {
            epidemiologyEventList.forEach { epidemiologyEvent ->
                runSafely {
                    epidemiologyDataApi.submitEpidemiologyData(
                        EpidemiologyRequest(
                            metadata = metadataProvider.getMetadata(),
                            events = listOf(
                                epidemiologyEvent.toEpidemiologyEventWithType(
                                    epidemiologyEventType,
                                    epidemiologyEventVersion,
                                    testKitType,
                                    requiresConfirmatoryTest
                                )
                            )
                        )
                    )
                }
            }
            submitFakeExposureWindows(
                epidemiologyEventType.toEmptySubmissionSource(),
                epidemiologyEventList.size
            )
        }
    }
}

@VisibleForTesting
internal fun EpidemiologyEvent.toEpidemiologyEventWithType(
    eventType: EpidemiologyEventType,
    eventVersion: Int,
    testKitType: VirologyTestKitType?,
    requiresConfirmatoryTest: Boolean?
): EpidemiologyEventWithType =
    EpidemiologyEventWithType(
        type = eventType,
        version = eventVersion,
        payload = this.payload.copy(
            testType = testKitType,
            requiresConfirmatoryTest = requiresConfirmatoryTest
        )
    )

private fun EpidemiologyEventType.toEmptySubmissionSource(): EmptySubmissionSource =
    when (this) {
        EXPOSURE_WINDOW_POSITIVE_TEST -> EXPOSURE_WINDOW_AFTER_POSITIVE
        EXPOSURE_WINDOW -> EmptySubmissionSource.EXPOSURE_WINDOW
    }
