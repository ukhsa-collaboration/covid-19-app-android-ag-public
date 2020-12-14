package uk.nhs.nhsx.covid19.android.app.payment

sealed class IsolationPaymentTokenState {
    object Unresolved : IsolationPaymentTokenState()
    object Disabled : IsolationPaymentTokenState()
    data class Token(val token: String) : IsolationPaymentTokenState()
}
