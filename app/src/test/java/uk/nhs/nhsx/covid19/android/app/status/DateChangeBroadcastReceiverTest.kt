package uk.nhs.nhsx.covid19.android.app.status

import android.app.Activity
import android.content.Context
import android.content.Intent
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals

class DateChangeBroadcastReceiverTest {

    private val callback = mockk<() -> Unit>()
    private val context = mockk<Context>()
    private val intent = mockk<Intent>()

    private val testSubject = DateChangeBroadcastReceiver()

    init {
        testSubject.callback = callback
        every { callback() } returns Unit
    }

    @Test
    fun `onReceive invokes callback`() {
        testSubject.onReceive(context, intent)

        verify { callback.invoke() }
    }

    @Test
    fun `registerReceiver sets callback and registers receiver`() {
        val activity = mockk<Activity>(relaxed = true)
        val newCallback = mockk<() -> Unit>()

        testSubject.registerReceiver(activity, newCallback)

        assertEquals(newCallback, testSubject.callback)

        verify { activity.registerReceiver(testSubject, any()) }
    }

    @Test
    fun `unregisterReceiver clears callback and unregisters receiver`() {
        val activity = mockk<Activity>(relaxed = true)

        testSubject.unregisterReceiver(activity)

        assertEquals(null, testSubject.callback)

        verify { activity.unregisterReceiver(testSubject) }
    }
}
