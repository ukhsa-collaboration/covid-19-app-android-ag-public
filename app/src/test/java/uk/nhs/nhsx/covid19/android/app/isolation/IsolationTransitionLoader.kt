package uk.nhs.nhsx.covid19.android.app.isolation

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File

class IsolationTransitionLoader {
    private val moshi: Moshi = Moshi.Builder().build()

    fun loadTransitions(): List<Transition> {
        val classLoader = javaClass.classLoader!!
        val resource = classLoader.getResource("isolationRules.json")

        val json = File(resource.path).readText()
        val transitions = moshi.adapter(Transitions::class.java).fromJson(json)
        return transitions?.transitions ?: throw Exception("Could not read transitions from json.")
    }
}

@JsonClass(generateAdapter = true)
data class Transitions(
    val transitions: List<Transition>
)
