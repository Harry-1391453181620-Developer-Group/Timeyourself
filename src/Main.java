import javax.swing.*;
import java.awt.*;

public class Main extends JFrame {
    public void newFrame() {
        JFrame frame = new JFrame("New GUI");
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new FlowLayout());
        Container contentPane = frame.getContentPane();
        JLabel label = new JLabel("Welcome to TimeYourSelf!", SwingConstants.CENTER);
    }

    static void main() {
        Main gui = new Main();
        gui.newFrame();
        gui.setVisible(true);
    }
}

