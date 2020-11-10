package uk.nhs.nhsx.covid19.android.app.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test

class DataAndPrivacyViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val policyUpdateProvider = mockk<PolicyUpdateProvider>(relaxed = true)

    private val testSubject = PolicyUpdateViewModel(policyUpdateProvider)

    @Test
    fun `marking privacy policy as accepted updates policy provider`() {
        testSubject.markPolicyAccepted()

        verify { policyUpdateProvider.markPolicyAccepted() }
    }
}
