package uk.nhs.nhsx.covid19.android.app.payment

import com.squareup.moshi.Moshi
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import kotlin.test.assertEquals

class IsolationPaymentTokenStateProviderTest {

    private val isolationPaymentTokenStateStorage = mockk<IsolationPaymentTokenStateStorage>(relaxed = true)
    private val moshi = Moshi.Builder().build()

    private val testSubject = IsolationPaymentTokenStateProvider(
        isolationPaymentTokenStateStorage,
        moshi
    )

    @Test
    fun `reading null token state returns unresolved`() {
        every { isolationPaymentTokenStateStorage.value } returns null

        assertEquals(Unresolved, testSubject.tokenState)
    }

    @Test
    fun `read unresolved token state`() {
        every { isolationPaymentTokenStateStorage.value } returns UNRESOLVED_JSON

        assertEquals(Unresolved, testSubject.tokenState)
    }

    @Test
    fun `read disabled token state`() {
        every { isolationPaymentTokenStateStorage.value } returns DISABLED_JSON

        assertEquals(Disabled, testSubject.tokenState)
    }

    @Test
    fun `read token present token state`() {
        every { isolationPaymentTokenStateStorage.value } returns TOKEN_JSON

        assertEquals(Token(TOKEN_VALUE), testSubject.tokenState)
    }

    @Test
    fun `store unresolved token state`() {
        testSubject.tokenState = Unresolved

        verify { isolationPaymentTokenStateStorage.value = UNRESOLVED_JSON }
    }

    @Test
    fun `store disabled token state`() {
        testSubject.tokenState = Disabled

        verify { isolationPaymentTokenStateStorage.value = DISABLED_JSON }
    }

    @Test
    fun `store token present token state`() {
        testSubject.tokenState = Token(TOKEN_VALUE)

        verify { isolationPaymentTokenStateStorage.value = TOKEN_JSON }
    }

    companion object {
        val TOKEN_VALUE = "myToken"

        val UNRESOLVED_JSON =
            """{"type":"UNRESOLVED"}"""
        val DISABLED_JSON =
            """{"type":"DISABLED"}"""
        val TOKEN_JSON =
            """{"type":"TOKEN","token":"$TOKEN_VALUE"}"""
    }
}
