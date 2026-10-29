package com.medassist.common.time;

import com.medassist.common.config.AppProperties;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Business dates ("today", fiscal year, month) are Indian dates, while instants in the
 * database are UTC. Mixing the two is the classic bug where a sale at 00:30 IST lands on
 * yesterday's report; every date decision goes through this class instead.
 */
@Component
public class BusinessTime {

    private final Clock clock;
    private final ZoneId zone;

    public BusinessTime(Clock clock, AppProperties app) {
        this.clock = clock;
        this.zone = app.businessZone();
    }

    public LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }

    public Instant now() {
        return clock.instant();
    }

    public ZoneId zone() {
        return zone;
    }

    public Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(zone).toInstant();
    }

    public LocalDate dateOf(Instant instant) {
        return ZonedDateTime.ofInstant(instant, zone).toLocalDate();
    }

    /** Indian fiscal year label: April 2026 - March 2027 is "26-27". */
    public String fiscalYear(LocalDate date) {
        int start = date.getMonthValue() >= 4 ? date.getYear() : date.getYear() - 1;
        return String.format("%02d-%02d", start % 100, (start + 1) % 100);
    }
}
