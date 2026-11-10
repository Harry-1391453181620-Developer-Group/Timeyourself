package com.timeyourself.api;

import com.timeyourself.model.Category;
import com.timeyourself.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST controller exposing CRUD operations for categories under {@code /api/categories}.
 *
 * Names are trimmed and validated before they reach the repository, and uniqueness is enforced without
 * regard to letter case, so two categories can never differ only by capitalisation.
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    /**
     * Request body carrying the name of a category to create or rename.
     *
     * @param name raw category name; trimmed, validated and stored by the controller
     */
    public record CategoryRequest(String name) {
    }

    /**
     * Repository used to read and mutate the stored categories.
     */
    private final CategoryRepository repository;

    /**
     * Creates the controller with the category repository it delegates all persistence to.
     *
     * @param repository repository providing the category operations
     */
    public CategoryController(CategoryRepository repository) {
        this.repository = repository;
    }

    /**
     * Lists every stored category together with its task count.
     *
     * @return all categories ordered by name ignoring case; never {@code null}
     */
    @GetMapping
    public List<Category> list() {
        return repository.findAll();
    }

    /**
     * Returns one category by its identifier.
     *
     * @param id identifier of the category to load
     * @return the matching category
     * @throws ResponseStatusException with status 404 when no category has the given identifier
     */
    @GetMapping("/{id}")
    public Category get(@PathVariable("id") long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "category not found"));
    }

    /**
     * Creates a category from a normalised, validated name.
     *
     * The name is trimmed, must not be blank and may not exceed 50 characters. The response carries the
     * persisted category and the 201 status requested by the mapping.
     *
     * @param request payload holding the requested name
     * @return the created category as stored
     * @throws ResponseStatusException with status 400 when the name is missing or too long, or status 409
     *         when a category with the same name already exists in any letter case
     */
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

    /**
     * Renames an existing category.
     *
     * A collision is only rejected when the requested name differs from the current one ignoring case, so
     * resubmitting the same name with different capitalisation is allowed.
     *
     * @param id identifier of the category to rename
     * @param request payload holding the new name
     * @return the updated category as stored
     * @throws ResponseStatusException with status 404 when the category does not exist, status 400 when the
     *         name is missing or too long, or status 409 when another category already uses the name
     */
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

    /**
     * Deletes one category by its identifier.
     *
     * @param id identifier of the category to delete
     * @throws ResponseStatusException with status 404 when no row was deleted, meaning the category does not
     *         exist
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") long id) {
        if (repository.deleteById(id) == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "category not found");
        }
    }

    /**
     * Deletes every category in one call.
     *
     * @return a map whose single entry reports how many categories were removed
     */
    @DeleteMapping
    public Map<String, Integer> deleteAll() {
        return Map.of("deleted", repository.deleteAll());
    }

    /**
     * Trims and validates a category name supplied by a client.
     *
     * @param raw name supplied by the caller, possibly {@code null}
     * @return the trimmed, non-blank name of at most 50 characters
     * @throws ResponseStatusException with status 400 when the trimmed name is empty or longer than 50
     *         characters
     */
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
