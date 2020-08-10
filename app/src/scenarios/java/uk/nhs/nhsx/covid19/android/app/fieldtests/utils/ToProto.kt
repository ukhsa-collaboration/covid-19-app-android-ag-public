package uk.nhs.nhsx.covid19.android.app.fieldtests.utils

import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.protobuf.ByteString

typealias TemporaryExposureKeyDto = uk.nhs.nhsx.covid19.android.app.fieldtests.proto.TemporaryExposureKey

fun List<TemporaryExposureKey>.toProto(): List<TemporaryExposureKeyDto> {
    return map { exposureKey ->
        TemporaryExposureKeyDto.newBuilder()
            .setKeyData(ByteString.copyFrom(exposureKey.keyData))
            .setRollingStartIntervalNumber(exposureKey.rollingStartIntervalNumber)
            .setRollingPeriod(144)
            .setTransmissionRiskLevel(exposureKey.transmissionRiskLevel)
            .build()
    }
}
