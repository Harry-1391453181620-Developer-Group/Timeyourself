package com.timeyourself.dto;

import java.time.LocalDateTime;

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