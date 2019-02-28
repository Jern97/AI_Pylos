package be.kuleuven.pylos.classes;

import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosPlayerColor;
import be.kuleuven.pylos.game.PylosSphere;

import java.util.ArrayList;
import java.util.List;

public class Move {
    private PylosSphere sphere;
    private PylosLocation location;
    private PylosPlayerColor color;
    private int score = Integer.MAX_VALUE; //De minst optimistische score
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
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public void addChild(Move m){
        children.add(m);
        score = Math.min(score, m.getScore()*-1);
    }
}
