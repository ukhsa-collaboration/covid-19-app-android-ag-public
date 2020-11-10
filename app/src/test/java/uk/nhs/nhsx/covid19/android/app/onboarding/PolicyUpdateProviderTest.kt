package uk.nhs.nhsx.covid19.android.app.onboarding

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.BuildConfig
import kotlin.test.assertEquals

class PolicyUpdateProviderTest {

    private val policyUpdateStorage = mockk<PolicyUpdateStorage>(relaxed = true)

    val testSubject = PolicyUpdateProvider(policyUpdateStorage)

    @Test
    fun `return not accepted policy for unmarked accepted policy`() {
        every { policyUpdateStorage.value } returns null

        val actual = testSubject.isPolicyAccepted()

        assertEquals(false, actual)
    }

    @Test
    fun `return not accepted policy for marked accepted policy on 3_9 version`() {
        every { policyUpdateStorage.value } returns "3.9"

        val actual = testSubject.isPolicyAccepted()

        assertEquals(false, actual)
    }

    @Test
    fun `return accepted policy for marked accepted policy on 3_10 version`() {
        every { policyUpdateStorage.value } returns "3.10"

        val actual = testSubject.isPolicyAccepted()

        assertEquals(true, actual)
    }

    @Test
    fun `return accepted policy for marked accepted policy on 3_11 version`() {
        every { policyUpdateStorage.value } returns "3.11"

        val actual = testSubject.isPolicyAccepted()

        assertEquals(true, actual)
    }

    @Test
    fun `mark accepted policy stores current application version`() {
        testSubject.markPolicyAccepted()

        verify { policyUpdateStorage setProperty "value" value eq(BuildConfig.VERSION_NAME_SHORT) }
    }

    @Test
    fun `compare versions`() {
        // Equal versions
        assertEquals(0, testSubject.compareVersions("1", "1"))
        assertEquals(0, testSubject.compareVersions("2.2", "2.2"))
        assertEquals(0, testSubject.compareVersions("3.3.3", "3.3.3"))

        // Newer version2
        assertEquals(-1, testSubject.compareVersions("1", "2"))
        assertEquals(-1, testSubject.compareVersions("1.1", "2"))
        assertEquals(-1, testSubject.compareVersions("1.1", "2.2"))
        assertEquals(-1, testSubject.compareVersions("1.1", "2.2.2"))
        assertEquals(-1, testSubject.compareVersions("1.2.3", "3.2.1"))

        // Older version2
        assertEquals(1, testSubject.compareVersions("2", "1"))
        assertEquals(1, testSubject.compareVersions("2", "1.1"))
        assertEquals(1, testSubject.compareVersions("2.2", "1.1"))
        assertEquals(1, testSubject.compareVersions("2.2.2", "1.1"))
        assertEquals(1, testSubject.compareVersions("3.2.1", "1.2.3"))
    }
}
