package uk.nhs.nhsx.covid19.android.app.testhelpers.base

import android.app.Activity
import android.content.Intent
import androidx.test.espresso.EspressoException
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.schibsted.spain.barista.internal.failurehandler.BaristaException
import org.awaitility.kotlin.await
import org.awaitility.kotlin.ignoreExceptionsMatching
import org.awaitility.kotlin.untilAsserted
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import uk.nhs.nhsx.covid19.android.app.testhelpers.retry.RetryRule
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import uk.nhs.nhsx.covid19.android.app.util.ScreenshotTakingRule
import java.util.concurrent.TimeUnit.SECONDS

@RunWith(AndroidJUnit4::class)
abstract class EspressoTest {

    var testAppContext: TestApplicationContext = TestApplicationContext()

    @get:Rule
    val screenshotRule = ScreenshotTakingRule()

    @get:Rule
    var rule = RetryRule(testAppContext, retryCount = 3)

    @Before
    fun setup() {
        testAppContext.reset()
    }

    @After
    fun teardown() {
        WorkManager.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
            .cancelAllWork()
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

    protected fun waitFor(idleTime: Long = 10L, assertion: () -> Unit) {
        await.atMost(
            idleTime, SECONDS
        ) ignoreExceptionsMatching {
            it is BaristaException || it is EspressoException
        } untilAsserted assertion
    }
}
