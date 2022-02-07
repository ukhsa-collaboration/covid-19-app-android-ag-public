package uk.nhs.nhsx.covid19.android.app

import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.localstats.LocalStats
import uk.nhs.nhsx.covid19.android.app.remote.data.Direction
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalAuthorityData
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class RandomLocalStatsGenerator {
    fun generate(): LocalStats {

        return LocalStats(
            postCodeDistrict = setOf(ENGLAND, WALES).random(),
            localAuthorityName = setOf("Local Authority 1", "Local Authority 2").random(),
            lastFetch = Instant.now().minus((0..1440L).random(), ChronoUnit.MINUTES),
            countryNewCasesBySpecimenDateRollingRateLastUpdate = LocalDate.now().minusDays((7..10L).random()),
            localAuthorityNewCasesBySpecimenDateRollingRateLastUpdate = LocalDate.now().minusDays((7..10L).random()),
            localAuthorityNewCasesByPublishDateLastUpdate = LocalDate.now().minusDays((1..7L).random()),
            countryNewCasesBySpecimenDateRollingRate = setOf(
                null,
                BigDecimal("55.3"),
                BigDecimal("100.2"),
                BigDecimal("215.9")
            ).random(),
            localAuthorityData = LocalAuthorityData(
                newCasesByPublishDate = setOf(null, 67, 103, 205).random(),
                newCasesByPublishDateRollingSum = setOf(null, 567, 1103, 2045).random(),
                newCasesByPublishDateDirection = setOf(null, *Direction.values()).random(),
                newCasesByPublishDateChange = setOf(null, 33, 45, 203).random(),
                newCasesByPublishDateChangePercentage = setOf(null, BigDecimal("33.4"), BigDecimal("12.4"), BigDecimal("76.4")).random(),
                newCasesBySpecimenDateRollingRate = setOf(null, BigDecimal("133.4"), BigDecimal("312.4"), BigDecimal("476.4")).random(),
            )
        )
    }
}
