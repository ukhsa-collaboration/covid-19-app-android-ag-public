package uk.nhs.nhsx.covid19.android.app.settings.myarea

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthority
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodes
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodesLoader
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeProvider
import uk.nhs.nhsx.covid19.android.app.settings.myarea.MyAreaViewModel.ViewState

class MyAreaViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val postCodeProvider = mockk<PostCodeProvider>()
    private val localAuthorityProvider = mockk<LocalAuthorityProvider>()
    private val localAuthorityPostCodesLoader = mockk<LocalAuthorityPostCodesLoader>()

    private val testSubject = MyAreaViewModel(
        postCodeProvider, localAuthorityProvider, localAuthorityPostCodesLoader
    )

    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    @Before
    fun setUp() {
        testSubject.viewState.observeForever(viewStateObserver)

        coEvery { localAuthorityPostCodesLoader.load() } returns localAuthorityPostCodes
    }

    @Test
    fun `when no post code or local authority are stored then observer emits null for both values`() {
        every { localAuthorityProvider.value } returns null
        every { postCodeProvider.value } returns null

        testSubject.onResume()

        verify { viewStateObserver.onChanged(ViewState(postCode = null, localAuthority = null)) }
    }

    @Test
    fun `when both post code and local authority id are stored then observer emits both post code and local authority name`() {
        every { localAuthorityProvider.value } returns localAuthorityId
        every { postCodeProvider.value } returns postCode

        testSubject.onResume()

        verify { viewStateObserver.onChanged(ViewState(postCode = postCode, localAuthority = localAuthorityName)) }
    }

    @Test
    fun `when loading local authority and post codes returns null then observer emits null for local authority name`() {
        coEvery { localAuthorityPostCodesLoader.load() } returns null
        every { localAuthorityProvider.value } returns localAuthorityId
        every { postCodeProvider.value } returns postCode

        testSubject.onResume()

        verify { viewStateObserver.onChanged(ViewState(postCode = postCode, localAuthority = null)) }
    }

    private val localAuthorityId = "localAuthorityId"
    private val localAuthorityName = "SomeAuthority"
    private val postCode = "postCode1"
    private val localAuthorityPostCodes = LocalAuthorityPostCodes(
        postcodes = mapOf(localAuthorityId to listOf(postCode, "postCode2")),
        localAuthorities = mapOf(localAuthorityId to LocalAuthority(localAuthorityName, "Somewhere"))
    )
}
