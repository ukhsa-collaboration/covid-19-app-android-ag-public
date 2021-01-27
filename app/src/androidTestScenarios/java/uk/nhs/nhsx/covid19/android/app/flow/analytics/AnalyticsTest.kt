package uk.nhs.nhsx.covid19.android.app.flow.analytics

import androidx.annotation.CallSuper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkInfo.State.ENQUEUED
import androidx.work.WorkInfo.State.RUNNING
import androidx.work.WorkManager
import com.jeroenmols.featureflag.framework.FeatureFlagTestHelper
import com.jeroenmols.featureflag.framework.TestSetting.USE_WEB_VIEW_FOR_INTERNAL_BROWSER
import org.junit.After
import org.junit.Before
import org.junit.Rule
import uk.nhs.nhsx.covid19.android.app.MainActivity
import uk.nhs.nhsx.covid19.android.app.common.PeriodicTask.PERIODIC_TASKS
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext.Companion.ENGLISH_LOCAL_AUTHORITY
import uk.nhs.nhsx.covid19.android.app.testhelpers.base.EspressoTest
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import java.util.concurrent.TimeoutException
import kotlin.test.assertNotNull

abstract class AnalyticsTest : EspressoTest() {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @CallSuper
    @Before
    open fun setUp() {
        FeatureFlagTestHelper.enableFeatureFlag(USE_WEB_VIEW_FOR_INTERNAL_BROWSER)

        testAppContext.clock.currentInstant =
            LocalDate.of(2020, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC)
        testAppContext.setLocalAuthority(ENGLISH_LOCAL_AUTHORITY)
        startTestActivity<MainActivity>()
    }

    @After
    open fun tearDown() {
        testAppContext.clock.reset()
        testAppContext.virologyTestingApi.reset()
    }

    // Metrics Assertions

    fun assertAnalyticsPacketIsNormal() {
        advanceToEndOfAnalyticsWindow()
        val lastRequest = getLastAnalyticsPayload()
        FieldAsserter().runAllAssertions(lastRequest.metrics)
    }

    fun assertOnFieldsForDateRange(dateRange: IntRange, function: FieldAsserter.() -> Unit) {
        dateRange.forEach {
            advanceToEndOfAnalyticsWindow()
            assertOnLastPacket(function, it)
        }
    }

    fun assertOnFields(function: FieldAsserter.() -> Unit) {
        advanceToEndOfAnalyticsWindow()
        assertOnLastPacket(function)
    }

    fun assertOnLastFields(function: FieldAsserter.() -> Unit) {
        assertOnLastPacket(function)
    }

    private fun assertOnLastPacket(function: FieldAsserter.() -> Unit, day: Int? = null) {
        val fieldAsserter = FieldAsserter()
        function(fieldAsserter)
        fieldAsserter.runAllAssertions(getLastAnalyticsPayload().metrics, day)
    }

    private fun getLastAnalyticsPayload() =
        testAppContext.analyticsApi.lastRequest().getOrAwaitValue()

    // Time Manipulation

    fun advanceToNextBackgroundTaskExecution() {
        advanceClock(60 * 60 * 4)
    }

    private fun advanceToEndOfAnalyticsWindow(steps: Int = 4) {
        val currentDate = testAppContext.clock.instant().atZone(ZoneOffset.UTC)

        val endOfAnalyticsWindow = currentDate.plusDays(1)
        val differenceToEndOfWindow =
            ChronoUnit.SECONDS.between(testAppContext.clock.instant(), endOfAnalyticsWindow)
        val secondsToAdvance = kotlin.math.ceil(differenceToEndOfWindow / steps.toDouble()).toLong()

        while (testAppContext.clock.instant().atZone(ZoneOffset.UTC) < endOfAnalyticsWindow) {
            advanceClock(secondsToAdvance)
        }
    }

    private fun advanceClock(secondsToAdvance: Long) {
        testAppContext.clock.currentInstant =
            testAppContext.clock.instant().plusSeconds(secondsToAdvance)
        testAppContext.getCurrentState()
        runBackgroundTasks()
    }

    fun runBackgroundTasks() {
        testAppContext.getPeriodicTasks().schedule()
        WorkManager.getInstance(testAppContext.app)
            .getWorkInfosForUniqueWorkLiveData(PERIODIC_TASKS.workName)
            .awaitSuccess()
    }
}

/* Copyright 2019 Google LLC.
   SPDX-License-Identifier: Apache-2.0 */
fun <T : Any> LiveData<T>.getOrAwaitValue(
    time: Long = 5,
    timeUnit: TimeUnit = SECONDS
): T {
    var data: T? = null
    val latch = CountDownLatch(1)
    val observer = object : Observer<T> {
        override fun onChanged(o: T?) {
            data = o
            latch.countDown()
            this@getOrAwaitValue.removeObserver(this)
        }
    }

    this.observeForever(observer)

    // Don't wait indefinitely if the LiveData is not set.
    if (!latch.await(time, timeUnit)) {
        throw TimeoutException("LiveData value was never set.")
    }

    assertNotNull(data)
    return data!!
}

fun LiveData<List<WorkInfo>>.awaitSuccess(
    time: Long = 5,
    timeUnit: TimeUnit = SECONDS
) {
    var hasStartedRunning = false
    val latch = CountDownLatch(1)
    val observer = object : Observer<List<WorkInfo>> {
        override fun onChanged(o: List<WorkInfo>?) {
            if (o != null) {
                if (o.any { it.state == ENQUEUED } && hasStartedRunning) {
                    latch.countDown()
                    InstrumentationRegistry.getInstrumentation().runOnMainSync {
                        this@awaitSuccess.removeObserver(this)
                    }
                }
                if (o.any { it.state == RUNNING }) {
                    hasStartedRunning = true
                }
            }
        }
    }

    this.observeForever(observer)

    if (!latch.await(time, timeUnit)) {
        throw TimeoutException("Work never completed successfully")
    }
}
