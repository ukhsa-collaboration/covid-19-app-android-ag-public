package uk.nhs.nhsx.covid19.android.app.util

import android.content.Context
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test

class CreateEncryptedSharedPreferencesActionTest {

    private val context = mockk<Context>()
    private val encryptedSharedPreferencesUtils = mockk<EncryptedSharedPreferencesUtils>(relaxed = true)
    private val strongBoxMigrationRetryChecker = mockk<StrongBoxMigrationRetryChecker>()

    private val testSubject = CreateEncryptedSharedPreferencesAction(
        context,
        encryptedSharedPreferencesUtils,
        strongBoxMigrationRetryChecker
    )

    @Test
    fun `create encrypted preferences`() {

        testSubject()

        verify {
            encryptedSharedPreferencesUtils.createEncryptedSharedPreferences(
                context,
                strongBoxMigrationRetryChecker
            )
        }
    }
}
