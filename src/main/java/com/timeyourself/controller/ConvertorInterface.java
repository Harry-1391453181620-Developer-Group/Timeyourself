package com.timeyourself.controller;

import com.timeyourself.model.EmergencyTask;
import com.timeyourself.model.Task;
import com.timeyourself.model.Event;

import javax.swing.*;
import java.awt.*;

/**
 * The interface of Convertor methods.
 */
public interface ConvertorInterface {
    public static void EmergencyTaskConvertToJTextArea(JPanel jp, EmergencyTask emergencyTask, Color singleEntryBackgroundColor, Dimension size) {

    }

    public static void TaskConvertToJTextArea(JPanel jp, Task task, Color singleEntryBackgroundColor, Dimension size) {

    }

    public static void EventConvertToJTextArea(JPanel jp, Event event, Color singleEntryBackgroundColor, Dimension size) {

    }
}
