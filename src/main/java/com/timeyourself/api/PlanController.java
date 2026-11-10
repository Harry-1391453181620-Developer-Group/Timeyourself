package com.timeyourself.api;

import com.timeyourself.model.Plan;
import com.timeyourself.repository.PlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing CRUD operations for plans under {@code /api/plans}.
 *
 * A plan groups tasks under a shared goal: it owns a name and an optional description, and names are
 * validated and kept unique without regard to letter case.
 */
@RestController
@RequestMapping("/api/plans")
public class PlanController {

    /**
     * Request body carrying the editable fields of a plan.
     *
     * @param name plan name; trimmed, required and limited to 50 characters
     * @param description optional plan description; trimmed and limited to 500 characters
     */
    public record PlanRequest(String name, String description) {
    }

    /**
     * Repository used to read and mutate the stored plans.
     */
    private final PlanRepository repository;

    /**
     * Creates the controller with the plan repository it delegates all persistence to.
     *
     * @param repository repository providing the plan operations
     */
    public PlanController(PlanRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists every stored plan together with its task count.
     *
     * @return all plans ordered by name ignoring case; never {@code null}
     */
    @GetMapping
    public List<Plan> list() {
        return repository.findAll();
    }

    /**
     * Returns one plan by its identifier.
     *
     * @param id identifier of the plan to load
     * @return the matching plan
     * @throws ResponseStatusException with status 404 when no plan has the given identifier
     */
    @GetMapping("/{id}")
    public Plan get(@PathVariable("id") long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "plan not found"));
    }

    /**
     * Creates a plan after normalising and validating its name and description.
     *
     * @param request payload holding the requested plan fields
     * @return the created plan as stored, returned with the 201 status requested by the mapping
     * @throws ResponseStatusException with status 400 when the name or description is invalid, or status 409
     *         when a plan with the same name already exists in any letter case
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Plan create(@RequestBody PlanRequest request) {
        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());
        if (repository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "plan name already exists");
        }
        long id = repository.insert(name, description);
        return repository.findById(id).orElseThrow();
    }

    /**
     * Replaces the name and description of an existing plan.
     *
     * A name collision is only rejected when the requested name differs from the current one ignoring case,
     * so resubmitting the same name with different capitalisation is allowed.
     *
     * @param id identifier of the plan to update
     * @param request payload holding the new plan fields
     * @return the updated plan as stored
     * @throws ResponseStatusException with status 404 when the plan does not exist, status 400 when the name
     *         or description is invalid, or status 409 when another plan already uses the name
     */
    @PutMapping("/{id}")
    public Plan update(@PathVariable("id") long id, @RequestBody PlanRequest request) {
        Plan current = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "plan not found"));

        String name = normalizeName(request.name());
        String description = normalizeDescription(request.description());

        if (!current.getName().equalsIgnoreCase(name) && repository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "plan name already exists");
        }
        repository.update(id, name, description);
        return repository.findById(id).orElseThrow();
    }

    /**
     * Deletes one plan by its identifier.
     *
     * @param id identifier of the plan to delete
     * @throws ResponseStatusException with status 404 when no row was deleted, meaning the plan does not
     *         exist
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        if (repository.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "plan not found");
        }
    }

    /**
     * Deletes every plan in one call.
     *
     * @return a map whose single entry reports how many plans were removed
     */
    @DeleteMapping
    public Map<String, Integer> deleteAll() {
        return Map.of("deleted", repository.deleteAll());
    }

    /**
     * Trims and validates a plan name supplied by a client.
     *
     * @param raw name supplied by the caller, possibly {@code null}
     * @return the trimmed, non-blank name of at most 50 characters
     * @throws ResponseStatusException with status 400 when the trimmed name is empty or longer than 50
     *         characters
     */
    private String normalizeName(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name too long");
        }
        return name;
    }

    /**
     * Trims an optional plan description and validates its length.
     *
     * @param raw description supplied by the caller, possibly {@code null}
     * @return the trimmed description, empty when the input was {@code null}
     * @throws ResponseStatusException with status 400 when the trimmed description exceeds 500 characters
     */
    private String normalizeDescription(String raw) {
        String description = raw == null ? "" : raw.trim();
        if (description.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description too long");
        }
        return description;
    }
}
