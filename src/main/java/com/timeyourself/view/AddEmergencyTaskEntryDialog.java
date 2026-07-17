package com.timeyourself.view;

import com.timeyourself.config.GUIConfig;
import com.timeyourself.model.EmergencyTask;
import com.timeyourself.service.EmergencyTaskService;

import javax.swing.*;
import java.awt.*;
import java.time.format.DateTimeParseException;

/**
 * The dialog where users can add EmergencyTask
 */
public class AddEmergencyTaskEntryDialog {
    public interface EmergencyTaskCallback {
        void onEmergencyTaskCreated(EmergencyTask emergencyTask);
    }

    private final EmergencyTaskService emergencyTaskService;

    public AddEmergencyTaskEntryDialog(Window parentWindow, ImageIcon icon, Color mainBackgroundColor, Color JTableBackgroundColor, Color buttonBackgroundColor, com.timeyourself.view.AddEmergencyTaskEntryDialog.EmergencyTaskCallback callback, EmergencyTaskService emergencyTaskService) {
        this.emergencyTaskService = emergencyTaskService;
        // Initialize the dialog
        JDialog dialog = new JDialog(parentWindow, "Timeyourself - Add new emergency task");
        dialog.setSize(600, 800);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setBackground(mainBackgroundColor);
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);

        Container contentPane = dialog.getContentPane();
        contentPane.setBackground(mainBackgroundColor);
        contentPane.setLayout(new BorderLayout());

        // Icon label
        JLabel label = new JLabel(icon, SwingConstants.CENTER);
        label.setVisible(true);
        contentPane.add(label, BorderLayout.NORTH);

        // Detail entering panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(mainBackgroundColor);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 20, 10, 20);
        gbc.gridx = 0;

        gbc.gridy = 0; formPanel.add(new JLabel("Emergency task name: "), gbc);
        gbc.gridy = 1; JTextField nameField = new JTextField(20); formPanel.add(nameField, gbc);

        gbc.gridy = 2; formPanel.add(new JLabel("Emergency task description: "), gbc);
        gbc.gridy = 3; JTextField descField = new JTextField(20); formPanel.add(descField, gbc);

        gbc.gridy = 4; formPanel.add(new JLabel("Emergency task start date: (Format: dd/MM/yyyy HH:mm):"), gbc);
        gbc.gridy = 5; JTextField startField = new JTextField(20); formPanel.add(startField, gbc);

        gbc.gridy = 6; formPanel.add(new JLabel("Emergency task due date: (Format: dd/MM/yyyy HH:mm):"), gbc);
        gbc.gridy = 7; JTextField dueField = new JTextField(20); formPanel.add(dueField, gbc);

        contentPane.add(formPanel, BorderLayout.CENTER);

        // Add confirmation button
        JButton confirmButton = new JButton("Confirm adding");
        confirmButton.setBackground(buttonBackgroundColor);
        confirmButton.setFont(GUIConfig.labelFont);

        confirmButton.addActionListener(e -> {
            try {
                String name = nameField.getText(); // Trim in service.
                String desc = descField.getText();
                String startStr = startField.getText();
                String dueStr = dueField.getText();

                if (callback != null) {
                    callback.onEmergencyTaskCreated(emergencyTaskService.create(name, desc, startStr, dueStr));
                }

                dialog.dispose();

            } catch (DateTimeParseException ex) {
                JOptionPane.showMessageDialog(dialog, "Error, please try enter more information.", "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });

        contentPane.add(confirmButton, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(null);

        dialog.setVisible(true);
    }
}