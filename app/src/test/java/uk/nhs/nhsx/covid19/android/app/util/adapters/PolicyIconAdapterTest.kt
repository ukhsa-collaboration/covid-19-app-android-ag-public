package uk.nhs.nhsx.covid19.android.app.util.adapters

import com.squareup.moshi.Moshi
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.DEFAULT
import uk.nhs.nhsx.covid19.android.app.remote.data.PolicyIcon.LARGE_EVENTS
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class PolicyIconAdapterTest {

    private val moshi = Moshi.Builder().add(PolicyIconAdapter()).build()

    private val testSubject = moshi.adapter(PolicyIcon::class.java)

    private val policyIconJson =
        """"large-events""""
    private val unknownPolicyIconJson =
        """"this-icon-does-not-exist""""

    @Test
    fun `convert PolicyIcon to json`() {
        val result = testSubject.toJson(LARGE_EVENTS)

        assertEquals(policyIconJson, result)
    }

    @Test
    fun `parse PolicyIcon from json`() {
        val result = testSubject.fromJson(policyIconJson)

        assertNotNull(result)

        assertEquals(LARGE_EVENTS, result)
    }

    @Test
    fun `parse unknown PolicyIcon from json falls back to default`() {
        val result = testSubject.fromJson(unknownPolicyIconJson)

        assertNotNull(result)

        assertEquals(DEFAULT, result)
    }
}
