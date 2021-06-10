package uk.nhs.nhsx.covid19.android.app.remote

import okhttp3.logging.HttpLoggingInterceptor

val additionalInterceptors = listOf(
    HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
)
