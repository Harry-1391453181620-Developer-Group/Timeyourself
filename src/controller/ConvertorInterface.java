package controller;

import models.EmergencyTask;
import models.Task;
import models.Event;

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
