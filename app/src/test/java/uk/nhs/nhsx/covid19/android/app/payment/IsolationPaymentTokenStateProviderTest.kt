package uk.nhs.nhsx.covid19.android.app.payment

import org.junit.jupiter.api.Test
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateProvider.Companion.ISOLATION_PAYMENT_TOKEN
import uk.nhs.nhsx.covid19.android.app.util.ProviderTest
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectation
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT

class IsolationPaymentTokenStateProviderTest :
    ProviderTest<IsolationPaymentTokenStateProvider, IsolationPaymentTokenState>() {

    override val getTestSubject = ::IsolationPaymentTokenStateProvider
    override val property = IsolationPaymentTokenStateProvider::tokenState
    override val key = ISOLATION_PAYMENT_TOKEN
    override val defaultValue = Unresolved
    override val expectations: List<ProviderTestExpectation<IsolationPaymentTokenState>> = listOf(
        ProviderTestExpectation(json = UNRESOLVED_JSON, objectValue = Unresolved, direction = JSON_TO_OBJECT),
        ProviderTestExpectation(json = DISABLED_JSON, objectValue = Disabled),
        ProviderTestExpectation(json = TOKEN_JSON, objectValue = Token(TOKEN_VALUE))
    )

    @Test
    fun `does not call set on storage when value has not changed`() {
        sharedPreferencesReturns(UNRESOLVED_JSON)

        testSubject.tokenState = Unresolved

        verifySharedPreferencesEditorWasNotCalled()
    }

    companion object {
        private const val TOKEN_VALUE = "myToken"

        private const val UNRESOLVED_JSON =
            """{"type":"UNRESOLVED"}"""

        private const val DISABLED_JSON =
            """{"type":"DISABLED"}"""

        private const val TOKEN_JSON =
            """{"type":"TOKEN","token":"$TOKEN_VALUE"}"""
    }
}
