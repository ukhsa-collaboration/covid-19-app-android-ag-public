package uk.nhs.nhsx.covid19.android.app.isolation

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi

class IsolationTransitionLoader constructor(
    private val context: Context
) {
    private val moshi: Moshi = Moshi.Builder().build()

    fun loadTransitions(): List<Transition> {
        val json = context.assets.open("isolationRules.json").bufferedReader().use {
            it.readText()
        }

        val transitions = moshi.adapter(Transitions::class.java).fromJson(json)
        return transitions?.transitions ?: throw Exception("Could not read transitions from json.")
    }
}

@JsonClass(generateAdapter = true)
data class Transitions(
    val transitions: List<Transition>
)
