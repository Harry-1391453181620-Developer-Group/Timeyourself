package view;

import controller.Convertor;
import models.EmergencyTask;
import models.Task;
import models.Event;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

/** The main JFrame that the user first see. */
public class MainFrame {
    public MainFrame() {
        // Give the values of GUIConfig items to local variables.
        final ImageIcon icon = GUIConfig.icon;
        final Color mainBackgroundColor = GUIConfig.mainBackgroundColor;
        final Color JTabbedPaneBackgroundColor = GUIConfig.JTabbedPaneBackgroundColor;
        final Color singleEntryBackgroundColor = GUIConfig.singleEntryBackgroundColor;
        final Color buttonBackgroundColor = GUIConfig.buttonBackgroundColor;
        final Dimension modifyEntryButtonSize = GUIConfig.modifyEntryButtonSize;
        final Border globalBorder = GUIConfig.globalBorder;
        final Font labelFont = GUIConfig.labelFont;

        // Initialize MainFrame.
        JFrame frame = new JFrame("Timeyourself");
        frame.setSize(2000, 1500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(mainBackgroundColor);
        frame.setLocationRelativeTo(null);

        // The container of MainFrame.
        Container contentPane = frame.getContentPane();
        contentPane.setBackground(mainBackgroundColor);
        contentPane.setLayout(new BorderLayout());

        // The icon label
        JLabel label = new JLabel(icon, SwingConstants.CENTER);
        label.setVisible(true);
        contentPane.add(label, BorderLayout.NORTH);

        // The main tabbed pane
        JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        tabbedPane.setBackground(JTabbedPaneBackgroundColor);

        // Panel 1, for tasks
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
        panel1.setBackground(JTabbedPaneBackgroundColor);
        panel1.add(Box.createVerticalStrut(15));

        // The label for Task pane
        JLabel title1 = new JLabel("Tasks", SwingConstants.CENTER);
        title1.setFont(labelFont);
        title1.setAlignmentX(Component.CENTER_ALIGNMENT);
        title1.setVisible(true);
        panel1.add(title1);
        panel1.add(Box.createVerticalStrut(15));

        // Add task button
        JButton button1 = new JButton("Add Task");
        button1.setMaximumSize(modifyEntryButtonSize);
        button1.setAlignmentX(Component.CENTER_ALIGNMENT);
        button1.setBackground(buttonBackgroundColor);
        button1.setFont(new Font("Timeyourselflogo", Font.ITALIC, 24));
        button1.setBorder(globalBorder);
        button1.addActionListener(e -> {
            new AddTaskEntryDialog(frame, icon, mainBackgroundColor, JTabbedPaneBackgroundColor, buttonBackgroundColor, new AddTaskEntryDialog.TaskCallback() {
                @Override
                public void onTaskCreated(Task task) {
                    Convertor.TaskConvertToJTextArea(panel1, task, singleEntryBackgroundColor);
                }
            });
        });
        panel1.add(button1);
        panel1.add(Box.createVerticalStrut(15));

        // Panel 2, for emergency tasks
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
        panel2.setBackground(JTabbedPaneBackgroundColor);
        panel2.add(Box.createVerticalStrut(15));

        // Label for Emergency task pane
        JLabel title2 = new JLabel("Emergency tasks", SwingConstants.CENTER);
        title2.setFont(labelFont);
        title2.setAlignmentX(Component.CENTER_ALIGNMENT);
        title2.setVisible(true);
        panel2.add(title2);
        panel2.add(Box.createVerticalStrut(15));

        // Add EmergencyTask button
        JButton button2 = new JButton("Add Emergency task");
        button2.setMaximumSize(modifyEntryButtonSize);
        button2.setAlignmentX(Component.CENTER_ALIGNMENT);
        button2.setBackground(buttonBackgroundColor);
        button2.setFont(new Font("Timeyourselflogo", Font.ITALIC, 24));
        button2.setBorder(globalBorder);
        button2.addActionListener(e -> {
            new AddEmergencyTaskEntryDialog(frame, icon, mainBackgroundColor, JTabbedPaneBackgroundColor, buttonBackgroundColor, new AddEmergencyTaskEntryDialog.EmergencyTaskCallback() {
                @Override
                public void onEmergencyTaskCreated(EmergencyTask emergencytask) {
                    Convertor.EmergencyTaskConvertToJTextArea(panel2, emergencytask, singleEntryBackgroundColor);
                }
            });
        });
        panel2.add(button2);
        panel2.add(Box.createVerticalStrut(15));

        // Panel 3, for events
        JPanel panel3 = new JPanel();
        panel3.setLayout(new BoxLayout(panel3, BoxLayout.Y_AXIS));
        panel3.setBackground(JTabbedPaneBackgroundColor);
        panel3.add(Box.createVerticalStrut(15));

        // Label for Event pane
        JLabel title3 = new JLabel("Event", SwingConstants.CENTER);
        title3.setFont(labelFont);
        title3.setAlignmentX(Component.CENTER_ALIGNMENT);
        title3.setVisible(true);
        panel3.add(title3);
        panel3.add(Box.createVerticalStrut(15));

        // Add Event button
        JButton button3 = new JButton("Add Event");
        button3.setMaximumSize(modifyEntryButtonSize);
        button3.setAlignmentX(Component.CENTER_ALIGNMENT);
        button3.setBackground(buttonBackgroundColor);
        button3.setFont(new Font("Timeyourselflogo", Font.ITALIC, 24));
        button3.setBorder(globalBorder);
        button3.addActionListener(e -> {
            new AddEventEntryDialog(frame, icon, mainBackgroundColor, JTabbedPaneBackgroundColor, buttonBackgroundColor, new AddEventEntryDialog.EventCallback()  {
                @Override
                public void onEventCreated(Event event) {
                    Convertor.EventConvertToJTextArea(panel3, event, singleEntryBackgroundColor);
                }
            });
        });
        panel3.add(button3);
        panel3.add(Box.createVerticalStrut(15));

        tabbedPane.addTab("Task", panel1);
        tabbedPane.addTab("Emergency Task", panel2);
        tabbedPane.addTab("Event", panel3);

        contentPane.add(tabbedPane,  BorderLayout.CENTER);

        frame.setVisible(true);
    }
}
