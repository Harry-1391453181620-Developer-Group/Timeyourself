package com.timeyourself.api;

import com.timeyourself.dto.TaskDto;
import com.timeyourself.repository.CategoryRepository;
import com.timeyourself.repository.PlanRepository;
import com.timeyourself.repository.TaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    public record TaskRequest(String name,
                              String description,
                              LocalDateTime startAt,
                              LocalDateTime dueAt,
                              Long categoryId,
                              Long planId) {
    }

    public record CompletionRequest(Boolean completed) {
    }

    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final PlanRepository planRepository;

    public TaskController(TaskRepository taskRepository,
                          CategoryRepository categoryRepository,
                          PlanRepository planRepository) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.planRepository = planRepository;
    }

    @GetMapping
    public List<TaskDto> list(@RequestParam(value = "scope", required = false) String scope,
                              @RequestParam(value = "anchor", required = false) String anchor,
                              @RequestParam(value = "categoryId", required = false) Long categoryId,
                              @RequestParam(value = "planId", required = false) Long planId) {
        if (categoryId != null || planId != null) {
            return taskRepository.search(null, null, categoryId, planId);
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
        return taskRepository.search(range.from(), range.to(), null, null);
    }

    @GetMapping("/{id}")
    public TaskDto get(@PathVariable("id") long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto create(@RequestBody TaskRequest request) {
        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());
        validateTimes(request.startAt(), request.dueAt());
        ensureCategoryExists(request.categoryId());
        ensurePlanExists(request.planId());

        long id = taskRepository.insert(name, description,
                request.startAt(), request.dueAt(),
                request.categoryId(), request.planId());
        return taskRepository.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public TaskDto update(@PathVariable("id") long id, @RequestBody TaskRequest request) {
        taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));

        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());
        validateTimes(request.startAt(), request.dueAt());
        ensureCategoryExists(request.categoryId());
        ensurePlanExists(request.planId());

        taskRepository.update(id, name, description,
                request.startAt(), request.dueAt(),
                request.categoryId(), request.planId());
        return taskRepository.findById(id).orElseThrow();
    }

    @PatchMapping("/{id}/completion")
    public TaskDto completion(@PathVariable("id") long id, @RequestBody CompletionRequest request) {
        if (request.completed() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "completed is required");
        }
        if (taskRepository.updateCompletion(id, request.completed()) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found");
        }
        return taskRepository.findById(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        if (taskRepository.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found");
        }
    }

    @DeleteMapping("/completed")
    public Map<String, Integer> deleteCompleted() {
        return Map.of("deleted", taskRepository.deleteCompleted());
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

    private void validateTimes(LocalDateTime startAt, LocalDateTime dueAt) {
        if (startAt != null && dueAt != null && dueAt.isBefore(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueAt cannot be before startAt");
        }
    }

    private void ensureCategoryExists(Long categoryId) {
        if (categoryId != null && categoryRepository.findById(categoryId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category not found: " + categoryId);
        }
    }

    private void ensurePlanExists(Long planId) {
        if (planId != null && planRepository.findById(planId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "plan not found: " + planId);
        }
    }
}