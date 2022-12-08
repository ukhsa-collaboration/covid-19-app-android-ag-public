package uk.nhs.nhsx.covid19.android.app.exposure.sharekeys

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.KeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.CanShareKeys.CanShareKeysResult.NoKeySharingPossible
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.Initial
import uk.nhs.nhsx.covid19.android.app.exposure.sharekeys.ShouldEnterShareKeysFlowResult.None
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit.HOURS
import kotlin.test.assertEquals

class ShouldEnterShareKeysFlowTest {

    private val canShareKeys = mockk<CanShareKeys>()
    private val fixedClock = Clock.fixed(Instant.parse("2020-07-28T00:05:00.00Z"), ZoneOffset.UTC)

    private val testSubject = ShouldEnterShareKeysFlow(canShareKeys, fixedClock)

    @Before
    fun setUp() {
        every { canShareKeys() } returns KeySharingPossible(mockk())
    }

    @Test
    fun `when key sharing is not possible then return None`() {
        every { canShareKeys() } returns NoKeySharingPossible

        val result = testSubject.invoke()

        assertEquals(None, result)
    }

    @Test
    fun `when key sharing is possible and user has declined sharing keys and acknowledgedDate is more than 24 hours ago returns Reminder`() {
        every { canShareKeys() } returns KeySharingPossible(
            keySharingInfo(acknowledgedHoursAgo = 25, hasDeclinedSharingKeys = true)
        )

        val result = testSubject.invoke()

        assertEquals(ShouldEnterShareKeysFlowResult.Reminder, result)
    }

    @Test
    fun `when key sharing is possible and user has declined sharing keys and acknowledgedDate is less than 24 hours ago returns None`() {
        every { canShareKeys() } returns KeySharingPossible(
            keySharingInfo(acknowledgedHoursAgo = 23, hasDeclinedSharingKeys = true)
        )

        val result = testSubject.invoke()

        assertEquals(None, result)
    }

    @Test
    fun `when not self reported, key sharing is possible and user has not declined sharing keys and acknowledgedDate is more than 24 hours ago returns Initial`() {
        every { canShareKeys() } returns KeySharingPossible(
            keySharingInfo(acknowledgedHoursAgo = 25, hasDeclinedSharingKeys = false)
        )

        val result = testSubject.invoke()

        assertEquals(Initial, result)
    }

    @Test
    fun `when not self reported, key sharing is possible and user has not declined sharing keys and acknowledgedDate is less than 24 hours ago returns Initial`() {
        every { canShareKeys() } returns KeySharingPossible(
            keySharingInfo(acknowledgedHoursAgo = 23, hasDeclinedSharingKeys = false)
        )

        val result = testSubject.invoke()

        assertEquals(Initial, result)
    }

    @Test
    fun `when self reported, key sharing is possible and user has not declined sharing keys and acknowledgedDate is more than 24 hours ago returns None`() {
        every { canShareKeys() } returns KeySharingPossible(
            keySharingInfo(acknowledgedHoursAgo = 25, hasDeclinedSharingKeys = false, isSelfReporting = true)
        )

        val result = testSubject.invoke()

        assertEquals(None, result)
    }

    @Test
    fun `when self reported, key sharing is possible and user has not declined sharing keys and acknowledgedDate is less than 24 hours ago returns None`() {
        every { canShareKeys() } returns KeySharingPossible(
            keySharingInfo(acknowledgedHoursAgo = 23, hasDeclinedSharingKeys = false, isSelfReporting = true)
        )

        val result = testSubject.invoke()

        assertEquals(None, result)
    }

    private fun keySharingInfo(acknowledgedHoursAgo: Long = 23, hasDeclinedSharingKeys: Boolean = true, isSelfReporting: Boolean = false) =
        KeySharingInfo(
            diagnosisKeySubmissionToken = "token",
            acknowledgedDate = Instant.now(fixedClock).minus(acknowledgedHoursAgo, HOURS),
            hasDeclinedSharingKeys = hasDeclinedSharingKeys,
            isSelfReporting = isSelfReporting
        )
}
