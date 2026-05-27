package view;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class GUIConfig {
    public static Dimension getSingleEntrySize(JTextArea textArea) {
        if (textArea == null) {
            return new Dimension(Integer.MAX_VALUE, 40);
        }
        int autoHeight = textArea.getPreferredSize().height;
        return new Dimension(Integer.MAX_VALUE, autoHeight);
    }

    public static final ImageIcon icon = new ImageIcon("./Timeyourselflogo.png");
    public static final Color mainBackgroundColor = new Color(144, 224, 239);
    public static final Color JTabbedPaneBackgroundColor = new Color(72, 202, 228);
    public static final Color singleEntryBackgroundColor = new Color(0, 180, 216);
    public static final Color buttonBackgroundColor = new Color(0, 150, 199);
    public static final Dimension modifyEntryButtonSize = new Dimension(500, 50);
    public static final Border globalBorder = BorderFactory.createLineBorder(Color.BLACK, 2);
    public static final Font labelFont = new Font("SansSerif", Font.BOLD, 24);
}
