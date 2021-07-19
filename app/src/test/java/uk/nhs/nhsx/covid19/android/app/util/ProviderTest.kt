package uk.nhs.nhsx.covid19.android.app.util

import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.BIDIRECTIONAL
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.JSON_TO_OBJECT
import uk.nhs.nhsx.covid19.android.app.util.ProviderTestExpectationDirection.OBJECT_TO_JSON
import kotlin.reflect.KMutableProperty1

@TestInstance(Lifecycle.PER_CLASS)
abstract class ProviderTest<P : Provider, T> {
    protected val sharedPreferences = mockk<SharedPreferences>(relaxUnitFun = true)
    protected val sharedPreferencesEditor = mockk<Editor>(relaxed = true)

    abstract val getTestSubject: (SharedPreferences) -> P

    private val testSubject by lazy { getTestSubject(sharedPreferences) }
    abstract val property: KMutableProperty1<P, T>
    abstract val key: String
    abstract val defaultValue: T
    abstract val expectations: List<ProviderTestExpectation<T>>

    fun serializationExpectations() = expectations.filter { it.isSerialization() }
    fun deserializationExpectations() = expectations.filter { it.isDeserialization() }

    @BeforeEach
    fun setUp() {
        every { sharedPreferences.edit() } returns sharedPreferencesEditor
    }

    @Test
    fun `verify default`() {
        every { sharedPreferences.all[key] } returns null

        assertEquals(defaultValue, property.get(testSubject))
    }

    @ParameterizedTest
    @MethodSource("deserializationExpectations")
    fun `verify deserialization of json`(expectation: ProviderTestExpectation<T>) {
        with(expectation) {
            every { sharedPreferences.all[key] } returns json

            assertEquals(objectValue, property.get(testSubject))
        }
    }

    @ParameterizedTest
    @MethodSource("serializationExpectations")
    fun `verify serialization to json`(expectation: ProviderTestExpectation<T>) {
        with(expectation) {
            property.set(testSubject, objectValue)

            if (objectValue == null && json == null) {
                verify { sharedPreferencesEditor.remove(key) }
            } else {
                verify { sharedPreferencesEditor.putString(key, json) }
            }
        }
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
