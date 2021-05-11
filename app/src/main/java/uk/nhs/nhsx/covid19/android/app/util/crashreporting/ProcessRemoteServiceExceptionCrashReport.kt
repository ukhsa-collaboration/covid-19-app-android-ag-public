package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import com.jeroenmols.featureflag.framework.FeatureFlag.REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import javax.inject.Inject

class ProcessRemoteServiceExceptionCrashReport @Inject constructor(
    private val crashReportProvider: CrashReportProvider,
    private val submitRemoteServiceExceptionCrashReport: SubmitRemoteServiceExceptionCrashReport
) {
    suspend operator fun invoke() {
        if (RuntimeBehavior.isFeatureEnabled(REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS)) {
            crashReportProvider.crashReport?.let {
                submitRemoteServiceExceptionCrashReport(it)
            }
        }
        crashReportProvider.clear()
    }
}
