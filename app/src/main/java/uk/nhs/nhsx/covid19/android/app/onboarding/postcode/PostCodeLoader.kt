package uk.nhs.nhsx.covid19.android.app.onboarding.postcode

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class PostCodeLoader @Inject constructor(
    private val context: Context,
    private val moshi: Moshi
) {
    suspend fun readListFromJson(): List<String>? = withContext(Dispatchers.IO) {
        val type = Types.newParameterizedType(
            List::class.java,
            String::class.java
        )

        runCatching {
            val json = context.assets.open("postalDistricts.json").bufferedReader().use {
                it.readText()
            }

            moshi.adapter<List<String>>(type).fromJson(json).orEmpty()
        }.getOrNull()
    }
}
