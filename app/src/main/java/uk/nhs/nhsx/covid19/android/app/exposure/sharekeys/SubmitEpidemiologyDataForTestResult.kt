package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import uk.nhs.nhsx.covid19.android.app.exposure.encounter.SubmitEpidemiologyData
import uk.nhs.nhsx.covid19.android.app.exposure.encounter.calculation.EpidemiologyEventProvider
import javax.inject.Inject

class SubmitEpidemiologyDataForTestResult @Inject constructor(
    private val epidemiologyEventProvider: EpidemiologyEventProvider,
    private val submitEpidemiologyData: SubmitEpidemiologyData
) {

    operator fun invoke(keySharingInfo: KeySharingInfo) {
        submitEpidemiologyData.submitAfterPositiveTest(
            epidemiologyEventProvider.epidemiologyEvents,
            testKitType = keySharingInfo.testKitType,
            requiresConfirmatoryTest = keySharingInfo.requiresConfirmatoryTest
        )
    }
}
