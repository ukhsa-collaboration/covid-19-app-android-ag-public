package uk.nhs.nhsx.covid19.android.app

import kotlinx.coroutines.delay
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_FAIL
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.ALWAYS_SUCCEED
import uk.nhs.nhsx.covid19.android.app.MockApiResponseType.FAIL_SUCCEED_LOOP

enum class MockApiResponseType { ALWAYS_FAIL, ALWAYS_SUCCEED, FAIL_SUCCEED_LOOP }

class MockApiBehaviour {
    var delayMillis: Long = 2000
    var responseType: MockApiResponseType = ALWAYS_FAIL

    private var previousSuccess = true

    suspend fun <T> invoke(onSuccess: (() -> T)): T {
        delay(delayMillis)
        return when (responseType) {
            ALWAYS_FAIL -> throw Exception()
            ALWAYS_SUCCEED -> onSuccess.invoke()
            FAIL_SUCCEED_LOOP -> {
                previousSuccess = !previousSuccess
                if (!previousSuccess) {
                    throw Exception()
                }
                onSuccess.invoke()
            }
        }
    }
}
