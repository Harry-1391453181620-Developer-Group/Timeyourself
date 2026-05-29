package view;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;


/**
 * All the settings for Timeyourself's UI.
 */
public class GUIConfig {

    /**
     * Generates a preferred dimension using .getPreferredSize().
     *
     * @param textArea the target JTextArea
     * @return a Dimension object with the width set to Integer.MAX_VALUE and the height set to the preferred height of the JTextArea
     */
    @Contract("null -> new")
    public static @NotNull Dimension getSingleEntrySize(JTextArea textArea) {
        if (textArea == null) {
            return new Dimension(Integer.MAX_VALUE, 40);
        }
        int autoHeight = textArea.getPreferredSize().height;
        return new Dimension(Integer.MAX_VALUE, autoHeight);
    }

    /** The icon of Timeyourself, used across all windows. */
    public static final ImageIcon icon = new ImageIcon("./Timeyourselflogo.png");
    /** The color of MainFrame and the AddEntryDialogs. */
    public static final Color mainBackgroundColor = new Color(144, 224, 239);
    /** The color of tabbedPane. */
    public static final Color JTabbedPaneBackgroundColor = new Color(72, 202, 228);
    /** The color of each entry. */
    public static final Color singleEntryBackgroundColor = new Color(0, 180, 216);
    /** The color of all buttons. */
    public static final Color buttonBackgroundColor = new Color(0, 150, 199);
    /** The color for each checkBox on each entry. */
    public static final Color globalCheckBoxColor = new Color(0, 119, 182);
    /** The size for the entry modification buttons and the codification confirm buttons. */
    public static final Dimension modifyEntryButtonSize = new Dimension(500, 50);
    /** The border used for any UI component. */
    public static final Border globalBorder = BorderFactory.createLineBorder(Color.BLACK, 2);
    /** The font for every JLabel */
    public static final Font labelFont = new Font("SansSerif", Font.BOLD, 24);
    /** The font for every JCheckBox */
    public static final Font globalCheckBoxFont = new Font("SansSerif", Font.BOLD, 18);
}
