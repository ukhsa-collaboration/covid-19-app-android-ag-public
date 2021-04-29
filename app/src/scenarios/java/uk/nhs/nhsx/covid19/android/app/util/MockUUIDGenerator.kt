package uk.nhs.nhsx.covid19.android.app.util

import java.util.UUID

class MockUUIDGenerator : UUIDGenerator {

    var nextUUID = UUID.fromString("bf66c57e-5ae0-448e-94ec-52ae6947ac79")

    override fun randomUUID(): UUID =
        nextUUID
}
