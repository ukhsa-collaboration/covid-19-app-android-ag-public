package uk.nhs.nhsx.covid19.android.app.isolation

import uk.nhs.nhsx.covid19.android.app.remote.data.DurationDays
import uk.nhs.nhsx.covid19.android.app.testhelpers.TestApplicationContext

interface StateRepresentation {
    val representationName: String
    fun setupState(testAppContext: TestApplicationContext, isolationConfiguration: DurationDays)
    fun skipUnsupportedState(state: State)
    fun skipUnsupportedEvent(event: Event)
}

interface StateRepresentationProvider {
    fun getStateRepresentations(state: State, event: Event? = null): List<StateRepresentation>
}

class AllStateRepresentations : StateRepresentationProvider {
    private val stateRepresentationProviders = listOf(
        StateStorage4_10Provider()

        // add more providers here as the state representation changes over app versions
    )

    override fun getStateRepresentations(state: State, event: Event?): List<StateRepresentation> =
        stateRepresentationProviders.flatMap { it.getStateRepresentations(state, event) }
}
