package uk.nhs.nhsx.covid19.android.app.localdata

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.ContextCompat
import uk.nhs.nhsx.covid19.android.app.R
import uk.nhs.nhsx.covid19.android.app.appComponent
import uk.nhs.nhsx.covid19.android.app.common.BaseActivity
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.ENGLAND
import uk.nhs.nhsx.covid19.android.app.common.postcode.PostCodeDistrict.WALES
import uk.nhs.nhsx.covid19.android.app.databinding.ActivityLocalDataAndStatisticsBinding
import uk.nhs.nhsx.covid19.android.app.localstats.LocalStats
import uk.nhs.nhsx.covid19.android.app.remote.data.Direction.DOWN
import uk.nhs.nhsx.covid19.android.app.remote.data.Direction.SAME
import uk.nhs.nhsx.covid19.android.app.remote.data.Direction.UP
import uk.nhs.nhsx.covid19.android.app.util.getResourcesLocale
import uk.nhs.nhsx.covid19.android.app.util.toLocalDate
import uk.nhs.nhsx.covid19.android.app.util.uiFullFormat
import uk.nhs.nhsx.covid19.android.app.util.uiLongFormat
import uk.nhs.nhsx.covid19.android.app.util.viewutils.gone
import uk.nhs.nhsx.covid19.android.app.util.viewutils.setNavigateUpToolbar
import java.math.BigDecimal
import java.math.RoundingMode.HALF_EVEN
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.SHORT
import javax.inject.Inject

class LocalDataAndStatisticsActivity : BaseActivity() {
    private val binding by lazy {
        ActivityLocalDataAndStatisticsBinding.inflate(layoutInflater)
    }

    @Inject
    lateinit var clock: Clock

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        setContentView(binding.root)

        with(binding) {
            setNavigateUpToolbar(
                primaryToolbar.toolbar,
                R.string.local_statistics_main_screen_navigation_title,
                upIndicator = R.drawable.ic_arrow_back_primary
            )

            setupUI()
        }
    }

    private fun setupUI() {
        val data = intent.getParcelableExtra<LocalStats>(EXTRA_LOCAL_STATS)
            ?: throw IllegalStateException("Local stats data was not available from starting intent")

        with(binding) {
            populateLastUpdatedInHeader(data)
            localAreaDataAndStatisticsLocalAuthorityTitle.text = data.localAuthorityName
            populatePeopleTestedPositiveUpdateDate(data)
            populateDailyCases(data)

            populateLast7DaysCases(data)
            populateCasesPer100kUpdateDate(data)

            populateLocalCasesPer100k(data)
            populateCountriesPer100k(data)

            populateFootnote(data)
        }
    }

    private fun populateFootnote(data: LocalStats) {
        val startDate = data.localAuthorityNewCasesByPublishDateLastUpdate.minusDays(13)
        val endDate = data.localAuthorityNewCasesByPublishDateLastUpdate.minusDays(7)
        findViewById<TextView>(R.id.localAreaDataAndStatisticsChangeFromSevenDays).text =
            getString(
                R.string.local_statistics_main_screen_about_data_footnote_1,
                startDate.uiLongFormat(this),
                endDate.uiLongFormat(this)
            )
    }

    private fun populateCountriesPer100k(data: LocalStats) = with(binding) {

        val postCodeDistrict = data.postCodeDistrict
        localAreaDataAndStatisticsCountry.text = when (postCodeDistrict) {
            ENGLAND -> getString(R.string.local_statistics_main_screen_england_average)
            WALES -> getString(R.string.local_statistics_main_screen_wales_average)
            else -> getString(R.string.local_statistics_main_screen_data_missing)
        }

        if (data.countryNewCasesBySpecimenDateRollingRate == null && (postCodeDistrict == ENGLAND || postCodeDistrict == WALES)) {
            when (postCodeDistrict) {
                ENGLAND ->
                    localAreaDataAndStatisticsCountryRollingRate.contentDescription =
                        getString(R.string.local_statistics_main_screen_england_rate_100k_not_available_accessibility_text)
                WALES ->
                    localAreaDataAndStatisticsCountryRollingRate.contentDescription =
                        getString(R.string.local_statistics_main_screen_wales_rate_100k_not_available_accessibility_text)
            }
        } else {
            val countryNewCasesBySpecimenDateRollingRateValue = data.countryNewCasesBySpecimenDateRollingRate.toString()
            when (postCodeDistrict) {
                ENGLAND ->
                    localAreaDataAndStatisticsCountryRollingRate.contentDescription =
                        getString(
                            R.string.local_statistics_main_screen_england_rate_100k_accessibility_text,
                            countryNewCasesBySpecimenDateRollingRateValue
                        )
                WALES ->
                    localAreaDataAndStatisticsCountryRollingRate.contentDescription =
                        getString(
                            R.string.local_statistics_main_screen_wales_rate_100k_accessibility_text,
                            countryNewCasesBySpecimenDateRollingRateValue
                        )
            }
        }

        localAreaDataAndStatisticsCountryRate.text = formatNumber(data.countryNewCasesBySpecimenDateRollingRate)
    }

    private fun populateLocalCasesPer100k(data: LocalStats) = with(binding) {
        val newCasesBySpecimenDateRollingRate = data.localAuthorityData.newCasesBySpecimenDateRollingRate
        localAreaDataAndStatisticsLocalAuthority.text = data.localAuthorityName
        localAreaDataAndStatisticsLocalAuthorityRate.text = formatNumber(newCasesBySpecimenDateRollingRate)

        localAreaDataAndStatisticsLocalAuthorityRollingRate.contentDescription =
            if (newCasesBySpecimenDateRollingRate == null) {
                getString(
                    R.string.local_statistics_main_screen_local_authority_rate_100k_not_available_accessibility_text,
                    data.localAuthorityName
                )
            } else {
                getString(
                    R.string.local_statistics_main_screen_local_authority_rate_100k_accessibility_text,
                    data.localAuthorityName,
                    newCasesBySpecimenDateRollingRate.toString()
                )
            }
    }

    private fun populateDailyCases(data: LocalStats) = with(binding) {
        val dailyCases = data.localAuthorityData.newCasesByPublishDate
        localAreaDataAndStatisticsDailyCount.text = formatNumber(dailyCases)
        localAreaDataAndStatisticsDailyCasesContainer.contentDescription = if (dailyCases == null) {
            getString(
                R.string.local_statistics_main_screen_daily_cases_not_available_accessibility_text,
                data.localAuthorityName
            )
        } else {
            resources.getQuantityString(
                R.plurals.local_statistics_main_screen_daily_accessibility_text,
                dailyCases,
                dailyCases,
                data.localAuthorityName
            )
        }
    }

    private fun populateCasesPer100kUpdateDate(data: LocalStats) {
        binding.localAreaDataAndStatisticsRollingRateDate.text = getString(
            R.string.local_statistics_main_screen_rolling_rate_last_updated,
            data.localAuthorityNewCasesBySpecimenDateRollingRateLastUpdate.uiLongFormat(this@LocalDataAndStatisticsActivity)
        )
    }

    private fun populateLast7DaysCases(data: LocalStats) = with(binding) {

        val newCasesByPublishDateRollingSum = data.localAuthorityData.newCasesByPublishDateRollingSum
        val newCasesByPublishDateDirection = data.localAuthorityData.newCasesByPublishDateDirection
        val newCasesByPublishDateChange = data.localAuthorityData.newCasesByPublishDateChange
        val newCasesByPublishDateChangePercentage = data.localAuthorityData.newCasesByPublishDateChangePercentage

        localAreaDataAndStatisticsLastSevenDaysCount.text = formatNumber(newCasesByPublishDateRollingSum)
        if (newCasesByPublishDateRollingSum != null && newCasesByPublishDateDirection != null && newCasesByPublishDateChange != null && newCasesByPublishDateChangePercentage != null) {
            // accessibility text
            val firstPart = resources.getQuantityString(
                R.plurals.local_statistics_main_screen_last_7_days_accessibility_text,
                newCasesByPublishDateRollingSum,
                newCasesByPublishDateRollingSum
            )
            val secondPartResId = when (newCasesByPublishDateDirection) {
                DOWN -> R.string.local_statistics_main_screen_last_seven_days_rate_down_accessibility_text
                UP -> R.string.local_statistics_main_screen_last_seven_days_rate_up_accessibility_text
                SAME -> R.string.local_statistics_main_screen_last_seven_days_rate_no_change_accessibility_text
            }
            val secondPart = getString(
                secondPartResId,
                newCasesByPublishDateChange,
                newCasesByPublishDateChangePercentage
            )
            localAreaDataAndStatisticsLastSevenDaysContainer.contentDescription = "$firstPart. $secondPart"

            // regular content
            val context = this@LocalDataAndStatisticsActivity
            val (img, stringRes) = when (newCasesByPublishDateDirection) {
                DOWN -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_green_down_arrow
                ) to R.string.local_statistics_main_screen_last_seven_days_rate_down
                UP -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_red_up_arrow
                ) to R.string.local_statistics_main_screen_last_seven_days_rate_up
                SAME -> ContextCompat.getDrawable(
                    context,
                    R.drawable.ic_gray_same
                ) to R.string.local_statistics_main_screen_last_seven_days_rate_no_change
            }
            localAreaDataAndStatisticsLastSevenDaysRate.setCompoundDrawablesRelativeWithIntrinsicBounds(
                img,
                null,
                null,
                null
            )

            localAreaDataAndStatisticsLastSevenDaysRate.text = getString(
                stringRes,
                newCasesByPublishDateChange,
                newCasesByPublishDateChangePercentage
            )
        } else {
            localAreaDataAndStatisticsLastSevenDaysRate.gone()
            localAreaDataAndStatisticsLastSevenDaysContainer.contentDescription = getString(
                R.string.local_statistics_main_screen_last_7_days_not_available_accessibility_text
            )
        }
    }

    private fun formatNumber(number: Int?): String {
        if (number == null) {
            return getString(R.string.local_statistics_main_screen_data_missing)
        }
        return number.toString()
    }

    private fun formatNumber(number: BigDecimal?): String {
        if (number == null) {
            return getString(R.string.local_statistics_main_screen_data_missing)
        }
        return number.setScale(1, HALF_EVEN).toString()
    }

    private fun populatePeopleTestedPositiveUpdateDate(data: LocalStats) {
        binding.localAreaDataAndStatisticsLatestDataProvided.text = getString(
            R.string.local_statistics_main_screen_people_tested_positive_last_updated,
            data.localAuthorityNewCasesByPublishDateLastUpdate.uiLongFormat(this@LocalDataAndStatisticsActivity)
        )
    }

    private fun populateLastUpdatedInHeader(data: LocalStats) {
        binding.localAreaDataAndStatisticsViewLatestLastUpdated.text = getString(
            R.string.local_statistics_main_screen_last_updated,
            data.lastFetch.toLocalDate(clock.zone).uiFullFormat(this),
            DateTimeFormatter.ofLocalizedTime(SHORT).withLocale(getResourcesLocale())
                .withZone(clock.zone)
                .format(data.lastFetch)
        )
    }

    companion object {
        private const val EXTRA_LOCAL_STATS = "EXTRA_LOCAL_STATS"

        fun getIntent(context: Context, localStats: LocalStats) =
            Intent(context, LocalDataAndStatisticsActivity::class.java)
                .putExtra(EXTRA_LOCAL_STATS, localStats)
    }
}
