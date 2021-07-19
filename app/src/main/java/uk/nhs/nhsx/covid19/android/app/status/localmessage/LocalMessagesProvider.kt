package uk.nhs.nhsx.covid19.android.app.status.localmessage

import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import uk.nhs.nhsx.covid19.android.app.remote.data.LocalMessagesResponse
import uk.nhs.nhsx.covid19.android.app.util.Provider
import uk.nhs.nhsx.covid19.android.app.util.storage
import javax.inject.Inject

class LocalMessagesProvider @Inject constructor(
    override val moshi: Moshi,
    override val sharedPreferences: SharedPreferences
) : Provider {
    var localMessages: LocalMessagesResponse? by storage(VALUE_KEY)

    companion object {
        const val VALUE_KEY = "CONTENT_MODULE_KEY"
    }
}
