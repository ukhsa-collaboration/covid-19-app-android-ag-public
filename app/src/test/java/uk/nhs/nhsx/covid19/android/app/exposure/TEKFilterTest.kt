package uk.nhs.nhsx.covid19.android.app.exposure

import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.exposure.SubmitTemporaryExposureKeys.DateWindow
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import java.time.LocalDate
import kotlin.test.assertEquals

class TEKFilterTest {
    @Test
    fun `dateWindow is null`() {
        val keys = listOf(tek1, tek2)
        val filteredKeys = filterKeysInWindow(dateWindow = null, keys = keys)
        assertEquals(keys, filteredKeys)
    }

    @Test
    fun `filter out rollingPeriod that is not equal to 144`() {
        val keys = listOf(
            tekWithWrongRollingPeriod,
            tek2
        )
        val filteredKeys = filterKeysInWindow(dateWindow = DateWindow(LocalDate.parse("2020-08-05"), LocalDate.parse("2020-08-07")), keys = keys)
        assertEquals(listOf(tek2), filteredKeys)
    }

    @Test
    fun `filter out keys that are not in the window`() {
        val keys = listOf(
            tek1,
            tek2
        )
        val filteredKeys = filterKeysInWindow(dateWindow = DateWindow(LocalDate.parse("2020-08-06"), LocalDate.parse("2020-08-06")), keys = keys)
        assertEquals(listOf(tek1), filteredKeys)
    }

    companion object {
        val tek1 = NHSTemporaryExposureKey(
            "tek1",
            rollingStartNumber = 2661120,
            rollingPeriod = 144
        )
        val tek2 = NHSTemporaryExposureKey(
            "tek2",
            rollingStartNumber = 2661264,
            rollingPeriod = 144
        )
        val tekWithWrongRollingPeriod = NHSTemporaryExposureKey(
            "tekWithWrongRollingPeriod",
            rollingStartNumber = 2661120,
            rollingPeriod = 143
        )
    }
}
