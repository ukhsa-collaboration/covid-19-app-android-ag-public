package uk.nhs.nhsx.covid19.android.app.testhelpers.retry

import org.junit.rules.MethodRule
import org.junit.runners.model.FrameworkMethod
import org.junit.runners.model.Statement
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext

class RepeatRule(
    private val testAppContext: TestApplicationContext
) : MethodRule {

    override fun apply(base: Statement, method: FrameworkMethod, target: Any): Statement {

        return object : Statement() {
            override fun evaluate() {
                if (!shallRepeat(method)) {
                    base.evaluate()
                } else {
                    val times = method.getAnnotation(Repeat::class.java).times
                    repeat(times) {
                        Timber.d("Running test ${method.name} attempt number: ${it + 1}")
                        base.evaluate()
                        testAppContext.device.pressBack()
                        testAppContext.reset()
                    }
                }
            }
        }
    }

    private fun shallRepeat(method: FrameworkMethod) =
        method.getAnnotation(Repeat::class.java) != null
}

@Retention(AnnotationRetention.RUNTIME)
annotation class Repeat(val times: Int = 10)
