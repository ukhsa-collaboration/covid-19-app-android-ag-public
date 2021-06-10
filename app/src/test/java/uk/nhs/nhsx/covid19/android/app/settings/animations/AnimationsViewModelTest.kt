package uk.nhs.nhsx.covid19.android.app.settings.animations

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Rule
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.settings.animations.AnimationsViewModel.ViewState
import uk.nhs.nhsx.covid19.android.app.util.viewutils.AreSystemLevelAnimationsEnabled

class AnimationsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val animationsProvider = mockk<AnimationsProvider>(relaxUnitFun = true)
    private val areSystemLevelAnimationsEnabled = mockk<AreSystemLevelAnimationsEnabled>(relaxUnitFun = true)
    private val viewStateObserver = mockk<Observer<ViewState>>(relaxUnitFun = true)

    @Test
    fun `when onResume is called with inAppAnimation enabled and systemLevelAnimationsEnabled, update view state to true`() {
        every { animationsProvider.inAppAnimationEnabled } returns true
        every { areSystemLevelAnimationsEnabled.invoke() } returns true

        val testSubject = setUpTestSubject()

        testSubject.onResume()

        val expectedViewState = ViewState(animationsEnabled = true, showDialog = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when onResume is called with inAppAnimation disabled and systemLevelAnimationsEnabled, update view state to false`() {
        every { animationsProvider.inAppAnimationEnabled } returns false
        every { areSystemLevelAnimationsEnabled.invoke() } returns true

        val testSubject = setUpTestSubject()

        testSubject.onResume()

        val expectedViewState = ViewState(animationsEnabled = false, showDialog = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when onResume is called with inAppAnimation enabled and systemLevelAnimationsEnabled is false, update view state to false`() {
        every { animationsProvider.inAppAnimationEnabled } returns true
        every { areSystemLevelAnimationsEnabled.invoke() } returns false

        val testSubject = setUpTestSubject()

        testSubject.onResume()

        val expectedViewState = ViewState(animationsEnabled = false, showDialog = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when onResume is called with inAppAnimation disabled and systemLevelAnimationsEnabled is false, update view state to false`() {
        every { animationsProvider.inAppAnimationEnabled } returns false
        every { areSystemLevelAnimationsEnabled.invoke() } returns false

        val testSubject = setUpTestSubject()

        testSubject.onResume()

        val expectedViewState = ViewState(animationsEnabled = false, showDialog = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    @Test
    fun `when inAppAnimations are enabled and systemLevelAnimations are disabled, onHomeScreenAnimationToggleClicked should not update provider`() {
        every { animationsProvider.inAppAnimationEnabled } returns true
        every { areSystemLevelAnimationsEnabled.invoke() } returns false

        val testSubject = setUpTestSubject()

        testSubject.onAnimationToggleClicked()

        val expectedViewState = ViewState(animationsEnabled = false, showDialog = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
        verify(exactly = 0) { animationsProvider setProperty "inAppAnimationEnabled" value any<Boolean>() }
    }

    @Test
    fun `when inAppAnimations are disabled and systemLevelAnimations are enabled, onHomeScreenAnimationToggleClicked should update to true`() {
        every { animationsProvider.inAppAnimationEnabled } returnsMany listOf(false, false, true)
        every { areSystemLevelAnimationsEnabled.invoke() } returns true

        val testSubject = setUpTestSubject()

        testSubject.onAnimationToggleClicked()

        val expectedViewState = ViewState(animationsEnabled = true, showDialog = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
        verify { animationsProvider setProperty "inAppAnimationEnabled" value eq(true) }
    }

    @Test
    fun `when inAppAnimations are enabled and systemLevelAnimations are enabled, onHomeScreenAnimationToggleClicked should update to false`() {
        every { animationsProvider.inAppAnimationEnabled } returnsMany listOf(false, true, false)
        every { areSystemLevelAnimationsEnabled.invoke() } returns true

        val testSubject = setUpTestSubject()

        testSubject.onAnimationToggleClicked()

        val expectedViewState = ViewState(animationsEnabled = false, showDialog = false)

        verify { viewStateObserver.onChanged(expectedViewState) }
        verify { animationsProvider setProperty "inAppAnimationEnabled" value eq(false) }
    }

    @Test
    fun `when inAppAnimations are disabled and systemLevelAnimations are disabled, onHomeScreenAnimationToggleClicked should not update provider`() {
        every { animationsProvider.inAppAnimationEnabled } returns false
        every { areSystemLevelAnimationsEnabled.invoke() } returns false

        val testSubject = setUpTestSubject()

        testSubject.onAnimationToggleClicked()

        verify(exactly = 0) { animationsProvider setProperty "inAppAnimationEnabled" value any<Boolean>() }
    }

    @Test
    fun `when inAppAnimations and systemLevelAnimations are disabled, onHomeScreenAnimationToggleClicked, showDialog is true and animationsEnabled is false`() {
        every { animationsProvider.inAppAnimationEnabled } returns false
        every { areSystemLevelAnimationsEnabled.invoke() } returns false

        val testSubject = setUpTestSubject()

        testSubject.onAnimationToggleClicked()

        val expectedViewState = ViewState(animationsEnabled = false, showDialog = true)

        verify { viewStateObserver.onChanged(expectedViewState) }
    }

    private fun setUpTestSubject(): AnimationsViewModel {
        val testSubject = AnimationsViewModel(animationsProvider, areSystemLevelAnimationsEnabled)
        testSubject.viewState().observeForever(viewStateObserver)
        return testSubject
    }
}
