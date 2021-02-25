package uk.nhs.nhsx.covid19.android.app.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.remote.EmptyApi
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionRequest
import uk.nhs.nhsx.covid19.android.app.remote.data.EmptySubmissionSource
import javax.inject.Inject

class SubmitEmptyData(
    private val emptyApi: EmptyApi,
    private val obfuscationRateLimiter: RandomObfuscationRateLimiter,
    private val emptySubmissionScope: CoroutineScope,
    private val emptySubmissionDispatcher: CoroutineDispatcher
) {

    @Inject
    constructor(emptyApi: EmptyApi, obfuscationRateLimiter: RandomObfuscationRateLimiter) : this(
        emptyApi,
        obfuscationRateLimiter,
        emptySubmissionScope = GlobalScope,
        emptySubmissionDispatcher = Dispatchers.IO
    )

    operator fun invoke(emptySubmissionSource: EmptySubmissionSource, instances: Int = 1) {
        if (obfuscationRateLimiter.allow) {
            Timber.d("Allowing obfuscation calls")
            emptySubmissionScope.launch(emptySubmissionDispatcher) {
                runSafely {
                    for (callIndex in 1..instances) {
                        Timber.d("Empty submission [$callIndex of $instances] for $emptySubmissionSource")
                        emptyApi.submit(EmptySubmissionRequest(emptySubmissionSource))
                    }
                }
            }
        } else {
            Timber.d("Blocking obfuscation calls")
        }
    }
}
