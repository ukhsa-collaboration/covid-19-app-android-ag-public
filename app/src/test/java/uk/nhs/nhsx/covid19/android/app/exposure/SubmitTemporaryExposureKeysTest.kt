package uk.nhs.nhsx.covid19.android.app.exposure

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success
import uk.nhs.nhsx.covid19.android.app.remote.KeysSubmissionApi
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SubmitTemporaryExposureKeysTest {

    private val keysSubmissionApi = mockk<KeysSubmissionApi>(relaxed = true)

    private val testSubject = SubmitTemporaryExposureKeys(keysSubmissionApi)

    @Test
    fun `submission returns failure because key submission throws exception`() = runBlocking {
        coEvery { keysSubmissionApi.submitGeneratedKeys(any()) } throws Exception()

        val result = testSubject(listOf(), "diagnosis_submission_token")

        assertTrue { result is Failure }
    }

    @Test
    fun `submission returns success`() = runBlocking {
        coEvery { keysSubmissionApi.submitGeneratedKeys(any()) } returns Unit

        val result = testSubject(listOf(), "diagnosis_submission_token")

        assertEquals(Success(Unit), result)
    }
}
