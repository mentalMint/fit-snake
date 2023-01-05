package lab4.model;

import lab4.protobuf.SnakesProto;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class GameModel {

    private final int width;
    private final int height;
    private int foodStatic;
    private int stateorder = 0;
    private SnakesProto.GameConfig gameConfig;
    private Random rand = new Random(System.currentTimeMillis());

    public LinkedList<Point> food = new LinkedList<>();

    public ConcurrentHashMap<Integer, Snake> snakes = new ConcurrentHashMap<>();


    public GameModel(SnakesProto.GameConfig gameConfig) {
        this.gameConfig = gameConfig;
        height = gameConfig.getHeight();
        width = gameConfig.getWidth();
        foodStatic = gameConfig.getFoodStatic();
        foodmap = new boolean[width][height];
        fieldmap = new int[width][height];
        generateFood();
    }

    public GameModel(SnakesProto.GameConfig config, SnakesProto.GameState gameState, int master, int deputy) {
        this.gameConfig = config;
        height = gameConfig.getHeight();
        width = gameConfig.getWidth();
        foodStatic = gameConfig.getFoodStatic();
        foodmap = new boolean[width][height];
        fieldmap = new int[width][height];
        for (var f: gameState.getFoodsList()){
            foodmap[f.getX()][f.getY()] = true;
            food.add(new Point(f.getX(),f.getY()));
        }
        for (var snake: gameState.getSnakesList()){
            if (snake.getPointsCount() < 2) continue;
            SnakesProto.GamePlayer snakeplayer = null;
            for(var player : gameState.getPlayers().getPlayersList()){
                if (player.getId() == snake.getPlayerId()) {
                    if (player.getId() == master) break;
                    else if (player.getId() == deputy){
                        snakeplayer = player.toBuilder().clearIpAddress().clearPort().setRole(SnakesProto.NodeRole.MASTER).build();
                    }
                    else{
                        snakeplayer = player;
                    }
                    break;
                }
            }
            snakes.put(snake.getPlayerId(), new Snake(snake, snakeplayer, config));
            var last = snake.getPoints(0);
            for (int j = 1; j<snake.getPointsCount(); ++j){
                var item = snake.getPoints(j);
                if (item.getX() != 0){
                    for (int x = item.getX(); x!=0; x += (item.getX()>0)?-1:1){
                        fieldmap[(last.getX() + x + width) % width][(last.getY() + height) % height] +=1;
                    }
                }
                else if (item.getY() != 0){
                    for (int y = item.getY(); y!=0; y += (item.getY()>0)?-1:1){
                        fieldmap[(last.getX() + width) % width][(last.getY() + y + height) % height] +=1;
                    }
                }
                last = SnakesProto.GameState.Coord.newBuilder()
                        .setX((last.getX() + item.getX() + width) % width)
                        .setY((last.getY() + item.getY() + height) % height)
                        .build();
            }
        }
        int maxid = 0;
        for (var player : gameState.getPlayers().getPlayersList()){
            if (player.getId() > maxid) maxid = player.getId();
        }
        lastId = maxid;
        stateorder = gameState.getStateOrder();

    }

    private void generateFood(){
        int foodtogenerate = foodStatic + snakes.size() - food.size();
        foodgen: for (int k = 0; k < foodtogenerate; ++k){
            int x = rand.nextInt(width);
            int y = rand.nextInt(height);
            for (int i=0; i<width; ++i) {
                for (int j=0; j<height; ++j) {
                    if (fieldmap[(x+i) % width][(y+j) % height] == 0 && !foodmap[(x+i) % width][(y+j) % height]) {
                        food.add(new Point((x+i) % width, (y+j) % height));
                        foodmap[(x+i) % width][(y+j) % height] = true;
                        continue foodgen;
                    }
                }
            }

        }
    }


    private int lastId = 0;
    private int newPlayerId() {
        return ++lastId;
    }

    public boolean addPlayer(SnakesProto.GamePlayer newPlayer){

        int x = rand.nextInt(width);
        int y = rand.nextInt(height);
        synchronized (this) {
            findplacex:
            for (int i = 0; i < width; ++i) {
                findplacey:
                for (int j = 0; j < height; ++j) {
                    int centerx = (x + i) % width;
                    int centery = (y + j) % height;
                    if (!foodmap[centerx][centery]) {

                        for (int dx = -2; dx <= 2; ++dx) {
                            for (int dy = -2; dy <= 2; ++dy) {
                                if (fieldmap[(centerx + dx + width) % width][(centery + dy + height) % height] > 0) {
                                    continue findplacey;
                                }
                            }
                        }
                        SnakesProto.Direction dir;

                        if (!foodmap[(centerx - 1 + width) % width][centery]) {
                            dir = SnakesProto.Direction.RIGHT;
                            fieldmap[(centerx - 1 + width) % width][centery] += 1;
                        } else if (!foodmap[centerx][(centery - 1 + height) % height]) {
                            dir = SnakesProto.Direction.DOWN;
                            fieldmap[centerx][(centery - 1 + height) % height] += 1;
                        } else if (!foodmap[(centerx + 1) % width][centery]) {
                            dir = SnakesProto.Direction.LEFT;
                            fieldmap[(centerx + 1) % width][centery] += 1;
                        } else if (!foodmap[centerx][(centery + 1) % height]) {
                            dir = SnakesProto.Direction.UP;
                            fieldmap[centerx][(centery + 1) % height] += 1;
                        } else continue findplacey;
                        snakes.put(newPlayer.getId(), new Snake(newPlayer, new Point(centerx, centery), dir, width, height));
                        fieldmap[centerx][centery] += 1;
                        generateFood();
                        return true;
                    }
                }
            }
        }
        return false;

    }
    public int addPlayer(SnakesProto.GamePlayer.Builder newPlayer) {
        int id = newPlayerId();
        if (addPlayer(newPlayer.setId(id).build()))
            return id;
        else
            return -1;
    }


    int[][] fieldmap;

    boolean[][] foodmap;

    public synchronized int newFrame(){

        snakes.values().removeIf(snake -> snake.isDead);

        for(var snake : snakes.values()){
            var head = snake.moveHead();
            fieldmap[head.x][head.y] += 1;

            if (foodmap[head.x][head.y]){
                snake.score++;
            }
            else{
                var tail = snake.moveTail();
                fieldmap[tail.x][tail.y] -= 1;
            }
        }

        food.removeIf(f -> {
            if (fieldmap[f.x][f.y] > 0){
                foodmap[f.x][f.y] = false;
                return true;
            }
            return false;
        });

        for(var snake : snakes.values()){
            var head = snake.getHead();
            if (fieldmap[head.x][head.y] > 1){
                snake.isDead=true;
            }
        }

        for(var snake : snakes.values()){
            if (!snake.isDead) continue;
            var body = snake.getCoordinates();
            var last = snake.getHead();
            fieldmap[last.x][last.y] -= 1;
            for(var item : body){
                    if (item.x != 0){
                        for (int i = item.x; i!=0; i += (item.x>0)?-1:1){
                            fieldmap[(last.x + i + width) % width][last.y] -= 1;
                            if (rand.nextBoolean()){
                                food.add(new Point((last.x + i + width) % width,last.y));
                                foodmap[(last.x + i + width) % width][last.y] = true;
                            }
                        }
                    }
                    else if (item.y != 0){
                        for (int i = item.y; i!=0; i += (item.y>0)?-1:1){
                            fieldmap[last.x][(last.y + i + height) % height] -= 1;
                            if (rand.nextBoolean()) {
                                food.add(new Point(last.x, (last.y + i + height) % height));
                                foodmap[last.x][(last.y + i + height) % height] = true;
                            }
                        }
                    }
                    last = last.addabs(item, width, height);
            }
        }
        generateFood();
        return ++stateorder;
    }

    public void changeSnakeDirection(Integer gamePlayer, SnakesProto.Direction direction) {
            if (gamePlayer == null || gamePlayer < 0) return;
            var snake = snakes.get(gamePlayer);
            if (snake!=null){
                snake.setDirection(direction);
            }
    }

    public void makeZombie(Integer pid) {
        if (pid != null){
            var snake = snakes.get(pid);
            if (snake == null) return;
            snake.player = null;
            snake.isZombie = true;
        }
    }
}














