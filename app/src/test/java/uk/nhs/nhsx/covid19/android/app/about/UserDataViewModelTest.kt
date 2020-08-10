package uk.nhs.nhsx.covid19.android.app.about

import android.content.SharedPreferences
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import uk.nhs.nhsx.covid19.android.app.onboarding.authentication.AuthenticationProvider
import uk.nhs.nhsx.covid19.android.app.onboarding.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueVisit
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResult
import uk.nhs.nhsx.covid19.android.app.testordering.LatestTestResultProvider

class UserDataViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val postCodeProvider = mockk<PostCodeProvider>(relaxed = true)
    private val venuesStorage = mockk<VisitedVenuesStorage>(relaxed = true)
    private val stateMachine = mockk<IsolationStateMachine>(relaxed = true)
    private val latestTestResultProvider = mockk<LatestTestResultProvider>(relaxed = true)
    private val sharedPreferences = mockk<SharedPreferences>()
    private val sharedPreferencesEditor = mockk<SharedPreferences.Editor>()
    private val sharedPreferencesDeletedDataEditor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val authenticationCodeProvider = mockk<AuthenticationProvider>(relaxed = true)

    private val testSubject = UserDataViewModel(
        postCodeProvider,
        venuesStorage,
        stateMachine,
        latestTestResultProvider,
        sharedPreferences,
        authenticationCodeProvider
    )

    private val postCodeObserver = mockk<Observer<String>>(relaxed = true)
    private val venueVisitsObserver = mockk<Observer<List<VenueVisit>>>(relaxed = true)
    private val stateMachineStateObserver = mockk<Observer<State>>(relaxed = true)
    private val latestTestResultObserver = mockk<Observer<LatestTestResult>>(relaxed = true)

    @Test
    fun `post code updated`() = runBlocking {

        val code = "SD12"
        coEvery { postCodeProvider.value } returns code

        testSubject.getPostCode().observeForever(postCodeObserver)

        testSubject.loadUserData()

        verify { postCodeObserver.onChanged(code) }
    }

    @Test
    fun `venue visits updated`() = runBlocking {
        coEvery { venuesStorage.getVisits() } returns listOf()

        testSubject.getVenueVisits().observeForever(venueVisitsObserver)

        testSubject.loadUserData()

        verify { venueVisitsObserver.onChanged(listOf()) }
    }

    @Test
    fun `status machine state updated`() = runBlocking {
        coEvery { stateMachine.readState() } returns State.Default()

        testSubject.getLastStatusMachineState().observeForever(stateMachineStateObserver)

        testSubject.loadUserData()

        verify { stateMachineStateObserver.onChanged(State.Default()) }
    }

    @Test
    fun `latest test result state updated`() = runBlocking {
        coEvery { latestTestResultProvider.latestTestResult } returns any()

        testSubject.getLatestTestResult().observeForever(latestTestResultObserver)

        testSubject.loadUserData()

        verify { latestTestResultObserver.onChanged(any()) }
    }

    @Test
    fun `delete removes data from storage`() {
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.clear() } returns sharedPreferencesDeletedDataEditor

        testSubject.deleteAllUserData()

        verify { venuesStorage.removeAllVenueVisits() }
        verify { sharedPreferencesEditor.clear() }
        verify { sharedPreferencesDeletedDataEditor.apply() }
    }

    @Test
    fun `delete keeps authentication status`() {
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        every { sharedPreferencesEditor.clear() } returns sharedPreferencesDeletedDataEditor
        every { authenticationCodeProvider.value } returns true

        testSubject.deleteAllUserData()

        verify { authenticationCodeProvider.value = true }
    }
}
