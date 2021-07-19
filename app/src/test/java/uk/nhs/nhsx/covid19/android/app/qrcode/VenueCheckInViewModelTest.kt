package uk.nhs.nhsx.covid19.android.app.qrcode

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEvent.CanceledCheckIn
import uk.nhs.nhsx.covid19.android.app.analytics.AnalyticsEventProcessor
import uk.nhs.nhsx.covid19.android.app.qrcode.VenueCheckInViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.qrcode.riskyvenues.VisitedVenuesStorage
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class VenueCheckInViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val visitedVenuesStorage = mockk<VisitedVenuesStorage>(relaxUnitFun = true)
    private val analyticsEventProcessor = mockk<AnalyticsEventProcessor>(relaxed = true)

    private val visitRemoveResultObserver = mockk<Observer<RemoveVisitResult>>(relaxed = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxed = true)
    private val isViewStateCameraPermissionNotGrantedObserver = mockk<Observer<Unit>>(relaxed = true)

    private val testSubject = VenueCheckInViewModel(visitedVenuesStorage, analyticsEventProcessor, fixedClock)

    @Test
    fun `remove last visit should remove visit then invoke observer and process analytics event`() {
        testSubject.getVisitRemovedResult().observeForever(visitRemoveResultObserver)

        testSubject.removeLastVisit()

        coVerify { visitedVenuesStorage.removeLastVisit() }
        verify { visitRemoveResultObserver.onChanged(RemoveVisitResult) }
        verify { analyticsEventProcessor.track(CanceledCheckIn) }
    }

    @Test
    fun `onCreate with qr code scan result Success should return view state Success`() {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onCreate(QrCodeScanResult.Success("testVenue"))

        val expectedViewState = ViewState.Success("testVenue", LocalDateTime.now(fixedClock), playAnimation = true)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `onCreate with qr code scan result Success should return view state Success with playAnimation false if animation finished`() {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onCreate(QrCodeScanResult.Success("testVenue"))

        testSubject.onAnimationCompleted()

        val expectedViewState = ViewState.Success("testVenue", LocalDateTime.now(fixedClock), playAnimation = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `onCreate with qr code scan result CameraPermissionNotGranted should return view state CameraPermissionNotGranted`() {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onCreate(QrCodeScanResult.CameraPermissionNotGranted)

        verify { viewStateObserver.onChanged(ViewState.CameraPermissionNotGranted) }
    }

    @Test
    fun `onCreate with qr code scan result InvalidContent should return view state InvalidContent`() {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onCreate(QrCodeScanResult.InvalidContent)

        verify { viewStateObserver.onChanged(ViewState.InvalidContent) }
    }

    @Test
    fun `onCreate with qr code scan result ScanningNotSupported should return view state ScanningNotSupported`() {
        testSubject.viewState().observeForever(viewStateObserver)

        testSubject.onCreate(QrCodeScanResult.ScanningNotSupported)

        verify { viewStateObserver.onChanged(ViewState.ScanningNotSupported) }
    }

    @Test
    fun `onResume invokes isViewStateCameraPermissionNotGrantedLiveData when view state CameraPermissionNotGranted`() {
        testSubject.isViewStateCameraPermissionNotGranted()
            .observeForever(isViewStateCameraPermissionNotGrantedObserver)

        testSubject.onCreate(QrCodeScanResult.CameraPermissionNotGranted)

        testSubject.onResume()

        verify { isViewStateCameraPermissionNotGrantedObserver.onChanged(null) }
    }

    @Test
    fun `onResume does not invoke isViewStateCameraPermissionNotGrantedLiveData when view state is not CameraPermissionNotGranted`() {
        testSubject.isViewStateCameraPermissionNotGranted()
            .observeForever(isViewStateCameraPermissionNotGrantedObserver)

        testSubject.onCreate(QrCodeScanResult.ScanningNotSupported)

        testSubject.onResume()

        verify(exactly = 0) { isViewStateCameraPermissionNotGrantedObserver.onChanged(any()) }
    }

    companion object {
        private val fixedClock = Clock.fixed(Instant.parse("2020-12-24T01:00:00.00Z"), ZoneOffset.UTC)
    }
}
