package uk.nhs.nhsx.covid19.android.app.status.localmessage

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessageTranslation

class LocalMessageViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val getLocalMessageFromStorage = mockk<GetLocalMessageFromStorage>()
    private val localMessage: LocalMessageTranslation = mockk()

    private val localMessageObserver = mockk<Observer<LocalMessageTranslation?>>(relaxUnitFun = true)

    private val testSubject = LocalMessageViewModel(getLocalMessageFromStorage)

    @Before
    fun setUp() {
        coEvery { getLocalMessageFromStorage.invoke() } returns localMessage
        testSubject.viewState().observeForever(localMessageObserver)
    }

    @Test
    fun `fetches local message in onCreate`() = runBlocking {
        testSubject.onCreate()

        verify { localMessageObserver.onChanged(localMessage) }
    }
}
