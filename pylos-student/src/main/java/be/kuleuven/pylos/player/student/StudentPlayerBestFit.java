package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Ine on 25/02/2015.
 */
public class StudentPlayerBestFit extends PylosPlayer{

    PylosSphere lastSphere = null;
    Random r = new Random();

    //Hashmap die coordinaten omvormt naar een index
    static HashMap<Coordinate, Integer> coordinateIndexHashMap = fillHashmap();

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



    private List<Move> findAllMoves(ReducedBoard board){
        List<Move> possibleMoves = new ArrayList<>();
        ArrayList<PylosLocation> allLocations = new ArrayList<>(Arrays.asList(board.getLocations()));
        List<PylosLocation> usableLocations = allLocations.stream().filter(PylosLocation::isUsable).collect(Collectors.toList());
        for(PylosLocation location : usableLocations){

        }
    }

    private static HashMap<Coordinate, Integer> fillHashmap(){
        int index = 0;
        for(int y = 0; y < 4; y++){
            for(int )
        }
    }
}
