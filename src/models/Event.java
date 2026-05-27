package models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents an event in the Time Yourself application.
 * An event has a name, date, location, and description.
 */
public class Event {
    /** The name of the event. */
    private String name;
    /** The date and time of the event. */
    private LocalDateTime date;
    /** The location of the event. */
    private String location;
    /** The description of the event. */
    private String description;

    /**
     * Constructs an event with the specified name, date, location, and description.
     *
     * @param name        the name of the event
     * @param date        the date and time of the event
     * @param location    the location of the event
     * @param description the description of the event
     */
    public Event(String name, LocalDateTime date, String location, String description) {
        this.setName(name);
        this.setDate(date);
        this.setDescription(description);
        this.setLocation(location);
    }

    /**
     * Constructs an event with the specified name, description, and date.
     *
     * @param name        the name of the event
     * @param description the description of the event
     * @param date        the date and time of the event
     */
    public Event(String name, String description, LocalDateTime date) {
        this.setName(name);
        this.setDate(date);
        this.setDescription(description);
    }

    /**
     * Constructs an event with the specified name, date, and location.
     *
     * @param name     the name of the event
     * @param date     the date and time of the event
     * @param location the location of the event
     */
    public Event(String name, LocalDateTime date, String location) {
        this.setName(name);
        this.setDate(date);
        this.setLocation(location);
    }

    /**
     * Constructs an event with the specified name and date.
     *
     * @param name the name of the event
     * @param date the date and time of the event
     */
    public Event(String name, LocalDateTime date) {
        this.setName(name);
        this.setDate(date);
    }

    /**
     * Returns the name of the event.
     *
     * @return the event name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the event.
     *
     * @param name the new name of the event
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the date and time of the event.
     *
     * @return the event date
     */
    public LocalDateTime getDate() {
        return date;
    }

    /**
     * Sets the date and time of the event.
     *
     * @param date the new date of the event
     */
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    /**
     * Returns the location of the event.
     *
     * @return the event location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location of the event.
     *
     * @param location the new location of the event
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Returns a string representation of the event.
     *
     * @return a string containing event details
     */
    public String toString() {
        return "Event: " + this.name + " - " + this.description + " - Date: " + this.date + " - Location: " + this.location;
    }

    /**
     * Returns the description of the event.
     *
     * @return the event description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of the event.
     *
     * @param description the new description of the event
     */
    public void setDescription(String description) {
        this.description = description;
    }
}
