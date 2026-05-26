package controller;
import javax.swing.*;

import models.EmergencyTask;
import models.EmergencyTask.*;
import models.Event;
import models.Task;
import models.Task.*;
import models.Event.*;

import java.awt.*;

public class Convertor implements ConvertorInterface {
    public static void EmergencyTaskConvertToJTextArea(JPanel jp, EmergencyTask emergencyTask, Color color, Dimension size) {
        JTextArea textArea = new JTextArea();
        textArea.setBackground(color);
        textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(emergencyTask.toString());
        textArea.setMaximumSize(size);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        jp.add(textArea);
        jp.add(Box.createVerticalStrut(15));
    }

    public static void TaskConvertToJTextArea(JPanel jp, Task task, Color color, Dimension size) {
        JTextArea textArea = new JTextArea();
        textArea.setBackground(color);
        textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(task.toString());
        textArea.setMaximumSize(size);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        jp.add(textArea);
        jp.add(Box.createVerticalStrut(15));
    }

    public static void EventConvertToJTextArea(JPanel jp, Event event, Color color, Dimension size) {
        JTextArea textArea = new JTextArea();
        textArea.setBackground(color);
        textArea.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(event.toString());
        textArea.setMaximumSize(size);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        jp.add(textArea);
        jp.add(Box.createVerticalStrut(15));
    }
}
