package com.timeyourself.dto;

import java.time.LocalDateTime;

/**
 * Immutable read model describing one task together with the names of its category and plan.
 *
 * Instances are produced by the persistence layer from the v_task view joined with the
 * category and plan tables, so the status is derived by the database at read time from the
 * completion flag and the due date. Because the record is immutable, an instance reflects
 * the row as it was when the query ran.
 *
 * @param id database identifier of the task
 * @param name display name of the task
 * @param description free-form description of the task, empty when none was supplied
 * @param startAt timestamp at which work on the task is planned to start, possibly null
 * @param dueAt deadline of the task, null when the task has no deadline
 * @param completed whether the task is marked as completed
 * @param completedAt timestamp at which the task was completed, null while it is open
 * @param status lifecycle status derived by the view: COMPLETED, FAILED or ONGOING
 * @param categoryId identifier of the linked category, null when the task is not categorised
 * @param categoryName name of the linked category, null when no category is linked
 * @param planId identifier of the linked plan, null when the task belongs to no plan
 * @param planName name of the linked plan, null when no plan is linked
 * @param createdAt timestamp at which the task row was inserted
 * @param updatedAt timestamp at which the task row was last modified
 */
public record TaskDto(
        long id,
        String name,
        String description,
        LocalDateTime startAt,
        LocalDateTime dueAt,
        boolean completed,
        LocalDateTime completedAt,
        String status,
        Long categoryId,
        String categoryName,
        Long planId,
        String planName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
