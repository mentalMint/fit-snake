package lab4.view;

import lab4.controller.KeyboardController;
import lab4.protobuf.SnakesProto;
import lab4.model.PlayerView;
import lab4.controller.NodeController;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Vector;

public class GameField extends JPanel {
    JLabel scoreText;
    JPanel viewPanel;
    GameView window;
    private final BufferedImage image;
    private final Graphics2D g;
    private int windowWidth;
    private int windowHeight;
    KeyboardController keyHandler;
    public int width, height;
    private final int dx;
    private final int dy;
    private final int tlx;
    private final int tly;
    SnakesProto.GameConfig config;
    JTable leaderboard;

    GameField(GameView window, SnakesProto.GameConfig config){
        super();
        this.config = config;
        this.width = config.getWidth();
        this.height = config.getHeight();
        this.window = window;
        this.windowWidth = 1370;
        this.windowHeight = 750;
        int rawdy = windowHeight/height;
        int rawdx = windowHeight/width;
        if (rawdx > rawdy){
            dx = rawdy;
            dy = rawdy;
        }
        else if (rawdy > rawdx){
            dx = rawdx;
            dy = rawdx;
        }
        else {
            dx = rawdx; dy = rawdy;
        }
        tlx = (windowHeight - dx*width)/2;
        tly = (windowHeight - dy*height)/2;
        setPreferredSize(new Dimension(windowWidth,windowHeight));
        this.setLayout(new BorderLayout());
        image = new BufferedImage(windowHeight, windowHeight, BufferedImage.TYPE_INT_RGB);
        g = (Graphics2D) image.getGraphics();

        JPanel leftPanel = new JPanel();
        leftPanel.setPreferredSize(new Dimension(windowWidth-windowHeight,windowHeight));
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        scoreText = new JLabel();
        scoreText.setFont(new Font(Font.DIALOG, Font.PLAIN, 50));
        scoreText.setMaximumSize(new Dimension(windowWidth-windowHeight,100));
        scoreText.setAlignmentX(CENTER_ALIGNMENT);

        JButton leaveButton = new JButton("Leave");
        leaveButton.setFont(new Font(Font.DIALOG, Font.BOLD, 50));
        leaveButton.setFocusPainted(false);
        leaveButton.setContentAreaFilled(false);
        leaveButton.setBorderPainted(false);
        leaveButton.setAlignmentX(CENTER_ALIGNMENT);
        leaveButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.leave();
                requestFocus();
            }
        });

        JButton exitButton = new JButton("Exit");
        exitButton.setFont(new Font(Font.DIALOG, Font.BOLD, 50));
        exitButton.setFocusPainted(false);
        exitButton.setContentAreaFilled(false);
        exitButton.setBorderPainted(false);
        exitButton.setAlignmentX(CENTER_ALIGNMENT);
        exitButton.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                window.finalUpdate();
            }
        });

        leaderboard = new JTable(0,2);
        leaderboard.setShowGrid(false);
        leaderboard.setMaximumSize(new Dimension(windowHeight/2,windowHeight/2));
        leaderboard.setFont(new Font("Roboto",Font.BOLD,30));
        leaderboard.setRowHeight(40);

        leftPanel.add(scoreText);
        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(leaderboard);
        leftPanel.add(Box.createVerticalGlue());
        leftPanel.add(leaveButton);
        leftPanel.add(Box.createVerticalStrut(100));
        leftPanel.add(exitButton);

        viewPanel = new JPanel();
        viewPanel.setPreferredSize(new Dimension(windowHeight,windowHeight));

        this.add(leftPanel, BorderLayout.WEST);
        this.add(viewPanel, BorderLayout.EAST);
        revalidate();
        setFocusable(true);
        requestFocus();

    }

    public void gameRender(PlayerView view){

        g.setColor(new Color(127, 201, 135));
        g.fillRect(0, 0, windowHeight, windowHeight);
        g.setColor(new Color(208, 236, 194));
        g.fillRect(tlx, tly, dx*width, dy*height);

        Vector<Vector<String>> data = new Vector<>();
        for(int i = 0; i< view.state.getPlayers().getPlayersCount(); ++i){
            var player = view.state.getPlayers().getPlayers(i);
            if (player.getId() == view.id)
                scoreText.setText(" Score: " + player.getScore());
            Vector<String> record = new Vector<>(2);
            record.add(0,player.getName());
            record.add(1, String.valueOf(player.getScore()));
            if (data.size() == 0)
            data.add(0,record);
            else {
                var firstitem = data.get(0);
                int maxscore = Integer.parseInt(firstitem.get(1));
                int place = 0;
                for (var max : data) {
                    if (Integer.parseInt(max.get(1)) <= maxscore)
                        place++;
                    else
                        break;
                }
                data.insertElementAt(record, place);
            }
        }
        leaderboard.setModel(new DefaultTableModel(data, new Vector<>(Arrays.asList("Name", "Score"))));
        leaderboard.getColumnModel().getColumn(1).setMaxWidth(100);


        int sx=0, sy=0;
        for(int i = 0; i< view.state.getSnakesCount(); ++i) {
            var mysnake = view.state.getSnakes(i);
            if (mysnake.getPlayerId() == view.id){
                var myhead = mysnake.getPoints(0);
                sx = width/2 - myhead.getX();
                sy = height/2 - myhead.getY();
                break;
            }
        }
        for(int i = 0; i< view.state.getSnakesCount(); ++i){
            var snake = view.state.getSnakes(i);
            if (snake.getPointsCount() < 2) continue;
            var last = snake.getPoints(0);
            if (snake.getPlayerId() == view.id)
                g.setColor(new Color(255, 103, 40, 255));
            else
                g.setColor(new Color(246, 224, 96, 255));
            if (snake.getState() == SnakesProto.GameState.Snake.SnakeState.ZOMBIE){
                g.setColor(new Color(22, 84, 109, 255));
            }
            g.fillRect(tlx + (((last.getX()+sx) % width + width) % width)*dx,
                    tly + (((last.getY()+sy)%height + height) % height)*dy,
                    dx, dy);
            g.setColor(new Color(66, 173, 61, 255));
            for (int j = 1; j<snake.getPointsCount(); ++j){
                var item = snake.getPoints(j);
                if (item.getX() != 0){
                    for (int x = item.getX(); x!=0; x += (item.getX()>0)?-1:1){
                        g.fillRect(tlx + (((last.getX() + x + sx + width) % width + width) % width) * dx,
                                tly + (((last.getY()+sy)%height + height) % height) * dy,
                                dx, dy);
                    }
                }
                else if (item.getY() != 0){
                    for (int y = item.getY(); y!=0; y += (item.getY()>0)?-1:1){
                        g.fillRect(tlx + (((last.getX()+sx) % width + width) % width) * dx,
                                tly + (((last.getY() + y + sy + height) % height + height) % height) * dy,
                                dx, dy);
                    }
                }
                last = SnakesProto.GameState.Coord.newBuilder()
                        .setX((last.getX() + item.getX() + width) % width)
                        .setY((last.getY() + item.getY() + height) % height)
                        .build();
            }
        }

        g.setColor(new Color(231, 28, 56, 255));
        var foodsCount = view.state.getFoodsCount();
        for (int i = 0; i< foodsCount;++i){
            var point = view.state.getFoods(i);
            //g.setColor(new Color(random.nextInt(240), random.nextInt(240), random.nextInt(240), 255));
            g.fillOval(tlx + dx/4 + (((point.getX()+sx) % width + width) % width)*dx,
                    tly + dy/4 + (((point.getY()+sy)%height + height) % height)*dy,
                    dx,dy);
        }

    }

    public void gameDraw(){
        Graphics g2 = viewPanel.getGraphics();
        g2.drawImage(image,0,0,null);
        g2.dispose();
    }

    public void setHandler(NodeController controller) {
        keyHandler = new KeyboardController(controller);
        this.addKeyListener(keyHandler);
    }

}
