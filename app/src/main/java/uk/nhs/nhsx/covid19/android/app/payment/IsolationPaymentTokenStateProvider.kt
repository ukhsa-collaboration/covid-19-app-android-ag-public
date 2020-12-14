package uk.nhs.nhsx.covid19.android.app.payment

import android.content.SharedPreferences
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Disabled
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Token
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenState.Unresolved
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateType.DISABLED
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateType.TOKEN
import uk.nhs.nhsx.covid19.android.app.payment.IsolationPaymentTokenStateType.UNRESOLVED
import uk.nhs.nhsx.covid19.android.app.util.SharedPrefsDelegate.Companion.with
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IsolationPaymentTokenStateProvider @Inject constructor(
    private val isolationPaymentTokenStateStorage: IsolationPaymentTokenStateStorage,
    moshi: Moshi
) {

    private val tokenStateListeners: MutableSet<(IsolationPaymentTokenState) -> Unit> = HashSet()

    private val serializationAdapter: JsonAdapter<IsolationPaymentTokenStateJson> =
        moshi.adapter(IsolationPaymentTokenStateJson::class.java)

    var tokenState: IsolationPaymentTokenState
        get() =
            isolationPaymentTokenStateStorage.value?.let {
                runCatching {
                    serializationAdapter.fromJson(it)?.toState()
                }
                    .getOrElse {
                        Timber.e(it)
                        Unresolved
                    }
            } ?: Unresolved

        set(newState) {
            val json = serializationAdapter.toJson(newState.toStateJson())
            val previousJson = isolationPaymentTokenStateStorage.value

            isolationPaymentTokenStateStorage.value = json

            if (json != previousJson) {
                notifyTokenStateListeners(newState)
            }
        }

    fun addTokenStateListener(listener: (IsolationPaymentTokenState) -> Unit) {
        tokenStateListeners.add(listener)
    }

    fun removeTokenStateListener(listener: (IsolationPaymentTokenState) -> Unit) {
        tokenStateListeners.remove(listener)
    }

    private fun notifyTokenStateListeners(newState: IsolationPaymentTokenState) {
        tokenStateListeners.forEach { listener -> listener(newState) }
    }
}

class IsolationPaymentTokenStateStorage @Inject constructor(sharedPreferences: SharedPreferences) {
    private val prefs = sharedPreferences.with<String>(VALUE_KEY)

    var value by prefs

    companion object {
        private const val VALUE_KEY = "ISOLATION_PAYMENT_TOKEN"
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
