package uk.nhs.nhsx.covid19.android.app.common.postcode

import android.content.Context
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalAuthorityPostCodesLoader @Inject constructor(
    private val context: Context,
    private val moshi: Moshi
) {

    private var localAuthorityPostCodesMap: LocalAuthorityPostCodes? = null

    private val mutex = Mutex()

    suspend fun load(): LocalAuthorityPostCodes? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (localAuthorityPostCodesMap != null) {
                    return@withContext localAuthorityPostCodesMap
                }

                runCatching {
                    val json = context.assets.open("localAuthorities.json").bufferedReader().use {
                        it.readText()
                    }
                    localAuthorityPostCodesMap =
                        moshi.adapter<LocalAuthorityPostCodes>(LocalAuthorityPostCodes::class.java).fromJson(json)
                    return@withContext localAuthorityPostCodesMap
                }.getOrNull()
            }
        }
}

fun LocalAuthority.supported(): Boolean =
    PostCodeDistrict.fromString(this.country)?.supported ?: false

@JsonClass(generateAdapter = true)
data class LocalAuthorityPostCodes(
    val postcodes: Map<String, List<String>>,
    val localAuthorities: Map<String, LocalAuthority>
)

@JsonClass(generateAdapter = true)
data class LocalAuthority(
    val name: String,
    val country: String
)
