package com.timeyourself.api;

import com.timeyourself.model.Plan;
import com.timeyourself.repository.PlanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/plans")
public class PlanController {

    public record PlanRequest(String name, String description) {
    }

    private final PlanRepository repository;

    public PlanController(PlanRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Plan> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Plan get(@PathVariable("id") long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "plan not found"));
    }

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

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        if (repository.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "plan not found");
        }
    }

    @DeleteMapping
    public Map<String, Integer> deleteAll() {
        return Map.of("deleted", repository.deleteAll());
    }

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

    private String normalizeDescription(String raw) {
        String description = raw == null ? "" : raw.trim();
        if (description.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "description too long");
        }
        return description;
    }
}