package uk.nhs.nhsx.covid19.android.app.isolation

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import java.io.File

class IsolationRuleLoader {
    private val moshi: Moshi = Moshi.Builder().build()

    fun loadIsolationRules(): IsolationRules {
        val classLoader = javaClass.classLoader!!
        val resource = classLoader.getResource("isolationRules.json")

        val json = File(resource.path).readText()
        val transitions = moshi.adapter(IsolationRules::class.java).fromJson(json)
        return transitions ?: throw Exception("Could not read transitions from json.")
    }
}

@JsonClass(generateAdapter = true)
data class Source(
    val commit: String,
    val referenceBasePath: String
)

@JsonClass(generateAdapter = true)
data class IsolationRules(
    val source: Source,
    val transitions: List<Transition>
)
