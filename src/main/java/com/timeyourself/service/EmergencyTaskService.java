package com.timeyourself.service;

import com.timeyourself.model.EmergencyTask;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EmergencyTaskService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Creates an EmergencyTask object based on the provided parameters, using all the provided constructors in EmergencyTask class.
     *
     * @param name     the name of the emergency task
     * @param desc     the description of the emergency task
     * @param startStr the start date string of the emergency task
     * @param dueStr   the due date string of the emergency task
     * @return the created EmergencyTask object
     */
    public @NotNull EmergencyTask create(@NotNull String name, String desc, String startStr, String dueStr) {
        name = name.trim();
        desc = desc.trim();
        startStr = startStr.trim();
        dueStr = dueStr.trim();
        if (name.isEmpty() && desc.isEmpty() && startStr.isEmpty() && dueStr.isEmpty()) {
            return new EmergencyTask();
        } else if (desc.isEmpty() && startStr.isEmpty() && dueStr.isEmpty()) {
            return new EmergencyTask(name);
        } else if (startStr.isEmpty() && dueStr.isEmpty()) {
            return new EmergencyTask(name, desc);
        } else if (desc.isEmpty()) {
            LocalDateTime dueDate = LocalDateTime.parse(dueStr, FORMATTER);
            LocalDateTime startDate = LocalDateTime.parse(startStr, FORMATTER);
            return new EmergencyTask(name, startDate, dueDate);
        } else if (dueStr.isEmpty()) {
            LocalDateTime startDate = LocalDateTime.parse(startStr, FORMATTER);
            return new EmergencyTask(name, desc, startDate);
        } else {
            LocalDateTime startDate = LocalDateTime.parse(startStr, FORMATTER);
            LocalDateTime dueDate = LocalDateTime.parse(dueStr, FORMATTER);
            return new EmergencyTask(name, desc, startDate, dueDate);
        }
    }
}