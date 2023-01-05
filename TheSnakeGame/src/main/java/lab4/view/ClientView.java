package lab4.view;

import lab4.protobuf.SnakesProto;
import lab4.controller.HostInfo;
import lab4.controller.NodeController;

import javax.swing.*;
import java.awt.*;

public class ClientView extends JFrame {
    private StartMenu menuPanel;
    private ConnectionPanel connectRoomPanel;
    private CreatePanel createRoomPanel;

    public ClientView(){
        super("The Snake Game");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setLayout(new BorderLayout());
        menuPanel = new StartMenu(this);
        connectRoomPanel = new ConnectionPanel(this);
        createRoomPanel = new CreatePanel(this);
        setMenuView();
        setVisible(true);
    }

    public void setGameView(SnakesProto.GameConfig config, String gameName){

        GameView game = new GameView(this, new NodeController(config, gameName, menuPanel.getUsername() ));
        this.setVisible(false);

    }


    public void setGameView(HostInfo address, SnakesProto.GameAnnouncement gameAnnouncement, SnakesProto.NodeRole role) {
        GameView game = new GameView(this, new NodeController(address, gameAnnouncement, menuPanel.getUsername(), role));
        this.setVisible(false);
    }

    public void setMenuView(){
        this.setContentPane(menuPanel);
        menuPanel.revalidate();
        pack();
        setVisible(true);
    }

    public void setConnectRoomView(){
        this.setContentPane(connectRoomPanel);
        connectRoomPanel.revalidate();
        pack();
        setVisible(true);
    }

    public void setCreateRoomView(){
        this.setContentPane(createRoomPanel);
        createRoomPanel.revalidate();
        pack();
        setVisible(true);
    }


}
