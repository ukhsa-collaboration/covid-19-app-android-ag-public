package uk.nhs.nhsx.covid19.android.app.flow.analytics

import uk.nhs.nhsx.covid19.android.app.remote.data.Metrics
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.test.assertEquals
import kotlin.test.assertTrue

typealias MetricsProperty = KProperty1<Metrics, Int>

class FieldAsserter {

    private val fieldToAssertion: MutableMap<MetricsProperty, MetricsAssertion> = setupMap()

    private fun setupMap(): MutableMap<MetricsProperty, MetricsAssertion> {
        val map = mutableMapOf<MetricsProperty, MetricsAssertion>()
        Metrics::class.memberProperties.forEach {
            if (it.returnType.javaClass == Int::class) {
                map[it as MetricsProperty] = ImplicitAssertNotPresent(it)
            }
        }

        return removeUntrackedFields(map)
    }

    fun ignore(vararg properties: MetricsProperty) {
        properties.forEach {
            fieldToAssertion.remove(it)
        }
    }

    fun assertPresent(field: MetricsProperty) {
        fieldToAssertion[field] = AssertPresent(field)
    }

    fun assertEquals(expected: Int, field: MetricsProperty) {
        fieldToAssertion[field] = AssertEquals(expected, field)
    }

    fun assertLessThanTotalBackgroundTasks(field: MetricsProperty) {
        fieldToAssertion[field] = AssertLessThanTotalBackgroundTasks(field)
    }

    fun runAllAssertions(metrics: Metrics, day: Int? = null) {
        fieldToAssertion.values.forEach {
            it.assert(metrics, day)
        }
    }

    private fun removeUntrackedFields(map: MutableMap<MetricsProperty, MetricsAssertion>): MutableMap<MetricsProperty, MetricsAssertion> {
        map.remove(Metrics::hasSelfDiagnosedPositiveBackgroundTick)
        map.remove(Metrics::runningNormallyBackgroundTick)
        map.remove(Metrics::totalBackgroundTasks)
        return map
    }

    private interface MetricsAssertion {
        fun assert(metrics: Metrics, dayOfAssertion: Int?)

        fun formatDateForErrorMessage(dayOfAssertion: Int?): String {
            return if (dayOfAssertion == null) "" else "Failure occurred on day $dayOfAssertion."
        }
    }

    private class AssertPresent(private val field: MetricsProperty) : MetricsAssertion {

        override fun assert(metrics: Metrics, dayOfAssertion: Int?) {
            assertTrue(
                field.get(metrics) > 0,
                "AssertPresent failed, ${field.name} was not present. ${formatDateForErrorMessage(dayOfAssertion)}"
            )
        }
    }

    private class AssertEquals(
        private val expected: Int,
        private val field: MetricsProperty
    ) : MetricsAssertion {

        override fun assert(metrics: Metrics, dayOfAssertion: Int?) {
            val actual = field.get(metrics)
            assertEquals(
                expected,
                actual,
                "AssertEquals failed, expected ${field.name} to have value $expected but found $actual. ${formatDateForErrorMessage(dayOfAssertion)}"
            )
        }
    }

    private class AssertLessThanTotalBackgroundTasks(private val field: MetricsProperty) :
        MetricsAssertion {

        override fun assert(metrics: Metrics, dayOfAssertion: Int?) {
            val actual = field.get(metrics)
            assertTrue(
                actual < metrics.totalBackgroundTasks,
                "AssertLessThanTotalBackgroundTasks failed for ${field.name}, actually had value of $actual. ${formatDateForErrorMessage(dayOfAssertion)}"
            )
        }
    }

    private class ImplicitAssertNotPresent(private val field: MetricsProperty) : MetricsAssertion {

        override fun assert(metrics: Metrics, dayOfAssertion: Int?) {
            assertEquals(
                0,
                field.get(metrics),
                "Implicit AssertNotPresent failed, $field was unexpectedly present. ${formatDateForErrorMessage(dayOfAssertion)}"
            )
        }
    }
}
