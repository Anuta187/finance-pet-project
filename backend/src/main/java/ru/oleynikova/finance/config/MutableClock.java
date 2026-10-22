package ru.oleynikova.finance.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Часы для профиля test, их можно сдвигать через POST /api/test/clock. */
public class MutableClock extends Clock {

    private final Clock base;
    private volatile Duration offset = Duration.ZERO;

    public MutableClock(Clock base) {
        this.base = base;
    }

    public void shift(Duration delta) {
        offset = offset.plus(delta);
    }

    public void reset() {
        offset = Duration.ZERO;
    }

    public Duration offset() {
        return offset;
    }

    @Override
    public ZoneId getZone() {
        return base.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        MutableClock c = new MutableClock(base.withZone(zone));
        c.offset = offset;
        return c;
    }

    @Override
    public Instant instant() {
        return base.instant().plus(offset);
    }
}
