package com.timeyourself.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

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