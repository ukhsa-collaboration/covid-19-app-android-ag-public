package uk.nhs.nhsx.covid19.android.app.testordering

import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType

interface TestResult {
    val testKitType: VirologyTestKitType?
    val requiresConfirmatoryTest: Boolean

    fun isPositive(): Boolean
    fun isConfirmed(): Boolean
}
