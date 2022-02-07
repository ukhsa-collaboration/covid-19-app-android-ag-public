package uk.nhs.nhsx.covid19.android.app.remote.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Moshi.Builder
import org.junit.Test
import uk.nhs.nhsx.covid19.android.app.util.adapters.BigDecimalAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.InstantAdapter
import uk.nhs.nhsx.covid19.android.app.util.adapters.LocalDateAdapter

class LocalStatsResponseTest {

    @Test
    fun `can parse sample response`() {
        val moshi = provideMoshi()
        val adapter = moshi.adapter(LocalStatsResponse::class.java)
        val result = adapter.fromJson(fixture)
        println(result)
    }

    private fun provideMoshi(): Moshi {
        return Builder()
            .add(LocalDateAdapter())
            .add(InstantAdapter())
            .add(BigDecimalAdapter)
            .build()
    }

    companion object {
        const val fixture = """
{
    "lastFetch": "2021-11-15T21:59:00Z",
    "metadata": {
        "england": {
            "newCasesBySpecimenDateRollingRate": {
                "lastUpdate": "2021-11-13"
            }
        },
        "wales": {
            "newCasesBySpecimenDateRollingRate": {
                "lastUpdate": "2021-11-13"
            }
        },
        "lowerTierLocalAuthorities": {
            "newCasesByPublishDate": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesByPublishDateChangePercentage": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesByPublishDateChange": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesByPublishDateRollingSum": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesByPublishDateDirection": {
                "lastUpdate": "2021-11-18"
            },
            "newCasesBySpecimenDateRollingRate": {
                "lastUpdate": "2021-11-13"
            }
        }
    },
    "england": {
        "newCasesBySpecimenDateRollingRate": 510.8
    },
    "wales": {
        "newCasesBySpecimenDateRollingRate": null
    },
    "lowerTierLocalAuthorities": {
        "E06000037": {
            "name": "West Berkshire",
            "newCasesByPublishDateRollingSum": -771,
            "newCasesByPublishDateChange": 207,
            "newCasesByPublishDateDirection": "UP",
            "newCasesByPublishDate": 105,
            "newCasesByPublishDateChangePercentage": 36.7,
            "newCasesBySpecimenDateRollingRate": 289.5
        },
        "E08000035": {
            "name": "Leeds",
            "newCasesByPublishDateRollingSum": null,
            "newCasesByPublishDateChange": null,
            "newCasesByPublishDateDirection": null,
            "newCasesByPublishDate": null,
            "newCasesByPublishDateChangePercentage": null,
            "newCasesBySpecimenDateRollingRate": null
        }
    }
}"""
    }
}
