package controller;
import javax.swing.*;

import models.EmergencyTask;
import models.EmergencyTask.*;
import models.Event;
import models.Task;
import models.Task.*;
import models.Event.*;
import org.jetbrains.annotations.NotNull;
import view.GUIConfig;

import java.awt.*;

/**
 * Responsible to convert abstract objects to UI components
 */
public class Convertor implements ConvertorInterface {
    /**
     * Converts the EmergencyTask objects to EmergencyTask entry component
     *
     * @param jp                         the panel that the entry component is going to add to
     * @param emergencyTask              the emergencyTask object that is going to be converted
     * @param singleEntryBackgroundColor the background color of the entry
     */
    public static void EmergencyTaskConvertToJTextArea(@NotNull JPanel jp, @NotNull EmergencyTask emergencyTask, Color singleEntryBackgroundColor){
        JPanel wrapperPanel = new JPanel(new BorderLayout(5, 0));
        wrapperPanel.setOpaque(false);
        wrapperPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));


        JTextArea textArea = new JTextArea();
        textArea.setBackground(singleEntryBackgroundColor);
        textArea.setBorder(GUIConfig.globalBorder);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(emergencyTask.toString());
        textArea.setFont(GUIConfig.labelFont);
        Dimension dynamicSize = GUIConfig.getSingleEntrySize(textArea);
        textArea.setMaximumSize(dynamicSize);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox deleteCheckBox = new JCheckBox("Delete");
        deleteCheckBox.setBackground(GUIConfig.globalCheckBoxColor);
        deleteCheckBox.setFont(GUIConfig.globalCheckBoxFont);
        deleteCheckBox.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(jp, "Sure to delete this emergency task?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                jp.remove(wrapperPanel);
                jp.revalidate();
                jp.repaint();
            } else {
                deleteCheckBox.setSelected(false);
            }
        });

        wrapperPanel.add(textArea, BorderLayout.CENTER);
        wrapperPanel.add(deleteCheckBox, BorderLayout.EAST);

        jp.add(wrapperPanel);
        jp.add(Box.createVerticalStrut(15));
        jp.revalidate();
        jp.repaint();
    }

    /**
     * Converts the Task objects to Task entry component
     *
     * @param jp                         the panel that the entry component is going to add to
     * @param task                       the emergencyTask object that is going to be converted
     * @param singleEntryBackgroundColor the background color of the entry
     * */
    public static void TaskConvertToJTextArea(@NotNull JPanel jp, @NotNull Task task, Color singleEntryBackgroundColor) {
        JPanel wrapperPanel = new JPanel(new BorderLayout(5, 0));
        wrapperPanel.setOpaque(false);
        wrapperPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));


        JTextArea textArea = new JTextArea();
        textArea.setBackground(singleEntryBackgroundColor);
        textArea.setBorder(GUIConfig.globalBorder);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(task.toString());
        textArea.setFont(GUIConfig.labelFont);
        Dimension dynamicSize = GUIConfig.getSingleEntrySize(textArea);
        textArea.setMaximumSize(dynamicSize);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox deleteCheckBox = new JCheckBox("Delete");
        deleteCheckBox.setBackground(GUIConfig.globalCheckBoxColor);
        deleteCheckBox.setFont(GUIConfig.globalCheckBoxFont);
        deleteCheckBox.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(jp, "Sure to delete this task?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                jp.remove(wrapperPanel);
                jp.revalidate();
                jp.repaint();
            } else {
                deleteCheckBox.setSelected(false);
            }
        });

        wrapperPanel.add(textArea, BorderLayout.CENTER);
        wrapperPanel.add(deleteCheckBox, BorderLayout.EAST);

        jp.add(wrapperPanel);
        jp.add(Box.createVerticalStrut(15));
        jp.revalidate();
        jp.repaint();
    }

    /**Converts the Event objects to Event entry component
     *
     * @param jp                         the panel that the entry component is going to add to
     * @param event                      the emergencyTask object that is going to be converted
     * @param singleEntryBackgroundColor the background color of the entry
     */
    public static void EventConvertToJTextArea(@NotNull JPanel jp, @NotNull Event event, Color singleEntryBackgroundColor) {
        JPanel wrapperPanel = new JPanel(new BorderLayout(5, 0));
        wrapperPanel.setOpaque(false);
        wrapperPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));


        JTextArea textArea = new JTextArea();
        textArea.setBackground(singleEntryBackgroundColor);
        textArea.setBorder(GUIConfig.globalBorder);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(event.toString());
        textArea.setFont(GUIConfig.labelFont);
        Dimension dynamicSize = GUIConfig.getSingleEntrySize(textArea);
        textArea.setMaximumSize(dynamicSize);
        textArea.setAlignmentX(Component.CENTER_ALIGNMENT);

        JCheckBox deleteCheckBox = new JCheckBox("Delete");
        deleteCheckBox.setBackground(GUIConfig.globalCheckBoxColor);
        deleteCheckBox.setFont(GUIConfig.globalCheckBoxFont);
        deleteCheckBox.addActionListener(e -> {
            int choice = JOptionPane.showConfirmDialog(jp, "Sure to delete this emergency task?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (choice == JOptionPane.YES_OPTION) {
                jp.remove(wrapperPanel);
                jp.revalidate();
                jp.repaint();
            } else {
                deleteCheckBox.setSelected(false);
            }
        });

        wrapperPanel.add(textArea, BorderLayout.CENTER);
        wrapperPanel.add(deleteCheckBox, BorderLayout.EAST);

        jp.add(wrapperPanel);
        jp.add(Box.createVerticalStrut(15));
        jp.revalidate();
        jp.repaint();
    }
}
