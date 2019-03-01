package be.kuleuven.pylos.classes;

import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosPlayerColor;
import be.kuleuven.pylos.game.PylosSphere;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

public class Move {
    private PylosSphere sphere;
    private PylosLocation location;
    private PylosPlayerColor color;
    private int score = Integer.MIN_VALUE; //De minst optimistische score
    private List<Move> children = new ArrayList<>();

    public Move(PylosSphere sphere, PylosLocation location, PylosPlayerColor color) {
        this.sphere = sphere;
        this.location = location;
        this.color = color;
    }

    //Constructor voor reverseMove
    public Move(PylosSphere sphere, PylosPlayerColor color){
        this.sphere = sphere;
        this.location = sphere.getLocation();
        this.color = color;
    }

    public PylosSphere getSphere() {
        return sphere;
    }

    public PylosLocation getLocation() {
        return location;
    }

    public List<Move> getChildren() {
        return children;
    }

    public void setScore(int score) {

        if(score == Integer.MAX_VALUE || score == Integer.MIN_VALUE){
            System.out.println("stop");
        }
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public void addChild(Move m){
        children.add(m);
        score = Math.max(score, m.getScore()*-1);
    }

    public Move getOptimalChild(){
        return children.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);

    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("("+score+")");
        if(sphere.getLocation() != null){
            sb.append("["+sphere.getLocation().X + ", " + sphere.getLocation().Y + ", " + sphere.getLocation().Z+ "]");
        }
        else{
            sb.append("[reserve]");
        }
        sb.append(" TO ");
        if(location != null){
            sb.append("["+location.X + ", " + location.Y + ", " + location.Z+ "]");
        }
        else{
            sb.append("[reserve]");
        }
        return sb.toString();
    }
}
