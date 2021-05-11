package uk.nhs.nhsx.covid19.android.app.di

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

class ApplicationClock(offsetDays: Long) : Clock() {

    private var clock = systemDefaultZone()

    init {
        offsetDays(offsetDays)
    }

    override fun getZone(): ZoneId = clock.zone

    override fun withZone(zone: ZoneId?): Clock = clock

    override fun instant(): Instant = clock.instant()

    fun offsetDays(numberOfDays: Long) {
        this.clock = offset(systemDefaultZone(), Duration.ofDays(numberOfDays))
    }
}
