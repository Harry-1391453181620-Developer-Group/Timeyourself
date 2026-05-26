package view;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

import controller.Convertor;
import controller.Convertor.*;

public class MainFrame {
    public MainFrame() {
        ImageIcon icon = new ImageIcon("./Timeyourselflogo.png");
        Color mainBackgroundColor = new Color(144, 224, 239);
        Color JTabbedPaneBackgroundColor = new Color(72, 202, 228);
        Color singleEntryBackgroundColor = new Color(0, 180, 216);
        Dimension singleEntrySize = new Dimension(Integer.MAX_VALUE, 40);

        JFrame frame = new JFrame("Timeyourself");
        frame.setSize(1000, 1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setBackground(mainBackgroundColor);
        frame.setLocationRelativeTo(null);

        Container contentPane = frame.getContentPane();
        contentPane.setBackground(mainBackgroundColor);
        contentPane.setLayout(new BorderLayout());

        JLabel label = new JLabel(icon, SwingConstants.CENTER);
        label.setVisible(true);
        contentPane.add(label, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane(SwingConstants.TOP);
        tabbedPane.setBackground(JTabbedPaneBackgroundColor);

        // Panel 1, for tasks
        JPanel panel1 = new JPanel();
        panel1.setLayout(new BoxLayout(panel1, BoxLayout.Y_AXIS));
        panel1.setBackground(JTabbedPaneBackgroundColor);
        panel1.add(Box.createVerticalStrut(15));

        JLabel title = new JLabel("Tasks", SwingConstants.CENTER);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setVisible(true);
        panel1.add(title);
        panel1.add(Box.createVerticalStrut(15));

        Convertor.TaskConvertToJTextArea(panel1, new models.Task("Task 1"), singleEntryBackgroundColor, singleEntrySize);
        Convertor.TaskConvertToJTextArea(panel1, new models.Task("Task 2"), singleEntryBackgroundColor, singleEntrySize);

        // Panel 2, for emergency tasks
        JPanel panel2 = new JPanel();
        panel2.setLayout(new BoxLayout(panel2, BoxLayout.Y_AXIS));
        panel2.setBackground(JTabbedPaneBackgroundColor);
        panel2.add(Box.createVerticalStrut(15));

        JLabel title2 = new JLabel("Emergency Tasks", SwingConstants.CENTER);
        title2.setAlignmentX(Component.CENTER_ALIGNMENT);
        title2.setVisible(true);
        panel2.add(title2);
        panel2.add(Box.createVerticalStrut(15));

        // Panel 3, for events
        JPanel panel3 = new JPanel();
        panel3.setLayout(new BoxLayout(panel3, BoxLayout.Y_AXIS));
        panel3.setBackground(JTabbedPaneBackgroundColor);
        panel3.add(Box.createVerticalStrut(15));

        JLabel title3 = new JLabel("Events", SwingConstants.CENTER);
        title3.setAlignmentX(Component.CENTER_ALIGNMENT);
        title3.setVisible(true);
        panel3.add(title3);
        panel3.add(Box.createVerticalStrut(15));

        tabbedPane.addTab("Task", panel1);
        tabbedPane.addTab("Emergency Task", panel2);
        tabbedPane.addTab("Event", panel3);

        contentPane.add(tabbedPane,  BorderLayout.CENTER);

        frame.setVisible(true);
    }
}
