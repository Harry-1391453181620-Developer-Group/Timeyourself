package com.timeyourself.api;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;

/**
 * Converts a day, week, month or year scope plus an anchor date into a half-open date range.
 *
 * The resulting range is used by the task, event and report endpoints to select records that overlap the
 * chosen calendar period, with the start instant inclusive and the end instant exclusive.
 */
public enum TaskScope {

    /**
     * A single calendar day, from the start of the anchor day to the start of the following day.
     */
    DAY,

    /**
     * The week containing the anchor date, from Monday up to but excluding the following Monday.
     */
    WEEK,

    /**
     * The calendar month containing the anchor date, from its first day up to but excluding the first day of
     * the next month.
     */
    MONTH,

    /**
     * The calendar year containing the anchor date, from its first day up to but excluding the first day of
     * the next year.
     */
    YEAR;

    /**
     * Parses a scope label, falling back to {@link #DAY} when no label is supplied.
     *
     * The lookup is case-insensitive and ignores surrounding whitespace.
     *
     * @param raw scope label such as {@code day} or {@code WEEK}, possibly {@code null} or blank
     * @return the matching scope, or {@link #DAY} when the input is absent
     * @throws IllegalArgumentException when the label does not name a supported scope
     */
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

    /**
     * Computes the half-open range covered by this scope around the given anchor date.
     *
     * @param anchor date that the scope is resolved against; never {@code null}
     * @return the range from the inclusive start instant to the exclusive end instant
     */
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

    /**
     * Half-open instant range produced by {@link #range(LocalDate)}.
     *
     * @param from inclusive start instant of the range
     * @param to exclusive end instant of the range
     */
    public record Range(LocalDateTime from, LocalDateTime to) {
    }
}
