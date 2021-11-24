package uk.nhs.nhsx.covid19.android.app.remote.data

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.AMBER
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.BLACK
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.GREEN
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.MAROON
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.NEUTRAL
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.RED
import uk.nhs.nhsx.covid19.android.app.remote.data.ColorScheme.YELLOW
import java.util.stream.Stream
import kotlin.test.assertEquals

internal class ColorSchemeToImageResourceTest {

    private val subject = ColorSchemeToImageResource()

    @MethodSource("testData")
    @ParameterizedTest(name = "{0} converts to the correct image resource")
    fun isTransformCorrect(scheme: ColorScheme, expectedImageResource: Int) {
        assertEquals(expected = expectedImageResource, actual = subject(scheme))
    }

    companion object {
        @JvmStatic
        fun testData(): Stream<Array<Any>> = Stream.of(
            arrayOf(NEUTRAL, R.drawable.ic_map_risk_neutral),
            arrayOf(GREEN, R.drawable.ic_map_risk_green),
            arrayOf(YELLOW, R.drawable.ic_map_risk_yellow),
            arrayOf(AMBER, R.drawable.ic_map_risk_amber),
            arrayOf(RED, R.drawable.ic_map_risk_red),
            arrayOf(MAROON, R.drawable.ic_map_risk_maroon),
            arrayOf(BLACK, R.drawable.ic_map_risk_black),
        )
    }
}
