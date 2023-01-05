package lab4.view;

import lab4.controller.Observer;
import lab4.protobuf.SnakesProto;
import lab4.model.PlayerView;
import lab4.controller.NodeController;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GameView implements Observer {

    JFrame window;
    GameField playPanel;

    ClientView mainApplication;
    NodeController controller;
    PlayerView view;
    boolean playing = false;

    public GameView(ClientView mainApplication, NodeController controller){
        this.mainApplication = mainApplication;
        this.controller = controller;
        window = new JFrame("The Snake Game: " + controller.getGameName());
        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                finalUpdate();
            }
        });
        window.setExtendedState(JFrame.MAXIMIZED_BOTH);
        window.setUndecorated(true);
        playPanel = new GameField(this, controller.getConfig());
        window.setContentPane(playPanel);
        window.pack();
        playPanel.setHandler(controller);
        window.setVisible(true);
        controller.register(this);
        controller.start();

    }

    @Override
    public void finalUpdate() {
        controller.stop();
        if (playing) {
            mainApplication.setMenuView();
        }
        else{
            mainApplication.setConnectRoomView();
        }
        this.window.dispose();
        controller.terminate();
        controller = null;
    }

    @Override
    public synchronized void update(){
        if(!playing && controller.role != null) playing = true;
        if (!playing) return;
        view = controller.getGameState();
        playPanel.gameRender(view);
        playPanel.gameDraw();
    }


    public void leave() {
        controller.changeRole(SnakesProto.NodeRole.VIEWER);
    }
}
