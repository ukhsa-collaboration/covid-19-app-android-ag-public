package uk.nhs.nhsx.covid19.android.app.state

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.common.postcode.LocalAuthorityPostCodeProvider
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.NORTHERN_IRELAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.SCOTLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.remote.data.CountrySpecificConfiguration
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import kotlin.test.assertEquals

class CreateIsolationConfigurationTest {

    @MockK
    private lateinit var localAuthorityPostCodeProvider: LocalAuthorityPostCodeProvider

    private lateinit var createIsolationConfiguration: CreateIsolationConfiguration

    val durationDays = DurationDays(
        england = CountrySpecificConfiguration(
            contactCase = 1,
            indexCaseSinceSelfDiagnosisOnset = 2,
            indexCaseSinceSelfDiagnosisUnknownOnset = 3,
            maxIsolation = 4,
            pendingTasksRetentionPeriod = 5,
            indexCaseSinceTestResultEndDate = 6,
            testResultPollingTokenRetentionPeriod = 7
        ),
        wales = CountrySpecificConfiguration(
            contactCase = 8,
            indexCaseSinceSelfDiagnosisOnset = 9,
            indexCaseSinceSelfDiagnosisUnknownOnset = 10,
            maxIsolation = 11,
            pendingTasksRetentionPeriod = 12,
            indexCaseSinceTestResultEndDate = 13,
            testResultPollingTokenRetentionPeriod = 14
        )
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        createIsolationConfiguration = CreateIsolationConfiguration(localAuthorityPostCodeProvider)
    }

    @Test
    fun `isolation configuration is created for England`() {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns ENGLAND

        val actualIsolationConfiguration = createIsolationConfiguration(durationDays)

        val expectedIsolationConfigurationForEngland =
            IsolationConfiguration(
                contactCase = 1,
                indexCaseSinceSelfDiagnosisOnset = 2,
                indexCaseSinceSelfDiagnosisUnknownOnset = 3,
                maxIsolation = 4,
                pendingTasksRetentionPeriod = 5,
                indexCaseSinceTestResultEndDate = 6,
                testResultPollingTokenRetentionPeriod = 7
            )
        assertEquals(expectedIsolationConfigurationForEngland, actualIsolationConfiguration)
    }

    @Test
    fun `isolation configuration is created for Wales`() {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns WALES

        val actualIsolationConfiguration = createIsolationConfiguration(durationDays)

        val expectedIsolationConfigurationForWales = IsolationConfiguration(
            contactCase = 8,
            indexCaseSinceSelfDiagnosisOnset = 9,
            indexCaseSinceSelfDiagnosisUnknownOnset = 10,
            maxIsolation = 11,
            pendingTasksRetentionPeriod = 12,
            indexCaseSinceTestResultEndDate = 13,
            testResultPollingTokenRetentionPeriod = 14
        )
        assertEquals(expectedIsolationConfigurationForWales, actualIsolationConfiguration)
    }

    @Test
    fun `use default isolation configuration when country is Scotland (not possible)`() {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns SCOTLAND

        val actualIsolationConfiguration = createIsolationConfiguration(durationDays)

        val expectedIsolationConfiguration = IsolationConfiguration(
            contactCase = 0,
            indexCaseSinceSelfDiagnosisOnset = 0,
            indexCaseSinceSelfDiagnosisUnknownOnset = 0,
            maxIsolation = 0,
            indexCaseSinceTestResultEndDate = 0,
            pendingTasksRetentionPeriod = 0,
            testResultPollingTokenRetentionPeriod = 0
        )
        assertEquals(expectedIsolationConfiguration, actualIsolationConfiguration)
    }

    @Test
    fun `use default isolation configuration when country is Northern Ireland (not possible)`() {
        coEvery { localAuthorityPostCodeProvider.getPostCodeDistrict() } returns NORTHERN_IRELAND

        val actualIsolationConfiguration = createIsolationConfiguration(durationDays)

        val expectedIsolationConfiguration = IsolationConfiguration(
            contactCase = 0,
            indexCaseSinceSelfDiagnosisOnset = 0,
            indexCaseSinceSelfDiagnosisUnknownOnset = 0,
            maxIsolation = 0,
            indexCaseSinceTestResultEndDate = 0,
            pendingTasksRetentionPeriod = 0,
            testResultPollingTokenRetentionPeriod = 0
        )
        assertEquals(expectedIsolationConfiguration, actualIsolationConfiguration)
    }
}
