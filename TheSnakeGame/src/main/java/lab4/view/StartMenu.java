package lab4.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class StartMenu extends JPanel {

    private ClientView window;
    private JTextField usernameField;

    StartMenu(ClientView window){
        this.window = window;
        setPreferredSize(new Dimension(1370, 750));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JButton connectButton = new JButton("Connect");
        connectButton.setFont(new Font(Font.DIALOG, Font.BOLD, 50));
        connectButton.setFocusPainted(false);
        connectButton.setContentAreaFilled(false);
        connectButton.setBorder(null);
        connectButton.setMargin(new Insets(10,100,10,100));
        connectButton.setAlignmentX(CENTER_ALIGNMENT);

        JButton createButton = new JButton("Create");
        createButton.setFont(new Font(Font.DIALOG, Font.BOLD, 50));
        createButton.setFocusPainted(false);
        createButton.setContentAreaFilled(false);
        createButton.setBorder(null);
        createButton.setMargin(new Insets(10,100,10,100));
        createButton.setAlignmentX(CENTER_ALIGNMENT);

        JButton exitButton = new JButton("Exit");
        exitButton.setFont(new Font(Font.DIALOG, Font.BOLD, 50));
        exitButton.setFocusPainted(false);
        exitButton.setContentAreaFilled(false);
        exitButton.setBorder(null);
        exitButton.setMargin(new Insets(10,100,10,100));
        exitButton.setAlignmentX(CENTER_ALIGNMENT);
        exitButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });


        usernameField = new JTextField("Name");
        usernameField.setFont(new Font(Font.DIALOG, Font.PLAIN, 40));
        usernameField.setBackground(null);
        usernameField.setMaximumSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width, 50));
        usernameField.setHorizontalAlignment(JTextField.CENTER);
        usernameField.setBorder(null);

        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));

        usernameLabel.setAlignmentX(CENTER_ALIGNMENT);
        usernameField.setAlignmentX(CENTER_ALIGNMENT);

        connectButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.setConnectRoomView();
            }
        });
        createButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.setCreateRoomView();
            }
        });

        add(Box.createVerticalGlue());
        add(usernameLabel);
        add(usernameField);
        add(Box.createVerticalGlue());
        add(connectButton);
        add(Box.createVerticalStrut(100));
        add(createButton);
        add(Box.createVerticalStrut(100));
        add(exitButton);
        add(Box.createVerticalGlue());
        setFocusable(true);
        requestFocus();

    }

    public String getUsername(){
        return usernameField.getText();
    }

}
