/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package uk.nhs.nhsx.covid19.android.app.fieldtests.nearby

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.Exposure
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.ExposureEvent
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.Summary
import uk.nhs.nhsx.covid19.android.app.fieldtests.utils.ExposureListener
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Broadcast receiver for callbacks from exposure notification API.
 */
class ExposureNotificationBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val exposureNotificationClient = Nearby.getExposureNotificationClient(context)

        val action = intent.action
        Timber.d("onReceive: action = $action")
        if (ExposureNotificationClient.ACTION_EXPOSURE_STATE_UPDATED == action) {
            val token = intent.getStringExtra(ExposureNotificationClient.EXTRA_TOKEN)!!
            Timber.d("onReceive: token = $token")
            GlobalScope.launch {
                val split = token.split("<|@|>")
                val deviceName = split[0]
                val exposureSummary = exposureNotificationClient.getExposureSummary(token).await()
                val exposureInformationList =
                    exposureNotificationClient.getExposureInformation(token).await()

                val exposureList = exposureInformationList.map {
                    Exposure(
                        it.attenuationDurationsInMinutes.map { it * 60 },
                        it.attenuationValue,
                        DateTimeFormatter.ISO_INSTANT.format(Instant.ofEpochMilli(it.dateMillisSinceEpoch)),
                        it.durationMinutes * 60,
                        it.totalRiskScore,
                        it.transmissionRiskLevel
                    )
                }

                val summary = Summary(
                    exposureSummary.attenuationDurationsInMinutes.map { it * 60 },
                    exposureSummary.daysSinceLastExposure,
                    exposureSummary.matchedKeyCount,
                    exposureSummary.maximumRiskScore,
                    exposureSummary.summationRiskScore
                )

                val event = ExposureEvent(
                    deviceName = deviceName,
                    summary = summary,
                    exposureInfos = exposureList
                )
                ExposureListener.onExposureResult(token = token, exposureEvent = event)
            }
        }
    }
}
