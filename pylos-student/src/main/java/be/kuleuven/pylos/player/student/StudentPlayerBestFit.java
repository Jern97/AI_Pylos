package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.classes.Move;
import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Ine on 25/02/2015.
 */
public class StudentPlayerBestFit extends PylosPlayer {

    final static int SEARCH_DEPTH = 3;


    PylosSphere lastSphere = null;
    Random r = new Random();

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        Move move = selectBestMove(sim, board, this, game, 0);
        game.moveSphere(move.getSphere(), move.getLocation());

        //TODO: TEMPORARY
        lastSphere = move.getSphere();
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        //TODO: TEMPORARY
        game.removeSphere(lastSphere);
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        //TODO: TEMPORARY
        game.pass();
    }

    private Move selectBestMove(PylosGameSimulator sim, PylosBoard board, PylosPlayer player, PylosGameIF game, int depth) {
        List<Move> moves = generateMoves(board, player, game);

        Move bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (Move m : moves) {
            Move reverseMove = new Move(m.getSphere(), player.PLAYER_COLOR);
            Move firstRemoveMove = null;
            Move secondRemoveMove = null;
            PylosGameState state = sim.getState();

            if (sim.getState() == PylosGameState.COMPLETED) {
                m.setScore(sim.getWinner() == player.PLAYER_COLOR ? 1 : -1);
                continue;
            }

            sim.moveSphere(m.getSphere(), m.getLocation());

            //TODO: Voorlopig zeer naïve manier van verwijderen
            if (sim.getState() == PylosGameState.REMOVE_FIRST) {
                PylosSphere s = m.getSphere();
                firstRemoveMove = new Move(s, player.PLAYER_COLOR);
                sim.removeSphere(s);
                if (sim.getState() == PylosGameState.REMOVE_SECOND) {
                    //Voorlopig altijd passen
                    sim.pass();
                }
            }


            if (depth == SEARCH_DEPTH) {
                m.setScore(evalBoard(board, player.PLAYER_COLOR));
            } else {
                m.setScore(selectBestMove(sim, board, player.OTHER, game,depth + 1).getScore() * -1);
            }


            //Spel terugdraaien in de tijd


            if (firstRemoveMove != null) {
                if (secondRemoveMove != null) {
                    sim.undoRemoveSecondSphere(secondRemoveMove.getSphere(), secondRemoveMove.getLocation(), PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR);
                } else {
                    sim.undoPass(PylosGameState.REMOVE_SECOND, player.PLAYER_COLOR);
                }
                sim.undoRemoveFirstSphere(firstRemoveMove.getSphere(), firstRemoveMove.getLocation(), PylosGameState.REMOVE_FIRST, player.PLAYER_COLOR);
            }

            if (reverseMove.getLocation() == null) {
                sim.undoAddSphere(reverseMove.getSphere(), state, player.PLAYER_COLOR);
            } else
                sim.undoMoveSphere(reverseMove.getSphere(), reverseMove.getLocation(), state, player.PLAYER_COLOR);


        }

        //Selecteer move met grootste score
        return moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);


    }

    private List<Move> generateMoves(PylosBoard board, PylosPlayer player, PylosGameIF game) {
        List<PylosLocation> usableLocations = Arrays.stream(board.getLocations()).filter(PylosLocation::isUsable).collect(Collectors.toList());
        List<Move> moves = new ArrayList<>();


        PylosSphere reserveSphere = board.getReserve(player);
        for (PylosLocation loc : usableLocations) {
            if (!game.moveSphereIsDraw(reserveSphere, loc)) {
                moves.add(new Move(reserveSphere, loc, player.PLAYER_COLOR));
            }
            //Als de locatie niet op de grond ligt kan het zijn dat we deze kunnen vullen met spheres op het veld
            if (loc.Z > 0) {
                //Selecteer alle spheres die onder deze locatie liggen op het bord en kunnen bewegen
                List<PylosSphere> freeSpheresBelow = Arrays.stream(board.getSpheres(player)).filter(s -> s.getLocation() != null && s.getLocation().Z < loc.Z && s.canMove() && !s.getLocation().isBelow(loc)).collect(Collectors.toList());
                for (PylosSphere s : freeSpheresBelow) {
                    if (!game.moveSphereIsDraw(s, loc)) {
                        moves.add(new Move(s, loc, player.PLAYER_COLOR));
                    }
                }
            }
        }

        return moves;
    }


    private int evalBoard(PylosBoard board, PylosPlayerColor color) {
        //Voorlopige eval functie
        return board.getReservesSize(color) - board.getReservesSize(color.other());
    }


}