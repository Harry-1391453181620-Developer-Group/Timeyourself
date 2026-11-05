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

@RestController
@RequestMapping("/api/events")
public class EventController {

    public record EventRequest(String name,
                               String description,
                               LocalDateTime startAt,
                               LocalDateTime endAt) {
    }

    private final EventRepository repository;

    public EventController(EventRepository repository) {
        this.repository = repository;
    }

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

    @GetMapping("/{id}")
    public EventDto get(@PathVariable("id") long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDto create(@RequestBody EventRequest request) {
        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());
        validateTimes(request.startAt(), request.endAt());

        long id = repository.insert(name, description, request.startAt(), request.endAt());
        return repository.findById(id).orElseThrow();
    }

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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        if (repository.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "event not found");
        }
    }

    @DeleteMapping("/past")
    public Map<String, Integer> deletePast() {
        return Map.of("deleted", repository.deleteEnded());
    }

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

    private String normalizeDescription(String raw) {
        String description = raw == null ? "" : raw.trim();
        if (description.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description too long");
        }
        return description;
    }

    private void validateTimes(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endAt cannot be before startAt");
        }
    }
}