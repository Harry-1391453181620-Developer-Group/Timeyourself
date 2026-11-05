package com.timeyourself.dto;

public record SummaryDto(
        long finishedTaskNumber,
        long remainingTaskNumber,
        String ongoingTaskName,
        String ongoingEventName,
        String sentence
) {
}