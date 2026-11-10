package com.timeyourself.model;

import java.time.LocalDateTime;

/**
 * Mutable model of a plan, a named collection of tasks, carrying the persisted values together with the number
 * of tasks that belong to the plan and therefore cannot be mapped from a single table row alone.
 */
public class Plan {

    /**
     * Database identifier of the plan, {@code null} until the plan has been persisted.
     */
    private Long id;
    /**
     * Display name of the plan.
     */
    private String name;
    /**
     * Optional free-text description of what the plan is meant for.
     */
    private String description;
    /**
     * Number of tasks assigned to the plan, populated by aggregate queries rather than by the table row.
     */
    private Long taskCount;
    /**
     * Creation timestamp of the plan as stored in the database.
     */
    private LocalDateTime createdAt;

    /**
     * Creates an empty plan whose properties are populated through the setters, for example by the persistence
     * layer or by JSON deserialization.
     */
    public Plan() {
    }

    /**
     * Returns the database identifier of the plan.
     *
     * @return identifier, or {@code null} when the plan has not been persisted yet
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the database identifier of the plan.
     *
     * @param id identifier to store, or {@code null} for an unpersisted plan
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the display name of the plan.
     *
     * @return plan name, or {@code null} when it has not been set
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the display name of the plan.
     *
     * @param name plan name to store
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the free-text description of the plan.
     *
     * @return description, or {@code null} when the plan has none
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the free-text description of the plan.
     *
     * @param description description to store, or {@code null} to clear it
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the number of tasks assigned to the plan.
     *
     * @return task count, or {@code null} when the value has not been computed
     */
    public Long getTaskCount() {
        return taskCount;
    }

    /**
     * Sets the number of tasks assigned to the plan.
     *
     * @param taskCount task count to store
     */
    public void setTaskCount(Long taskCount) {
        this.taskCount = taskCount;
    }

    /**
     * Returns the creation timestamp of the plan.
     *
     * @return creation timestamp, or {@code null} when it has not been loaded
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation timestamp of the plan.
     *
     * @param createdAt creation timestamp to store
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
