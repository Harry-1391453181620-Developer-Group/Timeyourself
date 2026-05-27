package controller;
import javax.swing.*;

import models.EmergencyTask;
import models.EmergencyTask.*;
import models.Event;
import models.Task;
import models.Task.*;
import models.Event.*;
import view.GUIConfig;

import java.awt.*;

public class Convertor implements ConvertorInterface {
    public static void EmergencyTaskConvertToJTextArea(JPanel jp, EmergencyTask emergencyTask, Color color) {
        JTextArea textArea = new JTextArea();
        textArea.setBackground(color);
        textArea.setBorder(GUIConfig.globalBorder);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(emergencyTask.toString());
        textArea.setFont(GUIConfig.labelFont);
        Dimension dynamicSize = GUIConfig.getSingleEntrySize(textArea);
        textArea.setMaximumSize(dynamicSize);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        jp.add(textArea);
        jp.add(Box.createVerticalStrut(15));
        jp.revalidate();
        jp.repaint();
    }

    public static void TaskConvertToJTextArea(JPanel jp, Task task, Color color) {
        JTextArea textArea = new JTextArea();
        textArea.setBackground(color);
        textArea.setBorder(GUIConfig.globalBorder);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(task.toString());
        textArea.setFont(GUIConfig.labelFont);
        Dimension dynamicSize = GUIConfig.getSingleEntrySize(textArea);
        textArea.setMaximumSize(dynamicSize);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        jp.add(textArea);
        jp.add(Box.createVerticalStrut(15));
        jp.revalidate();
        jp.repaint();
    }

    public static void EventConvertToJTextArea(JPanel jp, Event event, Color color) {
        JTextArea textArea = new JTextArea();
        textArea.setBackground(color);
        textArea.setBorder(GUIConfig.globalBorder);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(event.toString());
        textArea.setFont(GUIConfig.labelFont);
        Dimension dynamicSize = GUIConfig.getSingleEntrySize(textArea);
        textArea.setMaximumSize(dynamicSize);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);
        jp.add(textArea);
        jp.add(Box.createVerticalStrut(15));
        jp.revalidate();
        jp.repaint();
    }
}
