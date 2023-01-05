package lab4.view;

import lab4.protobuf.SnakesProto;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class CreatePanel extends JPanel {

    CreatePanel(ClientView window){
        setPreferredSize(new Dimension(1370, 750));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JButton createButton = new JButton("Create");
        createButton.setFont(new Font(Font.DIALOG, Font.BOLD, 50));
        createButton.setFocusPainted(false);
        createButton.setContentAreaFilled(false);
        createButton.setBorder(null);
        createButton.setMargin(new Insets(10,100,10,100));
        createButton.setAlignmentX(CENTER_ALIGNMENT);


        JButton backButton = new JButton("Back");
        backButton.setFont(new Font(Font.DIALOG, Font.BOLD, 50));
        backButton.setFocusPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setBorder(null);
        backButton.setMargin(new Insets(10,100,10,100));
        backButton.setAlignmentX(CENTER_ALIGNMENT);

        JTextField fieldWidthField = new JTextField("40");
        fieldWidthField.setFont(new Font(Font.DIALOG, Font.PLAIN, 40));
        fieldWidthField.setBackground(null);
        fieldWidthField.setMaximumSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width, 50));
        fieldWidthField.setHorizontalAlignment(JTextField.CENTER);
        fieldWidthField.setBorder(null);

        JTextField fieldHeightField = new JTextField("40");
        fieldHeightField.setFont(new Font(Font.DIALOG, Font.PLAIN, 40));
        fieldHeightField.setBackground(null);
        fieldHeightField.setMaximumSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width, 50));
        fieldHeightField.setHorizontalAlignment(JTextField.CENTER);
        fieldHeightField.setBorder(null);

        JTextField foodStaticField = new JTextField("1");
        foodStaticField.setFont(new Font(Font.DIALOG, Font.PLAIN, 40));
        foodStaticField.setBackground(null);
        foodStaticField.setMaximumSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width, 50));
        foodStaticField.setHorizontalAlignment(JTextField.CENTER);
        foodStaticField.setBorder(null);

        JTextField stateDelayField = new JTextField("1000");
        stateDelayField.setFont(new Font(Font.DIALOG, Font.PLAIN, 40));
        stateDelayField.setBackground(null);
        stateDelayField.setMaximumSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width,50));
        stateDelayField.setHorizontalAlignment(JTextField.CENTER);
        stateDelayField.setBorder(null);

        JLabel foodStaticLabel = new JLabel("FoodStatic:");
        foodStaticLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));
        JLabel stateDelayLabel = new JLabel("StateDelay:");
        stateDelayLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));
        JLabel fieldWidthLabel = new JLabel("FieldWidth:");
        fieldWidthLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));
        JLabel fieldHeightLabel = new JLabel("FieldHeight:");
        fieldHeightLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));


        foodStaticField.setAlignmentX(CENTER_ALIGNMENT);
        stateDelayField.setAlignmentX(CENTER_ALIGNMENT);
        foodStaticLabel.setAlignmentX(CENTER_ALIGNMENT);
        stateDelayLabel.setAlignmentX(CENTER_ALIGNMENT);

        fieldHeightLabel.setAlignmentX(CENTER_ALIGNMENT);
        fieldHeightField.setAlignmentX(CENTER_ALIGNMENT);

        fieldWidthLabel.setAlignmentX(CENTER_ALIGNMENT);
        fieldWidthField.setAlignmentX(CENTER_ALIGNMENT);

        backButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.setMenuView();
            }
        });

        createButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.setGameView(SnakesProto.GameConfig.newBuilder()
                        .setWidth(Integer.parseInt(fieldWidthField.getText()))
                        .setHeight(Integer.parseInt(fieldHeightField.getText()))
                        .setFoodStatic(Integer.parseInt(foodStaticField.getText()))
                        .setStateDelayMs(Integer.parseInt(stateDelayField.getText()))
                        .build(),
                        "My game1"
                );
            }
        });


        add(Box.createVerticalGlue());
        add(fieldWidthLabel);
        add(fieldWidthField);
        add(Box.createVerticalGlue());
        add(fieldHeightLabel);
        add(fieldHeightField);
        add(Box.createVerticalGlue());
        add(foodStaticLabel);
        add(foodStaticField);
        add(Box.createVerticalGlue());
        add(stateDelayLabel);
        add(stateDelayField);
        add(Box.createVerticalGlue());
        add(createButton);
        add(Box.createVerticalGlue());
        add(backButton);
        add(Box.createVerticalGlue());
        setFocusable(true);
        requestFocus();

        stateDelayLabel.requestFocus();

    }

}
