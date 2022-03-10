package uk.nhs.nhsx.covid19.android.app.isolation

import uk.nhs.nhsx.covid19.android.app.state.IsolationConfiguration

interface StateRepresentation {
    val representationName: String
    fun setupState(isolationTestContext: IsolationTestContext, isolationConfiguration: IsolationConfiguration)
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
