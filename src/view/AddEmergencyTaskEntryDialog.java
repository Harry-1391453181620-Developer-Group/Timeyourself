package view;

import models.EmergencyTask;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The dialog where users can add EmergencyTask
 */
public class AddEmergencyTaskEntryDialog {
    public interface EmergencyTaskCallback {
        void onEmergencyTaskCreated(EmergencyTask emergencyTask);
    }

    public AddEmergencyTaskEntryDialog(Window parentWindow, ImageIcon icon, Color mainBackgroundColor, Color JTableBackgroundColor, Color buttonBackgroundColor, view.AddEmergencyTaskEntryDialog.EmergencyTaskCallback callback) {
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
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                String startStr = startField.getText().trim();
                String dueStr = dueField.getText().trim();

                if (callback != null) {
                    callback.onEmergencyTaskCreated(createObject(name, desc, startStr, dueStr));
                }

                dialog.dispose();

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error, please try enter more information.", "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });

        contentPane.add(confirmButton, BorderLayout.SOUTH);

        dialog.setLocationRelativeTo(null);

        dialog.setVisible(true);
    }

    /**
     * Creates an EmergencyTask object based on the provided parameters, using all the provided constructors in EmergencyTask class.
     *
     * @param name     the name of the emergency task
     * @param desc     the description of the emergency task
     * @param startStr the start date string of the emergency task
     * @param dueStr   the due date string of the emergency task
     * @return the created EmergencyTask object
     */
    @Contract("_, _, _, _ -> new")
    public static @NotNull EmergencyTask createObject(@NotNull String name, String desc, String startStr, String dueStr) {
        if (name.isEmpty() && desc.isEmpty() && startStr.isEmpty() && dueStr.isEmpty()) {
            return new EmergencyTask();
        } else if (desc.isEmpty() && startStr.isEmpty() && dueStr.isEmpty()) {
            return new EmergencyTask(name);
        } else if (startStr.isEmpty() && dueStr.isEmpty()) {
            return new EmergencyTask(name, desc);
        } else if (desc.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime dueDate = LocalDateTime.parse(dueStr, formatter);
            LocalDateTime startDate = LocalDateTime.parse(startStr, formatter);
            return new EmergencyTask(name, startDate, dueDate);
        } else if (dueStr.isEmpty()) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime startDate = LocalDateTime.parse(startStr, formatter);
            return new EmergencyTask(name, desc, startDate);
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            LocalDateTime startDate = LocalDateTime.parse(startStr, formatter);
            LocalDateTime dueDate = LocalDateTime.parse(dueStr, formatter);
            return new EmergencyTask(name, desc, startDate, dueDate);
        }
    }
}