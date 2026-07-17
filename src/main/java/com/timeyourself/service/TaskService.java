package com.timeyourself.service;

import com.timeyourself.model.Task;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class TaskService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Creates a Task object based on the provided parameters, using all the provided constructors in EmergencyTask class.
     *
     * @param name     the name of the task
     * @param desc     the description of the task
     * @param startStr the start date string of the task
     * @param dueStr   the due date string of the task
     * @return the created Task object
     */
    public @NotNull Task create(@NotNull String name, String desc, String startStr, String dueStr) {
        name = name.trim();
        desc = desc.trim();
        startStr = startStr.trim();
        dueStr = dueStr.trim();
        if (name.isEmpty() && desc.isEmpty() && startStr.isEmpty() && dueStr.isEmpty()) {
            return new Task();
        } else if (desc.isEmpty() && startStr.isEmpty() && dueStr.isEmpty()) {
            return new Task(name);
        } else if (desc.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime dueDate = LocalDateTime.parse(dueStr, formatter);
            LocalDateTime startDate = LocalDateTime.parse(startStr, formatter);
            return new Task(name, dueDate, startDate);
        } else if (dueStr.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime startDate = LocalDateTime.parse(startStr, formatter);
            return new Task(name, desc, startDate);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime startDate = LocalDateTime.parse(startStr, formatter);
            LocalDateTime dueDate = LocalDateTime.parse(dueStr, formatter);
            return new Task(name, desc, dueDate, startDate);
        }
    }
}
