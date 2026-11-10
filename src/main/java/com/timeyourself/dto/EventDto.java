package com.timeyourself.dto;

import java.time.LocalDateTime;

/**
 * Immutable read model describing one calendar event exposed to the web layer.
 *
 * Instances are produced by the persistence layer from rows of the v_event view, so the
 * completion flag and the lifecycle state are derived by the database at read time
 * instead of being stored on the event table. Because the record is immutable, an
 * instance reflects the row as it was when the query ran.
 *
 * @param id database identifier of the event
 * @param name display name of the event
 * @param description free-form description of the event, empty when none was supplied
 * @param startAt scheduled start timestamp, null when the event has no defined start
 * @param endAt scheduled end timestamp, null when the event has no defined end
 * @param completed whether the event has already ended, as computed by the view
 * @param state lifecycle state derived by the view: NOT_STARTED, ONGOING or ENDED
 * @param createdAt timestamp at which the event row was inserted
 * @param updatedAt timestamp at which the event row was last modified
 */
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
