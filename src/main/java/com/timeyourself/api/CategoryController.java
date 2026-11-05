package com.timeyourself.api;

import com.timeyourself.model.Category;
import com.timeyourself.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    public record CategoryRequest(String name) {
    }

    private final CategoryRepository repository;

    public CategoryController(CategoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<Category> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Category get(@PathVariable("id") long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "category not found"));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@RequestBody CategoryRequest request) {
        String name = normalize(request.name());
        if (repository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "category name already exists");
        }
        long id = repository.insert(name);
        return repository.findById(id).orElseThrow();
    }

    @PutMapping("/{id}")
    public Category rename(@PathVariable("id") long id, @RequestBody CategoryRequest request) {
        Category current = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "category not found"));

        String name = normalize(request.name());

        if (!current.getName().equalsIgnoreCase(name) && repository.existsByName(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "category name already exists");
        }

        repository.rename(id, name);
        return repository.findById(id).orElseThrow();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        if (repository.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "category not found");
        }
    }

    @DeleteMapping
    public Map<String, Integer> deleteAll() {
        return Map.of("deleted", repository.deleteAll());
    }

    private String normalize(String raw) {
        String name = raw == null ? "" : raw.trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (name.length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name too long");
        }
        return name;
    }
}