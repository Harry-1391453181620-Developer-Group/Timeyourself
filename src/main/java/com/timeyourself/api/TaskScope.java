package com.timeyourself.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

public enum TaskScope {

    DAY, WEEK, MONTH, YEAR;

    public static TaskScope parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return DAY;
        }
        try {
            return TaskScope.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("scope must be day, week, month or year");
        }
    }

    public Range range(LocalDate anchor) {
        return switch (this) {
            case DAY -> new Range(anchor.atStartOfDay(), anchor.plusDays(1).atStartOfDay());
            case WEEK -> {
                LocalDate monday = anchor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                yield new Range(monday.atStartOfDay(), monday.plusDays(7).atStartOfDay());
            }
            case MONTH -> {
                LocalDate first = anchor.withDayOfMonth(1);
                yield new Range(first.atStartOfDay(), first.plusMonths(1).atStartOfDay());
            }
            case YEAR -> {
                LocalDate first = anchor.withDayOfYear(1);
                yield new Range(first.atStartOfDay(), first.plusYears(1).atStartOfDay());
            }
        };
    }

    public record Range(LocalDateTime from, LocalDateTime to) {
    }
}