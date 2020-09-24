package uk.nhs.nhsx.covid19.android.app.remote

import com.moczul.ok2curl.CurlInterceptor
import com.moczul.ok2curl.logger.Loggable
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

val additionalInterceptors = listOf(
    CurlInterceptor(
        Loggable { log ->
            Timber.tag("Ok2Curl").v(log)
        }
    ),
    HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
)
