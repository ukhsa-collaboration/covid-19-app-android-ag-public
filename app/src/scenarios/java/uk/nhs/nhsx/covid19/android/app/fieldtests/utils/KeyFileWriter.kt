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
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.protobuf.ByteString
import uk.nhs.nhsx.covid19.android.app.fieldtests.proto.SignatureInfo
import uk.nhs.nhsx.covid19.android.app.fieldtests.proto.TEKSignature
import uk.nhs.nhsx.covid19.android.app.fieldtests.proto.TEKSignatureList
import uk.nhs.nhsx.covid19.android.app.fieldtests.proto.TemporaryExposureKeyExport
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.Instant
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KeyFileWriter internal constructor(
    private val context: Context,
    private val signer: KeyFileSigner? = KeyFileSigner.get()
) {

    @JvmOverloads
    fun writeForKeys(
        keys: List<TemporaryExposureKey>,
        start: Instant,
        end: Instant,
        regionIsoAlpha2: String,
        maxBatchSize: Int = DEFAULT_MAX_BATCH_SIZE
    ): List<File> {
        val outFiles: MutableList<File> = ArrayList()
        var batchNum = 1
        for (batch in keys.chunked(maxBatchSize)) {
            val outFile =
                File(context.filesDir, String.format(Locale.ENGLISH, FILENAME_PATTERN, batchNum))
            try {
                ZipOutputStream(FileOutputStream(outFile)).use { out ->
                    val signatureEntry = ZipEntry(SIG_FILENAME)
                    val exportEntry = ZipEntry(EXPORT_FILENAME)
                    val exportProto = export(batch, start, end, regionIsoAlpha2, batchNum)
                    val exportBytes: ByteArray = header().toByteArray() + exportProto.toByteArray()
                    val signature = sign(exportBytes, batch.size, batchNum)
                    out.putNextEntry(signatureEntry)
                    out.write(signature.toByteArray())
                    out.putNextEntry(exportEntry)
                    out.write(exportBytes)
                    outFiles.add(outFile)
                    batchNum++
                }
            } catch (e: IOException) {
                // TODO: better exception.
                throw RuntimeException(e)
            }
        }
        return outFiles
    }

    private fun export(
        keys: List<TemporaryExposureKey>,
        start: Instant,
        end: Instant,
        regionIsoAlpha2: String,
        batchNum: Int
    ): TemporaryExposureKeyExport {
        return TemporaryExposureKeyExport.newBuilder()
            .addAllKeys(keys.toProto())
            .addSignatureInfos(signatureInfo())
            .setStartTimestamp(start.toEpochMilli())
            .setEndTimestamp(end.toEpochMilli())
            .setRegion(regionIsoAlpha2)
            .setBatchSize(keys.size)
            .setBatchNum(batchNum)
            .build()
    }

    private fun sign(
        exportBytes: ByteArray,
        batchSize: Int,
        batchNum: Int
    ): TEKSignatureList {
        // In tests the signer is null because Robolectric doesn't support the crypto constructs we use.
        val signature = ByteString.copyFrom(
            signer?.sign(exportBytes) ?: "fake-signature".toByteArray()
        )
        return TEKSignatureList.newBuilder()
            .addSignatures(
                TEKSignature.newBuilder()
                    .setSignatureInfo(signatureInfo())
                    .setBatchNum(batchNum)
                    .setBatchSize(batchSize)
                    .setSignature(signature)
            )
            .build()
    }

    private fun signatureInfo(): SignatureInfo {
        return signer?.signatureInfo() ?: SignatureInfo.getDefaultInstance()
    }

    private fun header(): String {
        return HEADER_V1.padEnd(HEADER_LEN, ' ')
    }

    companion object {
        private const val FILENAME_PATTERN = "test-keyfile-%d.zip"

        const val SIG_FILENAME = "export.sig"
        const val EXPORT_FILENAME = "export.bin"
        private const val HEADER_V1 = "EK Export v1"
        private const val HEADER_LEN = 16
        private const val DEFAULT_MAX_BATCH_SIZE = 10000
    }
}
