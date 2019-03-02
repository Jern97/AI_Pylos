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
    private PylosLocation from;
    private PylosLocation to;
    private PylosPlayerColor color;
    private int score = Integer.MAX_VALUE; //De minst optimistische score
    private List<Move> children = new ArrayList<>();

    public Move(PylosSphere sphere, PylosLocation to, PylosPlayerColor color) {
        this.sphere = sphere;
        if(sphere != null) this.from = sphere.getLocation();
        this.to = to;
        this.color = color;
    }

    //Constructor voor reverseMove
    public Move(PylosSphere sphere, PylosPlayerColor color){
        this.sphere = sphere;
        this.to = sphere.getLocation();
        this.color = color;
    }

    public PylosSphere getSphere() {
        return sphere;
    }

    public PylosLocation getTo() {
        return to;
    }

    public List<Move> getChildren() {
        return children;
    }

    public void setScore(int score){
        this.score = score;
    }

    public int getScore() {
        return score;
    }

    public void addChildren(List<Move> children){
        for(Move m: children) {
            this.children.add(m);
            score = Math.min(score, m.getScore() * -1);
        }
    }

    public Move getOptimalChild(){
        if(!children.isEmpty())
            return children.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);
        else return null;
    }



    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("("+score+")");
        if(from != null){
            sb.append("["+from.X + ", " + from.Y + ", " + from.Z+ "]");
        }
        else{
            sb.append("[reserve]");
        }
        sb.append(" TO ");
        if(to != null){
            sb.append("["+ to.X + ", " + to.Y + ", " + to.Z+ "]");
        }
        else{
            sb.append("[reserve]");
        }
        return sb.toString();
    }
}
