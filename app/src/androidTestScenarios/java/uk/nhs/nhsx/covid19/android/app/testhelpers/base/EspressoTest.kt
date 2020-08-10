package uk.nhs.nhsx.covid19.android.app.testhelpers.base

import android.app.Activity
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext

@RunWith(AndroidJUnit4::class)
abstract class EspressoTest {

    lateinit var testAppContext: TestApplicationContext

    @Before
    fun setup() {
        testAppContext = TestApplicationContext()
        testAppContext.reset()
    }

    @After
    fun teardown() {
        WorkManager.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)
            .cancelAllWork()
    }

    protected inline fun <reified T : Activity> startTestActivity(config: Intent.() -> Unit = {}): Activity? {
        val intent = Intent(testAppContext.app, T::class.java)
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK) }
            .apply(config)

        return InstrumentationRegistry
            .getInstrumentation()
            .startActivitySync(intent)
    }
}
