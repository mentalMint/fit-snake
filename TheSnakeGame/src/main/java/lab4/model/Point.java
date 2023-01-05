package lab4.model;

import lab4.protobuf.SnakesProto;

import java.util.Objects;

public class Point {

    public int x;
    public int y;

    public Point(int x, int y){
        this.x = x;
        this.y = y;
    }

    public Point(SnakesProto.Direction d){
        switch (d){
            case UP: this.x=0; this.y=-1; break;
            case DOWN: this.x=0; this.y=1; break;
            case RIGHT: this.x=1; this.y=0; break;
            case LEFT: this.x=-1; this.y=0; break;
        }
    }

    public Point(SnakesProto.GameState.Coord point) {
        this.x = point.getX();
        this.y = point.getY();
    }

    public Point substract(Point d, int dx, int dy){
        return new Point((this.x - d.x) % dx, (this.y - d.y) % dy);
    }
    public Point substract(Point d){
        return new Point((this.x - d.x), (this.y - d.y));
    }

    public Point substractabs(Point d, int dx, int dy){
        return new Point((this.x - d.x + dx) % dx, (this.y - d.y + dy) % dy);
    }


    public Point add(Point d, int dx, int dy){
        return new Point((this.x + d.x) % dx, (this.y + d.y) % dy);
    }
    public Point add(Point d){
        return new Point((this.x + d.x), (this.y + d.y));
    }
    public Point addabs(Point d, int dx, int dy){
        return new Point((this.x + d.x +dx) % dx, (this.y + d.y + dy) % dy);
    }


    @Override
    public String toString() {
        return "{" +  x + "; " + y + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Point point = (Point) o;
        return x == point.x && y == point.y;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
