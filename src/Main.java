import com.formdev.flatlaf.FlatLightLaf;
import view.MainFrame;

import javax.swing.*;
import java.awt.*;

import static view.GUIConfig.JTabbedPaneBackgroundColor;
import static view.GUIConfig.buttonBackgroundColor;

/**
 * The Main class serves as the entry point for the Timeyourself application. It initializes the main GUI frame.
 * Note that it uses the latest JDK26 syntax, which may not be compatible for older JDKs.
 */
public class Main extends JFrame {
    static void main() {
        // Preserve the original color
        UIManager.put("Button.background", buttonBackgroundColor);
        UIManager.put("Button.foreground", Color.BLACK);
        UIManager.put("Button.hoverBackground", buttonBackgroundColor);
        UIManager.put("Button.pressedBackground", buttonBackgroundColor);
        UIManager.put("Button.focusedBackground", buttonBackgroundColor);
        UIManager.put("Button.disabledBackground", buttonBackgroundColor);
        UIManager.put("Button.selectedBackground", buttonBackgroundColor);
        UIManager.put("Button.hoverOverlayColor", new Color(0, 0, 0, 0));
        UIManager.put("Panel.background", JTabbedPaneBackgroundColor);
        UIManager.put("TextField.background", Color.WHITE);

        // Set language
        UIManager.put("OptionPane.okButtonText", "OK");
        UIManager.put("OptionPane.yesButtonText", "Yes");
        UIManager.put("OptionPane.noButtonText", "No");
        UIManager.put("OptionPane.cancelButtonText", "Cancel");

        // Use FlatLaf to add arc.
        UIManager.put("Component.arc", 12);
        UIManager.put("Button.arc", 12);
        UIManager.put("TextComponent.arc", 12);
        FlatLightLaf.setup();

        // Start the mainframe.
        MainFrame gui = new MainFrame();
    }
}

