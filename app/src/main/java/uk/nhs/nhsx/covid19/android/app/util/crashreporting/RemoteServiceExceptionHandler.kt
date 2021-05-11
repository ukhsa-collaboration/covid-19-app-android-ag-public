package uk.nhs.nhsx.covid19.android.app.util.crashreporting

import com.jeroenmols.featureflag.framework.FeatureFlag.REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS
import com.jeroenmols.featureflag.framework.RuntimeBehavior
import timber.log.Timber
import java.lang.Thread.UncaughtExceptionHandler
import javax.inject.Inject

private const val REMOTE_SERVICE_EXCEPTION_CANONICAL_CLASS_NAME = "android.app.RemoteServiceException"

class RemoteServiceExceptionHandler @Inject constructor(
    private val remoteServiceExceptionReportRateLimiter: RemoteServiceExceptionReportRateLimiter,
    private val crashReportProvider: CrashReportProvider,
    private val defaultUncaughtExceptionHandler: SetDefaultUncaughtExceptionHandler,
    private val checkThrowableIsRemoteServiceException: CheckThrowableIsRemoteServiceException
) : UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        if (checkThrowableIsRemoteServiceException(throwable) && remoteServiceExceptionReportRateLimiter.isAllowed()) {
            Timber.d("RemoteServiceException was thrown. StackTrace: ${throwable.stackTraceToString()}")
            crashReportProvider.crashReport = CrashReport(
                exception = REMOTE_SERVICE_EXCEPTION_CANONICAL_CLASS_NAME,
                thread.name,
                throwable.stackTraceToString()
            )
        }
        throw throwable
    }

    fun initialize() {
        if (RuntimeBehavior.isFeatureEnabled(REMOTE_SERVICE_EXCEPTION_CRASH_ANALYTICS)) {
            defaultUncaughtExceptionHandler(this)
        }
    }
}

class CheckThrowableIsRemoteServiceException @Inject constructor() {
    operator fun invoke(throwable: Throwable) = throwable.javaClass.canonicalName == REMOTE_SERVICE_EXCEPTION_CANONICAL_CLASS_NAME
}
