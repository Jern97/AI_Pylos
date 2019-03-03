package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosGameIF;
import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosSphere;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by Ine on 5/05/2015.
 */
public class StudentPlayerRandomFit extends PylosPlayer{

    PylosSphere lastSphere = null;
    Random r = new Random();

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        ArrayList<PylosLocation> allLocations = new ArrayList<>(Arrays.asList(board.getLocations()));
        List<PylosLocation> usableLocations = allLocations.stream().filter(PylosLocation::isUsable).collect(Collectors.toList());
        PylosSphere reserveSphere = board.getReserve(this);
        while(true){
            PylosLocation location = usableLocations.get(r.nextInt(usableLocations.size()));
            if(!game.moveSphereIsDraw(reserveSphere, location)){
                game.moveSphere(reserveSphere, location);
                lastSphere = reserveSphere;
                break;
            }
        }
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        game.removeSphere(lastSphere);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        game.pass();
    }
}