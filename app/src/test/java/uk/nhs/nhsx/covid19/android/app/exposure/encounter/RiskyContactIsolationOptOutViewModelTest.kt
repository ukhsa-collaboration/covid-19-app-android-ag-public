package uk.nhs.nhsx.covid19.android.app.exposure.encounter

import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.OptOutOfContactIsolation
import uk.nhs.nhsx.covid19.android.app.state.IsolationState.OptOutReason.NEW_ADVICE

class RiskyContactIsolationOptOutViewModelTest {

    private val optOutOfContactIsolation = mockk<OptOutOfContactIsolation>(relaxUnitFun = true)
    private val acknowledgeRiskyContact = mockk<AcknowledgeRiskyContact>(relaxUnitFun = true)

    private val testSubject = RiskyContactIsolationOptOutViewModel(optOutOfContactIsolation, acknowledgeRiskyContact)

    @Test
    fun `verify acknowledge and opt out contact isolation when new advice is active`() {

        testSubject.acknowledgeAndOptOutContactIsolation()

        verifyOrder {
            optOutOfContactIsolation(reason = NEW_ADVICE)
            acknowledgeRiskyContact()
        }
    }
}
