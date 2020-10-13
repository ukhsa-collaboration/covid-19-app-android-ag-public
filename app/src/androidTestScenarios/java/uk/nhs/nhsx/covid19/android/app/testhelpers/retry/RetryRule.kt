package uk.nhs.nhsx.covid19.android.app.testhelpers.retry

import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.report.isReporterRunning
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext
import java.lang.Exception

class RetryRule(
    private val testAppContext: TestApplicationContext,
    private val retryCount: Int = 3
) : MethodRule {

    override fun apply(base: Statement, method: FrameworkMethod, target: Any): Statement {

        return object : Statement() {
            override fun evaluate() {

                try {
                    base.evaluate()
                    return
                } catch (t: Throwable) {
                    if (shallRetry(method) && !isReporterRunning()) {
                        repeat(retryCount) {
                            try {
                                Timber.d("re-running test ${method.name} attempt number: ${it + 1}")
                                base.evaluate()
                                return
                            } catch (t: Throwable) {
                                if (it == retryCount - 1) throw FlakyTestRetryLimitExceededException(t)
                                testAppContext.device.pressBack()
                                testAppContext.reset()
                            }
                        }
                    } else {
                        throw t
                    }
                }
            }
        }
    }

    private fun shallRetry(method: FrameworkMethod) =
        method.getAnnotation(RetryFlakyTest::class.java) != null
}

class FlakyTestRetryLimitExceededException(override val cause: Throwable?) : Exception()
