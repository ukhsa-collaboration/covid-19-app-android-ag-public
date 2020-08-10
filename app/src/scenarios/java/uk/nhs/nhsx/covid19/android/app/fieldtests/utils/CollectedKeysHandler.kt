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

package uk.nhs.nhsx.covid19.android.app.fieldtests.utils

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import uk.nhs.nhsx.covid19.android.app.exposure.FieldTestConfiguration
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.ExperimentInfo
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.ExposureEvent
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.Summary
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.Random
import kotlin.coroutines.resume

class CollectedKeysHandler(private val context: Context) {

    private val exposureNotificationClient = Nearby.getExposureNotificationClient(context)

    private val emptyExposureEvent = ExposureEvent(
        "", exposureInfos = listOf(),
        summary = Summary(
            attenuationDurations = listOf(0, 0, 0),
            daysSinceLastExposure = 0,
            matchedKeyCount = 0,
            maximumRiskScore = 0,
            summationRiskScore = 0
        )
    )

    suspend fun handle(
        configurations: List<FieldTestConfiguration>,
        experimentInfo: ExperimentInfo
    ): Map<FieldTestConfiguration, List<ExposureEvent>> {

        val keyFileWriter = KeyFileWriter(context, null)
        try {
            val configurationToResults =
                hashMapOf<FieldTestConfiguration, MutableList<ExposureEvent>>()
            val exposureEvents = experimentInfo.participants.forEach { participant ->
                val keys = participant.temporaryTracingKeys
                    .map {
                        TemporaryExposureKeyEncodingHelper.decodeObject(it)
                    }

                val files = keyFileWriter.writeForKeys(
                    keys,
                    Instant.now().minus(Duration.ofDays(14)),
                    Instant.now(),
                    "UK"
                )

                configurations.forEach { configuration ->
                    val exposureConfiguration =
                        ConfigurationConverter.toExposureConfiguration(configuration)

                    val exposureEvent = withTimeoutOrNull(1_000L) {
                        val token = participant.deviceName + "<|@|>" + Random().nextInt()
                        Timber.d("start submitting exposure $token")
                        Timber.d("keys for that token: $keys")

                        provideDiagnosisKeys(files, exposureConfiguration, token)
                    } ?: emptyExposureEvent.copy(deviceName = participant.deviceName)

                    configurationToResults.getOrPut(configuration) { mutableListOf() }
                        .add(exposureEvent)
                }
            }
            Timber.d("exposureEvents: $exposureEvents")
            return configurationToResults
        } catch (e: Exception) {
            Timber.e(e, "Error decoding")
        }
        return emptyMap()
    }

    private suspend fun provideDiagnosisKeys(
        files: List<File>,
        configuration: ExposureConfiguration,
        token: String
    ): ExposureEvent {
        return suspendCancellableCoroutine { cont ->
            exposureNotificationClient.provideDiagnosisKeys(files, configuration, token)
            ExposureListener.addListener { t, exposureEvent ->
                if (token == t) {
                    cont.resume(exposureEvent)
                }
            }
        }
    }
}
