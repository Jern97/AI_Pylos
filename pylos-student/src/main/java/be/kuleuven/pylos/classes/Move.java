package be.kuleuven.pylos.classes;

import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosSphere;

import java.util.ArrayList;
import java.util.List;

public class Move {
    private PylosSphere sphere;
    private PylosLocation location;
    private List<Move> children = new ArrayList<>();

    public Move(PylosSphere sphere, PylosLocation location) {
        this.sphere = sphere;
        this.location = location;
    }

    //Constructor voor reverseMove
    public Move(PylosSphere sphere){
        this.sphere = sphere;
        this.location = sphere.getLocation();
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
}
