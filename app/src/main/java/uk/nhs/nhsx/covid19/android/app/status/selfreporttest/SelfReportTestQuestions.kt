package uk.nhs.nhsx.covid19.android.app.status.selfreporttest

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import uk.nhs.nhsx.covid19.android.app.remote.data.NHSTemporaryExposureKey
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestKitType
import uk.nhs.nhsx.covid19.android.app.remote.data.VirologyTestResult
import java.time.LocalDate

@Parcelize
data class SelfReportTestQuestions(
    val testType: VirologyTestResult?,
    val temporaryExposureKeys: List<NHSTemporaryExposureKey>?,
    val testKitType: VirologyTestKitType?,
    val isNHSTest: Boolean?,
    val testEndDate: ChosenDate?,
    val hadSymptoms: Boolean?,
    val symptomsOnsetDate: ChosenDate?,
    val hasReportedResult: Boolean?
) : Parcelable

@Parcelize
data class ChosenDate(val rememberedDate: Boolean, val date: LocalDate) : Parcelable
