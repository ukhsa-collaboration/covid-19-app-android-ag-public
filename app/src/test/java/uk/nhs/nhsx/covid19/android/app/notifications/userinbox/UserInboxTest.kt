package uk.nhs.nhsx.covid19.android.app.notifications.userinbox

import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class UserInboxTest {

    private val fetchUserInboxItem = mockk<FetchUserInboxItem>(relaxed = true)
    private val migrateRiskyVenueIdProvider = mockk<MigrateRiskyVenueIdProvider>(relaxUnitFun = true)

    private fun createUserInbox(): UserInbox = UserInbox(
        fetchUserInboxItem,
        migrateRiskyVenueIdProvider,
    )

    private lateinit var testSubject: UserInbox

    @Before
    fun setUp() {
        testSubject = createUserInbox()
    }

    @Test
    fun `when user inbox is created, RiskyVenueIdProvider migration is performed`() {
        createUserInbox()

        verify { migrateRiskyVenueIdProvider() }
    }

    @Test
    fun `fetching an inbox item delegates call to FetchUserInboxItem`() {
        testSubject.fetchInbox()

        verify { fetchUserInboxItem() }
    }
}
