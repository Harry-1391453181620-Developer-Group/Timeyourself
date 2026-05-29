package view;

import models.Event;
import models.Task;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The dialog where users can add Event
 */
public class AddEventEntryDialog {
    public interface EventCallback {
        void onEventCreated(Event event);
    }

    public AddEventEntryDialog(Window parentWindow, ImageIcon icon, Color mainBackgroundColor, Color JTableBackgroundColor, Color buttonBackgroundColor, AddEventEntryDialog.EventCallback callback) {
        // Initialize the dialog
        JDialog dialog = new JDialog(parentWindow, "Timeyourself - Add new event");
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

        gbc.gridy = 0; formPanel.add(new JLabel("Event name: "), gbc);
        gbc.gridy = 1; JTextField nameField = new JTextField(20); formPanel.add(nameField, gbc);

        gbc.gridy = 2; formPanel.add(new JLabel("Event description: "), gbc);
        gbc.gridy = 3; JTextField descField = new JTextField(20); formPanel.add(descField, gbc);

        gbc.gridy = 4; formPanel.add(new JLabel("Event date: (Format: dd/MM/yyyy HH:mm):"), gbc);
        gbc.gridy = 5; JTextField dateField = new JTextField(20); formPanel.add(dateField, gbc);

        gbc.gridy = 6; formPanel.add(new JLabel("Event location:"), gbc);
        gbc.gridy = 7; JTextField locationField = new JTextField(20); formPanel.add(locationField, gbc);

        contentPane.add(formPanel, BorderLayout.CENTER);

        // Add confirmation button
        JButton confirmButton = new JButton("Confirm adding");
        confirmButton.setBackground(buttonBackgroundColor);
        confirmButton.setFont(GUIConfig.labelFont);

        confirmButton.addActionListener(e -> {
            try {
                String name = nameField.getText().trim();
                String desc = descField.getText().trim();
                String dateStr = dateField.getText().trim();
                String locationStr = locationField.getText().trim();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                LocalDateTime date = LocalDateTime.parse(dateStr, formatter);

                Event newEvent = new Event(name, date, desc, locationStr);

                if (callback != null) {
                    callback.onEventCreated(newEvent);
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
