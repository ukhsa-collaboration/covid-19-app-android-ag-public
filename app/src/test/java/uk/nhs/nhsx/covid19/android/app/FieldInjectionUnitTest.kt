package uk.nhs.nhsx.covid19.android.app

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import uk.nhs.nhsx.covid19.android.app.di.ApplicationComponent

abstract class FieldInjectionUnitTest {

    protected val context = mockk<Context>(relaxed = true)
    private val applicationContext = mockk<ExposureApplication>()
    private val appComponent = mockk<ApplicationComponent>(relaxed = true)

    @Before
    open fun setUp() {
        every { context.applicationContext } returns applicationContext
        every { applicationContext.appComponent } returns appComponent
    }
}
