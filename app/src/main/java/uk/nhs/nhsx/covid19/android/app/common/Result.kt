package uk.nhs.nhsx.covid19.android.app.common

import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.common.Result.Failure
import uk.nhs.nhsx.covid19.android.app.common.Result.Success

sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val throwable: Throwable) : Result<Nothing>()

    fun <U> map(function: (T) -> U): Result<U> =
        when (this) {
            is Success -> Success(function(value))
            is Failure -> Failure(throwable)
        }
}

suspend fun <T> runSafely(function: suspend () -> T): Result<T> =
    runCatching { Success(function()) }
        .getOrElse {
            Timber.e(it)
            Failure(it)
        }
