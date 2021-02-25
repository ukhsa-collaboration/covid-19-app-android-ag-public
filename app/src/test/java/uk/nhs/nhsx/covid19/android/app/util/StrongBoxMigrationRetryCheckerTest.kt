package uk.nhs.nhsx.covid19.android.app.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class StrongBoxMigrationRetryCheckerTest {

    private val migrationRetryStorage = mockk<StrongBoxMigrationRetryStorage>(relaxed = true)

    private val testSubject = StrongBoxMigrationRetryChecker(migrationRetryStorage)

    @Test
    fun `increments initial migration counter`() {
        every { migrationRetryStorage.value } returns null

        testSubject.increment()

        verify { migrationRetryStorage setProperty "value" value eq(1) }
    }

    @Test
    fun `increments migration counter`() {
        every { migrationRetryStorage.value } returns 1

        testSubject.increment()

        verify { migrationRetryStorage setProperty "value" value eq(2) }
    }

    @Test
    fun `allows migration on first try`() {
        every { migrationRetryStorage.value } returns null

        val result = testSubject.canMigrate()

        assertTrue(result)
    }

    @Test
    fun `allows migration on third try`() {
        every { migrationRetryStorage.value } returns 2

        val result = testSubject.canMigrate()

        assertTrue(result)
    }

    @Test
    fun `disallows migration on forth try`() {
        every { migrationRetryStorage.value } returns 3

        val result = testSubject.canMigrate()

        assertFalse(result)
    }
}
