package lab4.controller;

import lab4.protobuf.SnakesProto;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class KeyboardController implements KeyListener {

    NodeController controller;
    public KeyboardController(NodeController controller){
        this.controller = controller;
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DOWN){
            controller.changeDirection(SnakesProto.Direction.DOWN);
        }
        else if (e.getKeyCode() == KeyEvent.VK_UP){
            controller.changeDirection(SnakesProto.Direction.UP);
        }
        else if (e.getKeyCode() == KeyEvent.VK_LEFT){
            controller.changeDirection(SnakesProto.Direction.LEFT);
        }
        else if (e.getKeyCode() == KeyEvent.VK_RIGHT){
            controller.changeDirection(SnakesProto.Direction.RIGHT);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
}
