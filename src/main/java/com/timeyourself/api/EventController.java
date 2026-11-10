package com.timeyourself.api;

import com.timeyourself.dto.EventDto;
import com.timeyourself.repository.EventRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

/**
 * REST controller exposing CRUD operations and calendar filtering for events under {@code /api/events}.
 *
 * Incoming payloads are normalised before they reach the repository: names are trimmed and required,
 * descriptions are trimmed and optional, and an end time may not precede a start time.
 */
@RestController
@RequestMapping("/api/events")
public class EventController {

    /**
     * Request body carrying the editable fields of an event.
     *
     * @param name event name; trimmed, required and limited to 100 characters
     * @param description optional event description; trimmed and limited to 1000 characters
     * @param startAt optional start time of the event
     * @param endAt optional end time of the event, which may not precede {@code startAt}
     */
    public record EventRequest(String name,
                               String description,
                               LocalDateTime startAt,
                               LocalDateTime endAt) {
    }

    /**
     * Repository used to read and mutate the stored events.
     */
    private final EventRepository repository;

    /**
     * Creates the controller with the event repository it delegates all persistence to.
     *
     * @param repository repository providing the event operations
     */
    public EventController(EventRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists events, either all of them or only those overlapping a calendar scope.
     *
     * Without a scope the complete event list is returned. With a scope the anchor defaults to today, is
     * resolved into a half-open range, and an event is kept when it starts before the range ends and ends at
     * or after the range begins, so events without those timestamps remain visible.
     *
     * @param scope optional day, week, month or year scope; absent or blank disables the range filter
     * @param anchor optional yyyy-MM-dd anchor date used to resolve the scope; defaults to today
     * @return the matching events in effective start order; never {@code null}
     * @throws ResponseStatusException with status 400 when the scope or the anchor cannot be parsed
     */
    @GetMapping
    public List<EventDto> list(@RequestParam(value = "scope", required = false) String scope,
                               @RequestParam(value = "anchor", required = false) String anchor) {
        if (scope == null || scope.isBlank()) {
            return repository.findAll();
        }

        TaskScope parsed;
        try {
            parsed = TaskScope.parse(scope);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        LocalDate day;
        try {
            day = (anchor == null || anchor.isBlank()) ? LocalDate.now() : LocalDate.parse(anchor);
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "anchor must be yyyy-MM-dd");
        }

        TaskScope.Range range = parsed.range(day);
        return repository.search(range.from(), range.to());
    }

    /**
     * Returns one event by its identifier.
     *
     * @param id identifier of the event to load
     * @return the matching event
     * @throws ResponseStatusException with status 404 when no live event has the given identifier
     */
    @GetMapping("/{id}")
    public EventDto get(@PathVariable("id") long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));
    }

    /**
     * Creates an event after normalising its text fields and validating its time window.
     *
     * @param request payload holding the requested event fields
     * @return the created event as stored, returned with the 201 status requested by the mapping
     * @throws ResponseStatusException with status 400 when the name is missing or too long, when the
     *         description is too long, or when the end time precedes the start time
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto create(@RequestBody EventRequest request) {
        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());
        validateTimes(request.startAt(), request.endAt());

        long id = repository.insert(name, description, request.startAt(), request.endAt());
        return repository.findById(id).orElseThrow();
    }

    /**
     * Replaces the editable fields of an existing event.
     *
     * The event must already exist; its identifier is kept and only the supplied name, description, start
     * time and end time are written.
     *
     * @param id identifier of the event to update
     * @param request payload holding the new event fields
     * @return the updated event as stored
     * @throws ResponseStatusException with status 404 when the event does not exist, or status 400 when the
     *         name, description or time window is invalid
     */
    @PutMapping("/{id}")
    public EventDto update(@PathVariable("id") long id, @RequestBody EventRequest request) {
        repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));

        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());
        validateTimes(request.startAt(), request.endAt());

        repository.update(id, name, description, request.startAt(), request.endAt());
        return repository.findById(id).orElseThrow();
    }

    /**
     * Soft-deletes one event by its identifier.
     *
     * @param id identifier of the event to delete
     * @throws ResponseStatusException with status 404 when no row was deleted, meaning the event does not
     *         exist or is already deleted
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        if (repository.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found");
        }
    }

    /**
     * Soft-deletes every event whose end time lies in the past.
     *
     * Events without an end time are left untouched.
     *
     * @return a map whose single entry reports how many events were removed
     */
    @DeleteMapping("/past")
    public Map<String, Integer> deletePast() {
        return Map.of("deleted", repository.deleteEnded());
    }

    /**
     * Trims and validates an event name supplied by a client.
     *
     * @param raw name supplied by the caller, possibly {@code null}
     * @return the trimmed, non-blank name of at most 100 characters
     * @throws ResponseStatusException with status 400 when the trimmed name is empty or longer than 100
     *         characters
     */
    private String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (name.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name too long");
        }
        return name;
    }

    /**
     * Trims an optional event description and validates its length.
     *
     * @param raw description supplied by the caller, possibly {@code null}
     * @return the trimmed description, empty when the input was {@code null}
     * @throws ResponseStatusException with status 400 when the trimmed description exceeds 1000 characters
     */
    private String normalizeDescription(String raw) {
        String description = raw == null ? "" : raw.trim();
        if (description.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description too long");
        }
        return description;
    }

    /**
     * Verifies that an event does not end before it starts.
     *
     * Both bounds are optional: the check only fails when both are present and the end is strictly before
     * the start, so equal start and end values are accepted.
     *
     * @param startAt optional event start
     * @param endAt optional event end
     * @throws ResponseStatusException with status 400 when both values are present and the end precedes the
     *         start
     */
    private void validateTimes(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endAt cannot be before startAt");
        }
    }
}
