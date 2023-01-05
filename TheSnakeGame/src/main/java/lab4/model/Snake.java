package lab4.model;

import lab4.protobuf.SnakesProto;

import java.util.LinkedList;

public class Snake {

    public SnakesProto.GamePlayer player;
    public int playerId;
    public SnakesProto.Direction direction;
    private final LinkedList<Point> coordinates = new LinkedList<>();
    private Point tail;
    private Point head;
    public boolean isDead = false;
    public boolean isZombie = false;
    public int score = 0;
    private int x;
    private int y;
    public SnakesProto.Direction lastDirection;

    public Snake(SnakesProto.GamePlayer player, Point head, SnakesProto.Direction direction, int x, int y){
        this.player = player;
        this.playerId = player.getId();
        this.score = player.getScore();
        this.head = head;
        this.direction = direction;
        this.x=x;
        this.y=y;
        this.coordinates.add(new Point(0,0).substract(new Point(direction), x, y));
        this.tail = head.substractabs(new Point(direction), x, y);
        this.lastDirection = direction;
    }

    public Snake(SnakesProto.GameState.Snake snake, SnakesProto.GamePlayer player, SnakesProto.GameConfig config) {
        this.player = player;
        this.playerId = snake.getPlayerId();
        if(player != null)
            this.score = 0;
        this.head = new Point(snake.getPoints(0));
        this.direction = snake.getHeadDirection();
        this.x=config.getWidth();
        this.y=config.getHeight();
        this.tail = this.head;
        for (int i = 1; i < snake.getPointsCount(); ++i){
            this.coordinates.add(new Point(snake.getPoints(i)));
            this.tail = this.tail.addabs(new Point(snake.getPoints(i)),x,y);
        }
        this.lastDirection = direction;
        isZombie = snake.getState() == SnakesProto.GameState.Snake.SnakeState.ZOMBIE || player == null;
        isDead = false;

    }

    public Point moveHead(){
        var curDirection = direction;
        lastDirection = curDirection;
        var lastHead = head;
        var speed = new Point(curDirection);
        var newHead = lastHead.addabs(speed, x, y);
        int dx = coordinates.getFirst().x;
        int dy = coordinates.getFirst().y;
        int newdx = newHead.x - lastHead.x;
        int newdy = newHead.y - lastHead.y;

        head = newHead;
        if ((dx == 0 && newdx == 0) || (dy==0 && newdy==0)){
            coordinates.set(0,coordinates.getFirst().substract(speed));
        }
        else{
            coordinates.addFirst(new Point(0,0).substract(speed, x, y));
        }
        return newHead;
    }

    public LinkedList<Point> getCoordinates() {
        return coordinates;
    }

    public Point moveTail() {
        Point last = coordinates.getLast();
        var speed = new Point (last.x == 0 ? 0 : -last.x / Math.abs(last.x), last.y == 0 ? 0 : -last.y / Math.abs(last.y));
        var newlast = last.add(speed,x,y);
        if (newlast.x ==0 && newlast.y ==0){
            coordinates.removeLast();
        }else{
            coordinates.removeLast();
            coordinates.addLast(newlast);
        }
        var lasttail = tail;
        tail = tail.addabs(speed,x,y);
        return lasttail;
    }

    public Point getHead() {
        return head;
    }

    public void exit(){
        this.player = null;
        this.isZombie = true;
    }

    public void setDirection(SnakesProto.Direction direction){
        if (new Point(lastDirection).add(new Point(direction)).equals(new Point(0, 0))){
            return;
        }
        this.direction = direction;
    }

}
