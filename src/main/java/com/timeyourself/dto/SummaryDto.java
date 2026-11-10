package com.timeyourself.dto;

/**
 * Immutable snapshot of the counters and the status sentence shown in the application footer.
 *
 * The record gathers the global task counters together with the names of the task and the
 * event that are currently in progress, which the interface displays as a short activity
 * summary. Missing activities are represented by null names rather than by empty strings.
 *
 * @param finishedTaskNumber number of tasks already completed
 * @param remainingTaskNumber number of tasks not completed yet and not deleted
 * @param ongoingTaskName name of the task currently in progress, or null when none qualifies
 * @param ongoingEventName name of the event currently in progress, or null when none qualifies
 * @param sentence motivational sentence selected for the current day
 */
public record SummaryDto(
        long finishedTaskNumber,
        long remainingTaskNumber,
        String ongoingTaskName,
        String ongoingEventName,
        String sentence
) {
}
