package uk.nhs.nhsx.covid19.android.app.util

import java.util.UUID

class AndroidUUIDGenerator : UUIDGenerator {

    override fun randomUUID(): UUID =
        UUID.randomUUID()
}
