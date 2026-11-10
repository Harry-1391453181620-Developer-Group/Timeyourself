package com.timeyourself.model;

import java.time.LocalDateTime;

/**
 * Mutable model of a task category, carrying the persisted values together with the number of tasks that
 * reference the category and therefore cannot be mapped from a single table row alone.
 */
public class Category {

    /**
     * Database identifier of the category, {@code null} until the category has been persisted.
     */
    private Long id;
    /**
     * Display name of the category.
     */
    private String name;
    /**
     * Number of tasks assigned to the category, populated by aggregate queries rather than by the table row.
     */
    private Long taskCount;
    /**
     * Creation timestamp of the category as stored in the database.
     */
    private LocalDateTime createdAt;

    /**
     * Creates an empty category whose properties are populated through the setters, for example by the
     * persistence layer or by JSON deserialization.
     */
    public Category() {
    }

    /**
     * Returns the database identifier of the category.
     *
     * @return identifier, or {@code null} when the category has not been persisted yet
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the database identifier of the category.
     *
     * @param id identifier to store, or {@code null} for an unpersisted category
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the display name of the category.
     *
     * @return category name, or {@code null} when it has not been set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of the category.
     *
     * @param name category name to store
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the number of tasks assigned to the category.
     *
     * @return task count, or {@code null} when the value has not been computed
     */
    public Long getTaskCount() {
        return taskCount;
    }

    /**
     * Sets the number of tasks assigned to the category.
     *
     * @param taskCount task count to store
     */
    public void setTaskCount(Long taskCount) {
        this.taskCount = taskCount;
    }

    /**
     * Returns the creation timestamp of the category.
     *
     * @return creation timestamp, or {@code null} when it has not been loaded
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of the category.
     *
     * @param createdAt creation timestamp to store
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
