package uk.nhs.nhsx.covid19.android.app.onboarding

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import uk.nhs.nhsx.covid19.android.app.util.CompareReleaseVersions
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PolicyUpdateProviderTest {

    private val policyUpdateStorage = mockk<PolicyUpdateStorage>(relaxUnitFun = true)
    private val compareVersions = mockk<CompareReleaseVersions>()
    val testSubject = PolicyUpdateProvider(policyUpdateStorage, compareVersions)

    @Test
    fun `return not accepted policy for unmarked accepted policy`() {
        every { policyUpdateStorage.value } returns null

        val actual = testSubject.isPolicyAccepted()

        assertFalse(actual)
    }

    @Test
    fun `return not accepted policy for marked accepted policy on 4_15 version`() {
        every { policyUpdateStorage.value } returns "4.15"
        every { compareVersions.invoke("4.15", "4.16") } returns -1

        val actual = testSubject.isPolicyAccepted()

        assertFalse(actual)
    }

    @Test
    fun `return accepted policy for marked accepted policy on 4_16 version`() {
        every { policyUpdateStorage.value } returns "4.16"
        every { compareVersions.invoke("4.16", "4.16") } returns 0

        val actual = testSubject.isPolicyAccepted()

        assertTrue(actual)
    }

    @Test
    fun `return accepted policy for marked accepted policy on 4_17 version`() {
        every { policyUpdateStorage.value } returns "4.17"
        every { compareVersions.invoke("4.17", "4.16") } returns 1

        val actual = testSubject.isPolicyAccepted()

        assertTrue(actual)
    }

    @Test
    fun `mark accepted policy stores current application version`() {
        testSubject.markPolicyAccepted()

        verify { policyUpdateStorage setProperty "value" value eq(BuildConfig.VERSION_NAME_SHORT) }
    }
}
