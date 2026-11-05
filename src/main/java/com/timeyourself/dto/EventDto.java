package com.timeyourself.dto;

import java.time.LocalDateTime;

public record EventDto(
        long id,
        String name,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean completed,
        String state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}