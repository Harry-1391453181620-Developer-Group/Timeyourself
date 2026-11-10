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

/**
 * REST controller exposing task listing, CRUD operations and completion toggling under {@code /api/tasks}.
 *
 * Payloads are normalised before they reach the repository: names are trimmed and required, descriptions are
 * trimmed and optional, the due time may not precede the start time, and referenced categories and plans must
 * already exist.
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    /**
     * Request body carrying the editable fields of a task.
     *
     * @param name task name; trimmed, required and limited to 100 characters
     * @param description optional task description; trimmed and limited to 1000 characters
     * @param startAt optional start time of the task
     * @param dueAt optional due time of the task, which may not precede {@code startAt}
     * @param categoryId optional owning category identifier, which must reference an existing category
     * @param planId optional owning plan identifier, which must reference an existing plan
     */
    public record TaskRequest(String name,
                              String description,
                              LocalDateTime startAt,
                              LocalDateTime dueAt,
                              Long categoryId,
                              Long planId) {
    }

    /**
     * Request body carrying the desired completion state of a task.
     *
     * @param completed new completion flag; required and rejected when {@code null}
     */
    public record CompletionRequest(Boolean completed) {
    }

    /**
     * Repository used to read and mutate the stored tasks.
     */
    private final TaskRepository taskRepository;

    /**
     * Repository used to verify that a referenced category exists.
     */
    private final CategoryRepository categoryRepository;

    /**
     * Repository used to verify that a referenced plan exists.
     */
    private final PlanRepository planRepository;

    /**
     * Creates the controller with the repositories it delegates persistence and reference checks to.
     *
     * @param taskRepository repository providing the task operations
     * @param categoryRepository repository used to validate category references
     * @param planRepository repository used to validate plan references
     */
    public TaskController(TaskRepository taskRepository,
                          CategoryRepository categoryRepository,
                          PlanRepository planRepository) {
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.planRepository = planRepository;
    }

    /**
     * Lists tasks, filtered either by category or plan or by a calendar scope.
     *
     * When either {@code categoryId} or {@code planId} is supplied the scope and anchor are ignored and the
     * tasks are matched on the supplied associations alone; the identifiers are passed to the repository
     * without an existence check. Otherwise the scope defaults to day, the anchor defaults to today, and a
     * task is kept when it starts before the range ends and is due at or after the range begins, so tasks
     * without those timestamps remain visible.
     *
     * @param scope optional day, week, month or year scope; absent or blank means day
     * @param anchor optional yyyy-MM-dd anchor date used to resolve the scope; defaults to today
     * @param categoryId optional category filter, which takes precedence over the time scope
     * @param planId optional plan filter, which takes precedence over the time scope
     * @return the matching tasks in effective start order; never {@code null}
     * @throws ResponseStatusException with status 400 when the scope or the anchor cannot be parsed
     */
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

    /**
     * Returns one live task by its identifier.
     *
     * @param id identifier of the task to load
     * @return the matching task
     * @throws ResponseStatusException with status 404 when no task with the given identifier exists or it was
     *         already deleted
     */
    @GetMapping("/{id}")
    public TaskDto get(@PathVariable("id") long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
    }

    /**
     * Creates a task after normalising its text fields and validating its references and time window.
     *
     * The task is stored in the not-completed state, and the returned representation is read back from the
     * repository so that joined category and plan names are included.
     *
     * @param request payload holding the requested task fields
     * @return the created task as stored, returned with the 201 status requested by the mapping
     * @throws ResponseStatusException with status 400 when the name or description is invalid, when the due
     *         time precedes the start time, or when a referenced category or plan does not exist
     */
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

    /**
     * Replaces the editable fields of an existing task.
     *
     * The task must already exist; its identifier and completion state are preserved, while a missing
     * category or plan identifier clears the previous association.
     *
     * @param id identifier of the task to update
     * @param request payload holding the new task fields
     * @return the updated task as stored
     * @throws ResponseStatusException with status 404 when the task does not exist, or status 400 when the
     *         name, description, time window or a referenced category or plan is invalid
     */
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

    /**
     * Updates only the completion state of a task.
     *
     * Completing a task records the current time as its completion timestamp, while reopening clears that
     * timestamp; all other fields are left untouched.
     *
     * @param id identifier of the task to update
     * @param request payload holding the desired completion flag
     * @return the task as stored after the update
     * @throws ResponseStatusException with status 400 when the completion flag is absent, or status 404 when
     *         no live task has the given identifier
     */
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

    /**
     * Soft-deletes one task by its identifier.
     *
     * @param id identifier of the task to delete
     * @throws ResponseStatusException with status 404 when no row was deleted, meaning the task does not exist
     *         or is already deleted
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        if (taskRepository.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found");
        }
    }

    /**
     * Soft-deletes every completed task in one call.
     *
     * @return a map whose single entry reports how many tasks were removed
     */
    @DeleteMapping("/completed")
    public Map<String, Integer> deleteCompleted() {
        return Map.of("deleted", taskRepository.deleteCompleted());
    }

    /**
     * Trims and validates a task name supplied by a client.
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
     * Trims an optional task description and validates its length.
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
     * Verifies that a task is not due before it starts.
     *
     * Both bounds are optional: the check only fails when both are present and the due time is strictly
     * before the start time, so equal start and due values are accepted.
     *
     * @param startAt optional task start
     * @param dueAt optional task due time
     * @throws ResponseStatusException with status 400 when both values are present and the due time precedes
     *         the start time
     */
    private void validateTimes(LocalDateTime startAt, LocalDateTime dueAt) {
        if (startAt != null && dueAt != null && dueAt.isBefore(startAt)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueAt cannot be before startAt");
        }
    }

    /**
     * Verifies that an optional category reference points at an existing category.
     *
     * @param categoryId category identifier to check, possibly {@code null}
     * @throws ResponseStatusException with status 400 when the identifier is set but no such category exists
     */
    private void ensureCategoryExists(Long categoryId) {
        if (categoryId != null && categoryRepository.findById(categoryId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "category not found: " + categoryId);
        }
    }

    /**
     * Verifies that an optional plan reference points at an existing plan.
     *
     * @param planId plan identifier to check, possibly {@code null}
     * @throws ResponseStatusException with status 400 when the identifier is set but no such plan exists
     */
    private void ensurePlanExists(Long planId) {
        if (planId != null && planRepository.findById(planId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "plan not found: " + planId);
        }
    }
}
