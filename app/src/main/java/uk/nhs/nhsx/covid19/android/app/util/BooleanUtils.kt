package uk.nhs.nhsx.covid19.android.app.util

fun Boolean?.defaultFalse() =
    this ?: false

fun Boolean?.defaultTrue() =
    this ?: true

fun Boolean.toInt() = if (this) 1 else 0
