package uk.nhs.nhsx.covid19.android.app.util

fun <T> List<T>.shallowCopy(): List<T> = mutableListOf<T>().apply {
    addAll(this@shallowCopy)
}
