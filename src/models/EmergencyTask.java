package models;

import java.time.LocalDateTime;

/**
 * Inherit from Task class.
 * Represents an emergency task in the Time Yourself application.
 * An emergency task is a specialized type of {@link Task} that is flagged as urgent.
 */
public class EmergencyTask extends Task {
    /**
     * Indicates whether the task is an emergency task.
     * Note: This field is static, which means it is shared across all instances.
     * This might be intended to track if *any* emergency task exists or if the current context is emergency mode.
     */
    private static boolean isEmergencyTask = true;

    /**
     * Default constructor. Initializes an emergency task with empty values.
     */
    public EmergencyTask() {
        super();
        this.setTaskName("");
        this.setTaskDescription("");
        this.isCompleted = false;
        this.setDueDate(null);
        this.setStartDate(null);
        this.index = taskCount;
        isEmergencyTask = true;
        taskCount++;
    }

    /**
     * Constructs an emergency task with the specified name.
     *
     * @param taskName the name of the emergency task
     */
    public EmergencyTask(String taskName) {
        super();
        this.setTaskName(taskName);
        this.setTaskDescription("");
        this.isCompleted = false;
        this.setDueDate(null);
        this.setStartDate(null);
        this.index = taskCount;
        isEmergencyTask = true;
        taskCount++;
    }

    /**
     * Constructs an emergency task with the specified name, due date, and start date.
     *
     * @param taskName  the name of the emergency task
     * @param dueDate   the due date of the task
     * @param startDate the start date of the task
     */
    public EmergencyTask(String taskName, LocalDateTime dueDate, LocalDateTime startDate) {
        super();
        this.setTaskName(taskName);
        this.setTaskDescription("");
        this.isCompleted = false;
        this.setDueDate(dueDate);
        this.setStartDate(startDate);
        this.index = taskCount;
        isEmergencyTask = true;
        taskCount++;
    }

    /**
     * Constructs an emergency task with the specified name, description, due date, and start date.
     *
     * @param taskName        the name of the emergency task
     * @param taskDescription the description of the task
     * @param dueDate         the due date of the task
     * @param startDate       the start date of the task
     */
    public EmergencyTask(String taskName, String taskDescription, LocalDateTime dueDate, LocalDateTime startDate) {
        super();
        this.setTaskName(taskName);
        this.setTaskDescription(taskDescription);
        this.isCompleted = false;
        this.setDueDate(dueDate);
        this.setStartDate(startDate);
        this.index = taskCount;
        isEmergencyTask = true;
        taskCount++;
    }

    /**
     * Constructs an emergency task with the specified name, description, and start date.
     * The due date is set to null.
     *
     * @param taskName        the name of the emergency task
     * @param taskDescription the description of the task
     * @param startDate       the start date of the task
     */
    public EmergencyTask(String taskName, String taskDescription, LocalDateTime startDate) {
        super();
        this.setTaskName(taskName);
        this.setTaskDescription(taskDescription + "No due date set.");
        this.isCompleted = false;
        this.setDueDate(null);
        this.setStartDate(startDate);
        this.index = taskCount;
        isEmergencyTask = true;
        taskCount++;
    }

    /**
     * Converts this emergency task to a regular task.
     * Note: In Java, explicit deletion of an object's storage space is not possible.
     * The object will be garbage collected when no longer referenced.
     * This method returns the new Task so the caller can replace the reference.
     *
     * @return the new Task object
     */
    public Task toTask() {
        Task task = new Task(this.taskName, this.taskDescription, this.dueDate, this.startDate);
        isEmergencyTask = false;
        System.gc(); // Suggest garbage collection
        return task;
    }

    /**
     * Returns a string representation of the emergency task.
     *
     * @return a string containing emergency task details
     */
    @Override
    public String toString() {
        return "Emergency Task: " + this.taskName + " - " + this.taskDescription + " - Due Date: " + this.dueDate + " - Start Date: " + this.startDate;
    }
}
