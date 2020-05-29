import javax.swing.*;
import java.awt.*;

public class AuthPanel extends JPanel {
    private JTextField practionerId = new JTextField(50);
    private JButton authButton = new JButton("Sign in");
    public AuthPanel() {
        setLayout(new BorderLayout());

        JPanel panel = new JPanel(new GridLayout(1, 2));
        panel.add(new JLabel("Practitioner ID"));
        panel.add(practionerId);

        add(panel, BorderLayout.CENTER);
        add(authButton, BorderLayout.SOUTH);
    }
}
