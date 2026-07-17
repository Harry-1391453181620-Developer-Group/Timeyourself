package com.timeyourself.service;

import com.timeyourself.model.Event;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class EventService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");


    /**
     * Creates an Event object based on the provided parameters, using all the provided constructors in EmergencyTask class.
     *
     * @param name        the name of the event
     * @param desc        the description of the event
     * @param dateStr     the date string of the event
     * @param locationStr the location string of the event
     * @return the created Event object
     */
    public @NotNull Event create(@NotNull String name, String desc, String dateStr, @NotNull String locationStr) {
        if (locationStr.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime date = LocalDateTime.parse(dateStr, formatter);
            return new Event(name, desc, date);
        } else if (desc.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime date = LocalDateTime.parse(dateStr, formatter);
            return new Event(name, date, locationStr);
        } else if (desc.isEmpty() && locationStr.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime date = LocalDateTime.parse(dateStr, formatter);
            return new Event(name, date);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime date = LocalDateTime.parse(dateStr, formatter);
            return new Event(name, date, locationStr, desc);
        }
    }
}
