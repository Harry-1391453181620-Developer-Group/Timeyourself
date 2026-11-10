package com.timeyourself.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Immutable snapshot of the statistics reported for one calendar period.
 *
 * The record combines the raw counters produced by the statistics queries with the
 * window bounds and the derived rates calculated by the API layer. Percentage and
 * average values are already rounded to two decimal places before they are stored here,
 * so clients can render them without further formatting.
 *
 * @param period reporting period identifier in upper case: DAILY, WEEKLY, MONTHLY or YEARLY
 * @param anchor reference date the period was resolved from
 * @param from inclusive start of the reporting window
 * @param to exclusive end of the reporting window
 * @param tasksDone number of tasks completed inside the window
 * @param eventsPast number of events that ended inside the window and are already over
 * @param tasksFailed number of open tasks whose due date fell inside the window and has passed
 * @param tasksNotDone number of open tasks scheduled inside the window that are not yet due
 * @param completionRate share of completed tasks among done, failed and not-done tasks, from 0 to 100
 * @param meanTaskHours mean number of hours between start and completion of the completed tasks
 * @param averageTasksPerUnit mean number of completed tasks per time unit of the window
 * @param unit time unit the average refers to: hour for daily reports and day otherwise
 */
public record ReportDto(
        String period,
        LocalDate anchor,
        LocalDateTime from,
        LocalDateTime to,
        long tasksDone,
        long eventsPast,
        long tasksFailed,
        long tasksNotDone,
        double completionRate,
        double meanTaskHours,
        double averageTasksPerUnit,
        String unit
) {
}
