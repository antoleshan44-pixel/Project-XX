package com.urbano.common.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

public class DateUtils {

    public static final ZoneId UTC = ZoneId.of("UTC");
    public static final ZoneId NAIROBI = ZoneId.of("Africa/Nairobi");
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static OffsetDateTime nowUTC() {
        return OffsetDateTime.now(UTC);
    }

    public static OffsetDateTime toNairobi(OffsetDateTime utcTime) {
        if (utcTime == null) return null;
        return utcTime.atZoneSameInstant(NAIROBI).toOffsetDateTime();
    }

    public static OffsetDateTime toUTC(OffsetDateTime nairobiTime) {
        if (nairobiTime == null) return null;
        return nairobiTime.atZoneSameInstant(UTC).toOffsetDateTime();
    }

    public static String formatISO(OffsetDateTime date) {
        if (date == null) return null;
        return date.format(ISO_FORMATTER);
    }

    public static OffsetDateTime parseISO(String dateString) {
        if (dateString == null || dateString.isEmpty()) return null;
        return OffsetDateTime.parse(dateString, ISO_FORMATTER);
    }

    public static LocalDate toLocalDate(OffsetDateTime date) {
        if (date == null) return null;
        return date.toLocalDate();
    }

    public static boolean isAfterNow(OffsetDateTime date) {
        return date != null && date.isAfter(nowUTC());
    }

    public static boolean isBeforeNow(OffsetDateTime date) {
        return date != null && date.isBefore(nowUTC());
    }

    public static long daysBetween(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) return 0;
        return Duration.between(start, end).toDays();
    }
}