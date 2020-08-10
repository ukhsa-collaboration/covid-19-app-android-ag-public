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

import android.util.Base64
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey.TemporaryExposureKeyBuilder
import uk.nhs.nhsx.covid19.android.app.fieldtests.network.TemporaryTracingKey

/**
 * Helper class for encoding and decoding TemporaryExposureKeys for interop testing.
 */
object TemporaryExposureKeyEncodingHelper {

    fun decodeObject(temporaryTracingKey: TemporaryTracingKey): TemporaryExposureKey {
        return TemporaryExposureKeyBuilder()
            .setKeyData(Base64.decode(temporaryTracingKey.key, Base64.DEFAULT))
            .setRollingStartIntervalNumber(temporaryTracingKey.intervalNumber)
            .setRollingPeriod(temporaryTracingKey.intervalCount)
            .setTransmissionRiskLevel(7)
            .build()
    }
}
