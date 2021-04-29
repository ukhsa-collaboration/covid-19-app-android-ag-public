package uk.nhs.nhsx.covid19.android.app.remote

class MockEmptyApi : EmptyApi {
    override suspend fun submit() {}
}
