package uk.nhs.nhsx.covid19.android.app.testhelpers.base

import android.app.Activity
import android.content.Intent
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.di.MockApiModule
import uk.nhs.nhsx.covid19.android.app.testhelpers.AWAIT_AT_MOST_SECONDS
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryRule
import uk.nhs.nhsx.covid19.android.app.util.ScreenshotTakingRule

@RunWith(AndroidJUnit4::class)
abstract class EspressoTest {

    var testAppContext: TestApplicationContext = TestApplicationContext()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val screenshotRule = ScreenshotTakingRule()

    @get:Rule
    var rule = RetryRule(testAppContext, retryCount = 3)

    @Before
    fun setup() {
        testAppContext.reset()
    }

    @Before
    fun setUpMockApiBehaviour() {
        MockApiModule.behaviour.apply {
            delayMillis = 0
            responseType = ALWAYS_SUCCEED
        }
    }

    @After
    fun teardown() {
        WorkManager.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
            .cancelAllWork()
        testAppContext.encryptedStorage.sharedPreferences.edit(commit = true) { clear() }
    }

    protected fun runBackgroundTasks() {
        testAppContext.runBackgroundTasks()
    }

    protected inline fun <reified T : Activity> startTestActivity(
        config: Intent.() -> Unit = {}
    ): Activity? {
        val intent = Intent(testAppContext.app, T::class.java)
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
            .apply(config)

        return InstrumentationRegistry
            .getInstrumentation()
            .startActivitySync(intent)
    }

    fun waitFor(idleTime: Long = AWAIT_AT_MOST_SECONDS, assertion: () -> Unit) {
        uk.nhs.nhsx.covid19.android.app.testhelpers.waitFor(idleTime, assertion)
    }
}
