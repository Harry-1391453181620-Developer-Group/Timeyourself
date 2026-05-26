package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a task in the Time Yourself application.
 * A task has a name, description, completion status, start date, and due date.
 * It also tracks a unique index based on the order of creation.
 * Superclass of EmergencyTask class.
 */
public class Task {
    /** The name of the task. */
    public String taskName;
    /** The description of the task. */
    public String taskDescription;
    /** The completion status of the task. */
    public boolean isCompleted;
    /** The due date and time of the task. */
    public LocalDateTime dueDate;
    /** The start date and time of the task. */
    public LocalDateTime startDate;
    /** The unique index of the task. */
    public int index;
    /** The total count of tasks created. */
    public static int taskCount = 0;

    /**
     * Default constructor. Initializes a task with empty values and current task count as index.
     */
    public Task() {
        this.setTaskName("");
        this.setTaskDescription("");
        this.isCompleted = false;
        this.setDueDate(null);
        this.setStartDate(null);
        this.index = taskCount;
        taskCount++;
    }

    /**
     * Constructs a task with the specified name.
     *
     * @param taskName the name of the task
     */
    public Task(String taskName) {
        this.setTaskName(taskName);
        this.setTaskDescription("");
        this.isCompleted = false;
        this.setDueDate(null);
        this.setStartDate(null);
        this.index = taskCount;
        taskCount++;
    }

    /**
     * Constructs a task with the specified name, due date, and start date.
     *
     * @param taskName  the name of the task
     * @param dueDate   the due date of the task
     * @param startDate the start date of the task
     */
    public Task(String taskName, LocalDateTime dueDate, LocalDateTime startDate) {
        this.setTaskName(taskName);
        this.setTaskDescription("");
        this.isCompleted = false;
        this.setDueDate(dueDate);
        this.setStartDate(startDate);
        this.index = taskCount;
        taskCount++;
    }

    /**
     * Constructs a task with the specified name, description, due date, and start date.
     *
     * @param taskName        the name of the task
     * @param taskDescription the description of the task
     * @param dueDate         the due date of the task
     * @param startDate       the start date of the task
     */
    public Task(String taskName, String taskDescription, LocalDateTime dueDate, LocalDateTime startDate) {
        this.setTaskName(taskName);
        this.setTaskDescription(taskDescription);
        this.isCompleted = false;
        this.setDueDate(dueDate);
        this.setStartDate(startDate);
        this.index = taskCount;
        taskCount++;
    }

    /**
     * Constructs a task with the specified name, description, and start date.
     * The due date is set to null.
     *
     * @param taskName        the name of the task
     * @param taskDescription the description of the task
     * @param startDate       the start date of the task
     */
    public Task(String taskName, String taskDescription, LocalDateTime startDate) {
        this.setTaskName(taskName);
        this.setTaskDescription(taskDescription);
        this.isCompleted = false;
        this.setDueDate(null);
        this.setStartDate(startDate);
        this.index = taskCount;
        taskCount++;
    }

    /**
     * Returns the name of the task.
     *
     * @return the task name
     */
    public String getTaskName() {
        return taskName;
    }

    /**
     * Sets the name of the task.
     * If the provided name is null, a default name "Task " + index is used.
     *
     * @param taskName the new name of the task
     */
    public void setTaskName(String taskName) {
        if (taskName != null) {
            this.taskName = taskName;
        } else {
            this.taskName = "Task " + this.index;
        }
    }

    /**
     * Returns the description of the task.
     *
     * @return the task description
     */
    public String getTaskDescription() {
        return taskDescription;
    }

    /**
     * Sets the description of the task.
     * If the description is not null, appends "No due date set."
     * If null, sets description to "No Description".
     *
     * @param taskDescription the new description of the task
     */
    public void setTaskDescription(String taskDescription) {
        if (taskDescription != null) {
            this.taskDescription = taskDescription + "No due date set.";
        } else {
            this.taskDescription = "No Description";
        }
    }

    /**
     * Checks if the task is completed.
     *
     * @return true if the task is completed, false otherwise
     */
    public boolean isCompleted() {
        return isCompleted;
    }

    /**
     * Marks the task as completed.
     */
    public void setCompleted() {
        this.isCompleted = true;
    }

    /**
     * Returns the due date of the task.
     *
     * @return the due date
     */
    public LocalDateTime getDueDate() {
        return dueDate;
    }

    /**
     * Sets the due date of the task.
     * Validates that the due date is not before the start date.
     *
     * @param dueDate the new due date
     * @throws IllegalArgumentException if the due date is before the start date
     */
    public void setDueDate(LocalDateTime dueDate) {
        if (dueDate == null) {
            this.dueDate = null;
            return;
        }
        if (this.startDate != null && dueDate.isBefore(this.startDate)) {
            throw new IllegalArgumentException("Due date cannot be before start date");
        }
        this.dueDate = dueDate;
    }

    /**
     * Returns the start date of the task.
     *
     * @return the start date
     */
    public LocalDateTime getStartDate() {
        return startDate;
    }

    /**
     * Sets the start date of the task.
     * Validates that the start date is not after the due date.
     *
     * @param startDate the new start date
     * @throws IllegalArgumentException if the start date is after the due date
     */
    public void setStartDate(LocalDateTime startDate) {
        if (startDate == null) {
            this.startDate = null;
            return;
        }
        if (this.startDate != null && startDate.isAfter(this.dueDate)) {
            throw new IllegalArgumentException("Due date cannot be before start date");
        }
        this.startDate = startDate;
    }

    /**
     * Returns the index of the task.
     *
     * @return the task index
     */
    public int getIndex() {
        return index;
    }

    /**
     * Returns the due date as a formatted string.
     *
     * @return the formatted due date or "No due date set." if null
     */
    public String getDueDateAsString() {
        if (dueDate == null) {
            return "No due date set.";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return dueDate.format(formatter);
        }
    }

    /**
     * Returns the start date as a formatted string.
     *
     * @return the formatted start date or "No start date set." if null
     */
    public String getStartDateAsString() {
        if (startDate == null) {
            return "No start date set.";
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            return startDate.format(formatter);
        }
    }

    /**
     * Returns the total number of tasks created.
     *
     * @return the task count
     */
    public static int getTaskCount() {
        return taskCount;
    }

    /**
     * Converts this regular task to an emergency task.
     * Note: In Java, explicit deletion of an object's storage space is not possible.
     * The object will be garbage collected when no longer referenced.
     * This method returns the new EmergencyTask so the caller can replace the reference.
     *
     * @return the new Task object
     */
    public EmergencyTask toEmergencyTask() {
        EmergencyTask task = new EmergencyTask(this.taskName, this.taskDescription, this.dueDate, this.startDate);
        System.gc(); // Suggest garbage collection
        return task;
    }

    /**
     * Returns a string representation of the task.
     *
     * @return a string containing task details
     */
    @Override
    public String toString() {
        return "Emergency Task: " + this.taskName + " - " + this.taskDescription + " - Due Date: " + this.dueDate + " - Start Date: " + this.startDate;
    }
}
