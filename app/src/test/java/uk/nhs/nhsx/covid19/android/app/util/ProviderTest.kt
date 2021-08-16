package uk.nhs.nhsx.covid19.android.app.util

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import com.squareup.moshi.Moshi
import io.mockk.Called
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.api.condition.DisabledIf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.covid19.android.app.di.module.NetworkModule
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.BIDIRECTIONAL
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.OBJECT_TO_JSON
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KProperty1

@TestInstance(Lifecycle.PER_CLASS)
abstract class ProviderTest<P : Provider, T> {
    protected val sharedPreferences = mockk<SharedPreferences>(relaxUnitFun = true)
    private val sharedPreferencesEditor = mockk<Editor>(relaxed = true)
    protected val moshi: Moshi = NetworkModule.moshi
    abstract val getTestSubject: (Moshi, SharedPreferences) -> P

    protected val testSubject by lazy { getTestSubject(moshi, sharedPreferences) }
    abstract val property: KProperty1<P, T>
    abstract val key: String
    abstract val defaultValue: T
    abstract val expectations: List<ProviderTestExpectation<T>>

    private fun serializationExpectations() = expectations.filter { it.isSerialization() }
    private fun deserializationExpectations() = expectations.filter { it.isDeserialization() }
    private fun serializationExpectationsEmpty() = serializationExpectations().isEmpty()
    private fun deserializationExpectationsEmpty() = deserializationExpectations().isEmpty()

    @BeforeEach
    fun setUp() {
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
        sharedPreferencesReturns(null)
    }

    @AfterEach
    internal fun tearDown() {
        // It seems mockk mocks don't get cleared in between executions of parameterized tests
        clearAllMocks()
    }

    protected fun sharedPreferencesReturns(json: String?) {
        every { sharedPreferences.all[key] } returns json
    }

    @Test
    fun `verify default when null`() {
        sharedPreferencesReturns(null)

        assertEquals(defaultValue, property.get(testSubject))
    }

    @Test
    fun `verify default when corrupt`() {
        sharedPreferencesReturns(CORRUPT_VALUE)

        assertEquals(defaultValue, property.get(testSubject))
    }

    @DisabledIf("deserializationExpectationsEmpty")
    @ParameterizedTest
    @MethodSource("deserializationExpectations")
    fun `verify deserialization of json`(expectation: ProviderTestExpectation<T>) {
        with(expectation) {
            sharedPreferencesReturns(json)

            assertEquals(objectValue, property.get(testSubject))
        }
    }

    @DisabledIf("serializationExpectationsEmpty")
    @ParameterizedTest
    @MethodSource("serializationExpectations")
    fun `verify serialization to json`(expectation: ProviderTestExpectation<T>) {
        with(expectation) {
            (property as KMutableProperty1<P, T>).set(testSubject, objectValue)

            assertSharedPreferenceSetsValue(json)
        }
    }

    protected fun assertSharedPreferenceSetsValue(json: String?) {
        if (json == null) {
            verify { sharedPreferencesEditor.remove(key) }
        } else {
            verify { sharedPreferencesEditor.putString(key, json) }
        }
    }

    protected fun verifySharedPreferencesEditorWasNotCalled() {
        verify { sharedPreferencesEditor wasNot Called }
    }

    companion object {
        private const val CORRUPT_VALUE = "CORRUPT_VALUE_dsfdsfsdfdsfdsf"
    }
}

data class ProviderTestExpectation<T>(
    val json: String?,
    val objectValue: T,
    val direction: ProviderTestExpectationDirection = BIDIRECTIONAL
) {
    fun isDeserialization(): Boolean = direction == JSON_TO_OBJECT || direction == BIDIRECTIONAL

    fun isSerialization(): Boolean = direction == OBJECT_TO_JSON || direction == BIDIRECTIONAL
}

enum class ProviderTestExpectationDirection {
    JSON_TO_OBJECT,
    OBJECT_TO_JSON,
    BIDIRECTIONAL
}
