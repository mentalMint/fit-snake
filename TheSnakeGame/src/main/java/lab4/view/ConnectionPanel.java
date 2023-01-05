package lab4.view;

import lab4.controller.Observer;
import lab4.protobuf.SnakesProto;
import lab4.controller.GameFinder;
import lab4.controller.HostInfo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;
import java.util.Vector;

public class ConnectionPanel extends JPanel implements Observer {

    private final JList list;
    ArrayList<Map.Entry<HostInfo, SnakesProto.GameAnnouncement>> listdata= new ArrayList<>();
    GameFinder finder;
    ConnectionPanel(ClientView window){
        finder=new GameFinder();
        finder.register(this);
        setPreferredSize(new Dimension(1370, 750));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        JButton joinButton = new JButton("Explore");
        joinButton.setFont(new Font(Font.DIALOG, Font.BOLD, 50));
        joinButton.setFocusPainted(false);
        joinButton.setContentAreaFilled(false);
        joinButton.setBorder(null);
        joinButton.setMargin(new Insets(10,100,10,100));
        joinButton.setAlignmentX(CENTER_ALIGNMENT);


        JButton backButton = new JButton("Back");
        backButton.setFont(new Font(Font.DIALOG, Font.BOLD, 50));
        backButton.setFocusPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setBorder(null);
        backButton.setMargin(new Insets(10,100,10,100));
        backButton.setAlignmentX(CENTER_ALIGNMENT);

        JTextField hostField = new JTextField("snakes.ippolitov.me");
        hostField.setFont(new Font(Font.DIALOG, Font.PLAIN, 50));
        hostField.setBackground(null);
        hostField.setMaximumSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width, 50));
        hostField.setHorizontalAlignment(JTextField.CENTER);
        hostField.setBorder(null);

        JTextField portField = new JTextField("9192");
        portField.setFont(new Font(Font.DIALOG, Font.PLAIN, 50));
        portField.setBackground(null);
        portField.setMaximumSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width,50));
        portField.setHorizontalAlignment(JTextField.CENTER);
        portField.setBorder(null);

        JLabel hostLabel = new JLabel("Server host:");
        hostLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));
        JLabel portLabel = new JLabel("Server port:");
        portLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 30));


        hostField.setAlignmentX(CENTER_ALIGNMENT);
        portField.setAlignmentX(CENTER_ALIGNMENT);
        hostLabel.setAlignmentX(CENTER_ALIGNMENT);
        portLabel.setAlignmentX(CENTER_ALIGNMENT);
        backButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.setMenuView();
            }
        });

        joinButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                finder.sendDiscover(hostField.getText(), portField.getText());
                //window.setGameView(new SnakesProto.GameConfig());
            }
        });
        //list = new JList<>(new Vector<>());
        DefaultListModel listmodel=new DefaultListModel();
        list = new JList<>(listmodel);
        JScrollPane scrollPane = new JScrollPane(list);

        add(Box.createVerticalGlue());
        add(hostLabel);
        add(hostField);
        add(Box.createVerticalGlue());
        add(portLabel);
        add(portField);
        add(Box.createVerticalGlue());
        add(joinButton);
        add(Box.createVerticalGlue());
        add(scrollPane);
        add(Box.createVerticalGlue());
        add(backButton);
        add(Box.createVerticalGlue());
        setFocusable(true);
        requestFocus();

        hostField.requestFocus();
        finder.start();
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    int selected = list.locationToIndex(e.getPoint());
                    window.setGameView(listdata.get(selected).getKey(), listdata.get(selected).getValue(), SnakesProto.NodeRole.NORMAL);
                }
                else if (e.getButton() == MouseEvent.BUTTON2){
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        int selected = list.locationToIndex(e.getPoint());
                        window.setGameView(listdata.get(selected).getKey(), listdata.get(selected).getValue(), SnakesProto.NodeRole.VIEWER);
                    }
                }
            }
        });

    }

    @Override
    public void update() {

        DefaultListModel<String> model = (DefaultListModel<String>) list.getModel();
        model.removeAllElements();
        Vector<String> newdata = new Vector<>();
        listdata.clear();
        for(var games:finder.games.entrySet()){
            for (int i = 0; i<games.getValue().getGamesCount();++i){
                var game = games.getValue().getGames(i);
                listdata.add(new AbstractMap.SimpleEntry<>(games.getKey(),game));
                newdata.add(game.getGameName() + "(" + games.getKey().ip + ":" + games.getKey().port + ") - " + game.getPlayers().getPlayersCount() + " players");
            }
        }
        model.addAll(newdata);
    }

    @Override
    public void finalUpdate() {

    }
}
