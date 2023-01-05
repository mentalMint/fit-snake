package lab4.model;

import lab4.protobuf.SnakesProto;

public class PlayerView {
    public int id;
    public SnakesProto.GameState state;

    public PlayerView(int id, SnakesProto.GameState state){
        this.id = id;
        this.state = state;
    }
}
