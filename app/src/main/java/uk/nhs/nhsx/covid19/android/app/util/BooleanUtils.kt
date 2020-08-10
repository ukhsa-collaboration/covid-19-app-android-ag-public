package uk.nhs.nhsx.covid19.android.app.util

fun Boolean?.defaultFalse() =
    this ?: false

fun Boolean?.defaultTrue() =
    this ?: true
