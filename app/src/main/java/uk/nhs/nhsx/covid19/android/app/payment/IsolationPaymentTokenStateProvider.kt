package uk.nhs.nhsx.covid19.android.app.payment

import android.content.SharedPreferences
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateType.DISABLED
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateType.TOKEN
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateType.UNRESOLVED
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsolationPaymentTokenStateProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {

    private var storedTokenState: IsolationPaymentTokenStateJson? by storage(ISOLATION_PAYMENT_TOKEN)

    var tokenState: IsolationPaymentTokenState
        get() = storedTokenState?.toState() ?: Unresolved
        set(newState) {
            if (tokenState != newState) {
                storedTokenState = newState.toStateJson()
            }
        }

    companion object {
        const val ISOLATION_PAYMENT_TOKEN = "ISOLATION_PAYMENT_TOKEN"
    }
}

internal enum class IsolationPaymentTokenStateType {
    UNRESOLVED,
    DISABLED,
    TOKEN
}

@JsonClass(generateAdapter = true)
internal data class IsolationPaymentTokenStateJson(
    val type: IsolationPaymentTokenStateType,
    val token: String? = null
)

private fun IsolationPaymentTokenStateJson.toState(): IsolationPaymentTokenState =
    when (type) {
        UNRESOLVED -> Unresolved
        DISABLED -> Disabled
        TOKEN ->
            if (token != null) {
                Token(token)
            } else {
                Timber.e("Unexpected null token while deserializing state with type TOKEN: $this")
                Unresolved
            }
    }

private fun IsolationPaymentTokenState.toStateJson(): IsolationPaymentTokenStateJson =
    when (this) {
        Unresolved -> IsolationPaymentTokenStateJson(UNRESOLVED)
        Disabled -> IsolationPaymentTokenStateJson(DISABLED)
        is Token -> IsolationPaymentTokenStateJson(TOKEN, token)
    }
