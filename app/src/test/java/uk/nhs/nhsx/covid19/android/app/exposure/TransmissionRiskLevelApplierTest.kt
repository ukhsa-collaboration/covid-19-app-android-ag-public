package uk.nhs.nhsx.covid19.android.app.exposure

import io.mockk.every
import io.mockk.mockk
import org.junit.Before
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.state.IsolationStateMachine
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation
import uk.nhs.nhsx.covid19.android.app.state.State.Isolation.IndexCase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import kotlin.test.assertEquals

class TransmissionRiskLevelApplierTest {
    private val stateMachine = mockk<IsolationStateMachine>()
    private lateinit var transmissionRiskLevelApplier: TransmissionRiskLevelApplier

    @Before
    fun setup() {
        transmissionRiskLevelApplier = TransmissionRiskLevelApplier(stateMachine = stateMachine)
    }

    // Transmission risk level = MAX transmission risk level - daysFromOnset
    @Test
    fun `apply transmission risk level based on distance from onset day`() {
        // Given
        val filteredKeys = generateKeys(from = 10, to = 13)
        setOnsetDate(LocalDate.of(2020, 7, 12))

        // When
        val keys = transmissionRiskLevelApplier.applyTransmissionRiskLevels(filteredKeys)

        // Then
        val expectedRiskLevels = mutableListOf(5, 6, 7, 6)
        keys.sortedBy { it.rollingStartNumber }
            .forEach { key ->
                val expectedRiskLevel = expectedRiskLevels.removeAt(0)
                assertEquals(expectedRiskLevel, key.transmissionRiskLevel)
            }
    }

    @Test
    fun `min transmission risk level is 0`() {
        // Given
        val filteredKeys = generateKeys(from = 2, to = 13)
        setOnsetDate(LocalDate.of(2020, 7, 4))

        // When
        val keys = transmissionRiskLevelApplier.applyTransmissionRiskLevels(filteredKeys)

        // Then
        val expectedRiskLevels = mutableListOf(5, 6, 7, 6, 5, 4, 3, 2, 1, 0, 0, 0)
        keys.sortedBy { it.rollingStartNumber }
            .forEach { key ->
                val expectedRiskLevel = expectedRiskLevels.removeAt(0)
                assertEquals(expectedRiskLevel, key.transmissionRiskLevel)
            }
    }

    @Test
    fun `transmission risk level for `() {
        // Given
        val generatedKeys = generateKeys(from = 1, to = 14)
        setOnsetDate(LocalDate.of(2020, 7, 8))

        // When
        val keys = transmissionRiskLevelApplier.applyTransmissionRiskLevels(generatedKeys)

        // Then
        //                                     1  2  3  4  5  6  7  8  9  10 11 12 13 14
        val expectedRiskLevels = mutableListOf(0, 0, 0, 0, 0, 5, 6, 7, 6, 5, 4, 3, 2, 1)
        keys.sortedBy { it.rollingStartNumber }
            .forEach { key ->
                val expectedRiskLevel = expectedRiskLevels.removeAt(0)
                assertEquals(expectedRiskLevel, key.transmissionRiskLevel)
            }
    }

    @Test
    fun `returns risk level 0 when isolation state does not have index case`() {
        // Given
        every { stateMachine.readState(any()) } returns isolationWithoutIndexCase()

        // When
        val keys = transmissionRiskLevelApplier.applyTransmissionRiskLevels(generateKeys(from = 1, to = 5))

        // Then
        keys.forEach { key ->
            assertEquals(0, key.transmissionRiskLevel)
        }
    }

    private fun generateKeys(from: Int, to: Int): List<NHSTemporaryExposureKey> {
        return (from..to).map { dayOfMonth ->
            val date = LocalDate.of(2020, 7, dayOfMonth)
            NHSTemporaryExposureKey(
                key = UUID.randomUUID().toString(),
                rollingStartNumber = date.toRollingStartNumber()
            )
        }
    }

    private fun isolationWithoutIndexCase() = Isolation(
        isolationStart = Instant.parse("2020-07-21T12:00:00Z"),
        indexCase = null,
        isolationConfiguration = DurationDays()
    )

    private fun setOnsetDate(symptomsOnsetDate: LocalDate) {
        val isolation = Isolation(
            isolationStart = Instant.parse("2020-07-21T12:00:00Z"),
            indexCase = IndexCase(
                symptomsOnsetDate,
                expiryDate = LocalDate.parse("2020-07-27"),
                selfAssessment = true
            ),
            isolationConfiguration = DurationDays()
        )
        every { stateMachine.readState(any()) } returns isolation
    }
}

private fun LocalDate.toRollingStartNumber(): Int {
    val secondsSince1970 = atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli() / 1000
    return (secondsSince1970 / (60 * 10)).toInt()
}
