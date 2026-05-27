package view;

import models.EmergencyTask;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AddEmergencyTaskEntryDialog {
    public interface EmergencyTaskCallback {
        void onEmergencyTaskCreated(EmergencyTask emergencyTask);
    }

    public AddEmergencyTaskEntryDialog(Window parentWindow, ImageIcon icon, Color mainBackgroundColor, Color JTableBackgroundColor, Color buttonBackgroundColor, view.AddEmergencyTaskEntryDialog.EmergencyTaskCallback callback) {
        JDialog dialog = new JDialog(parentWindow, "Timeyourself - Add new emergency task");
        dialog.setSize(600, 800);
        dialog.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        dialog.setBackground(mainBackgroundColor);
        dialog.setLocationRelativeTo(null);
        dialog.setModal(true);

        Container contentPane = dialog.getContentPane();
        contentPane.setBackground(mainBackgroundColor);
        contentPane.setLayout(new BorderLayout());

        JLabel label = new JLabel(icon, SwingConstants.CENTER);
        label.setVisible(true);
        contentPane.add(label, BorderLayout.NORTH);

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

        JButton confirmButton = new JButton("Confirm adding");
        confirmButton.setBackground(buttonBackgroundColor);
        confirmButton.setFont(GUIConfig.labelFont);

        confirmButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                String startStr = startField.getText().trim();
                String dueStr = dueField.getText().trim();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                LocalDateTime dueDate = LocalDateTime.parse(dueStr, formatter);
                LocalDateTime startDate = LocalDateTime.parse(startStr, formatter);

                EmergencyTask newEmergencyTask = new EmergencyTask(name, desc, dueDate, startDate);

                if (callback != null) {
                    callback.onEmergencyTaskCreated(newEmergencyTask);
                }

                dialog.dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Date format error! Please check! \nExample: 27/05/2026 18:00", "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });

        contentPane.add(confirmButton, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(null);

        dialog.setVisible(true);
    }
}