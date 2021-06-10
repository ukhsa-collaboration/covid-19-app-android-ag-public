package uk.nhs.nhsx.covid19.android.app.about

import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class UpdateAreaInfoTest {

    private val context = mockk<Context>()
    private val workManager = mockk<WorkManager>(relaxed = true)

    private val testSubject = UpdateAreaInfo(context)

    @Before
    fun setUp() {
        mockkStatic(WorkManager::class)
    }

    @Test
    fun `schedule area risk update`() = runBlocking {
        every { WorkManager.getInstance(context) } returns workManager

        testSubject.schedule()

        verify { workManager.enqueue(any<OneTimeWorkRequest>()) }
    }
}
