package uk.nhs.nhsx.covid19.android.app.testordering.unknownresult

import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class UnknownTestResultViewModelTest {
    private val receivedUnknownTestResultProvider = mockk<ReceivedUnknownTestResultProvider>(relaxUnitFun = true)

    val testSubject = UnknownTestResultViewModel(receivedUnknownTestResultProvider)

    @Test
    fun `acknowledging unknown test result resets flag`() {
        testSubject.acknowledgeUnknownTestResult()
        verify { receivedUnknownTestResultProvider.value = false }
    }
}
