package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.classes.Move;
import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by Ine on 25/02/2015.
 */
public class StudentPlayerBestFit extends PylosPlayer {

    final static int SEARCH_DEPTH = 2;


    PylosSphere lastSphere = null;
    Random r = new Random();

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        ArrayList<PylosLocation> allLocations = new ArrayList<>(Arrays.asList(board.getLocations()));
        List<PylosLocation> usableLocations = allLocations.stream().filter(PylosLocation::isUsable).collect(Collectors.toList());
        PylosSphere reserveSphere = board.getReserve(this);
        while (true) {
            PylosLocation location = usableLocations.get(r.nextInt(usableLocations.size()));
            if (!game.moveSphereIsDraw(reserveSphere, location)) {
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

    private Move selectBestMove(PylosGameSimulator sim, PylosBoard board, PylosPlayer player, int depth) {
        List<Move> moves = generateMoves(board, player);

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move m : moves) {
            Move reverseMove = new Move(m.getSphere());
            PylosGameState state = sim.getState();
            sim.moveSphere(m.getSphere(), m.getLocation());

            if(depth == SEARCH_DEPTH) evalBoard(board);




            //Spel terugdraaien in de tijd
            if (reverseMove.getLocation() == null) {
                sim.undoAddSphere(reverseMove.getSphere(), state, player.PLAYER_COLOR);
            }
            else sim.undoMoveSphere(reverseMove.getSphere(), reverseMove.getLocation(), state, player.PLAYER_COLOR);



        }


    }


    private List<Move> generateMoves(PylosBoard board, PylosPlayer player) {
        List<PylosLocation> usableLocations = Arrays.stream(board.getLocations()).filter(PylosLocation::isUsable).collect(Collectors.toList());
        List<Move> moves = new ArrayList<>();


        PylosSphere reserveSphere = board.getReserve(player);
        for (PylosLocation loc : usableLocations) {
            moves.add(new Move(reserveSphere, loc));
            //Als de locatie niet op de grond ligt kan het zijn dat we deze kunnen vullen met spheres op het veld
            if (loc.Z > 0) {
                //Selecteer alle spheres die onder deze locatie liggen op het bord en kunnen bewegen
                List<PylosSphere> freeSpheresBelow = Arrays.stream(board.getSpheres(player)).filter(s -> s.getLocation() != null && s.getLocation().Z < loc.Z && s.canMove()).collect(Collectors.toList());
                for (PylosSphere s : freeSpheresBelow) {
                    moves.add(new Move(s, loc));
                }
            }
        }

        return moves;
    }


    private int evalBoard(PylosBoard board) {
        //Voorlopige eval functie
        return board.getReservesSize(this.PLAYER_COLOR) - board.getReservesSize(this.PLAYER_COLOR.other());
    }


}