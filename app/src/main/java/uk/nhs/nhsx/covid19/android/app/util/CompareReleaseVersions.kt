package uk.nhs.nhsx.covid19.android.app.util

import javax.inject.Inject
import kotlin.math.max

class CompareReleaseVersions @Inject constructor() {
    /**
     * Compares two version numbers. Version numbers should be in dot-separated format (e.g. 1.2.34)
     * @param version1 first version number
     * @param version2 second version number
     * @return -1 - if version1 < version2, 0 - if version1 == version2, 1 - if version1 > version2
     */
    operator fun invoke(version1: String, version2: String): Int {
        val version1Splits = version1.split(".")
        val version2Splits = version2.split(".")
        val maxLengthOfVersionSplits = max(version1Splits.size, version2Splits.size)
        for (i in 0 until maxLengthOfVersionSplits) {
            val v1 = if (i < version1Splits.size) version1Splits[i].toIntOrNull() ?: 0 else 0
            val v2 = if (i < version2Splits.size) version2Splits[i].toIntOrNull() ?: 0 else 0
            val compare = v1.compareTo(v2)
            if (compare != 0) {
                return compare
            }
        }
        return 0
    }
}
