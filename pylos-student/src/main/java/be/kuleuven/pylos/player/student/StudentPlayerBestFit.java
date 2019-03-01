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

    final static int SEARCH_DEPTH = 5;


    PylosSphere lastSphere = null;
    Random r = new Random();


    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        Move move = selectBestMove(sim, board, game, 1);
        System.out.println("Move chosen: " + move);
        System.out.println("Next predicted move: " + move.getOptimalChild());
        System.out.println();
        game.moveSphere(move.getSphere(), move.getLocation());

        //TODO: TEMPORARY
        lastSphere = move.getSphere();
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        //TODO: TEMPORARY
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        Move move = selectBestMove(sim, board, game, 1);
        game.removeSphere(move.getSphere());
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        //TODO: TEMPORARY
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        Move move = selectBestMove(sim, board, game, 1);

        if (move.getSphere() != null) game.removeSphere(move.getSphere());
        else game.pass();
    }

    private Move selectBestMove(PylosGameSimulator sim, PylosBoard board, PylosGameIF game, int depth) {

        PylosPlayerColor currentColor = sim.getColor();

        if (sim.getState() == PylosGameState.COMPLETED) {
            //Indien het spel vervolledigd is
            Move m = new Move(null, null);
            m.setScore(evalBoard(board, currentColor));
            return m;
        }


        if (sim.getState() == PylosGameState.MOVE) {

            List<Move> moves = generateMoves(board, currentColor, game, depth);

            for (Move m : moves) {
                Move reverseMove = new Move(m.getSphere(), currentColor);
                sim.moveSphere(m.getSphere(), m.getLocation());

                if (sim.getState() == PylosGameState.REMOVE_FIRST) {
                    //Zelfde speler blijft aan de beurt
                    m.setScore(selectBestMove(sim, board, game, depth).getScore());
                } else {
                    //Het is de beurt aan de andere
                    if (depth < SEARCH_DEPTH && sim.getState() != PylosGameState.COMPLETED) {
                        m.addChild(selectBestMove(sim, board, game, depth + 1));
                    } else {
                        m.setScore(evalBoard(board, currentColor));
                    }
                }

                //Keer eke were
                if (reverseMove.getLocation() == null) {
                    sim.undoAddSphere(reverseMove.getSphere(), PylosGameState.MOVE, currentColor);
                } else
                    sim.undoMoveSphere(reverseMove.getSphere(), reverseMove.getLocation(), PylosGameState.MOVE, currentColor);

                if (sim.getState() != PylosGameState.MOVE) {
                    System.out.println("stop");
                }

            }
            if (depth == 1) {
                for (Move m : moves) System.out.println(m);
            }
            //Selecteer move met grootste score
            return moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);
        }
        if (sim.getState() == PylosGameState.REMOVE_FIRST) {
            List<Move> moves = generateRemoves(board, currentColor, game, depth, false);

            for (Move m : moves) {
                Move reverseMove = new Move(m.getSphere(), sim.getColor());

                sim.removeSphere(m.getSphere());

                m.setScore(selectBestMove(sim, board, game, depth).getScore());

                //Keer eke were
                sim.undoRemoveFirstSphere(reverseMove.getSphere(), reverseMove.getLocation(), PylosGameState.REMOVE_FIRST, currentColor);
            }
            return moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);
        }
        if (sim.getState() == PylosGameState.REMOVE_SECOND) {
            List<Move> moves = generateRemoves(board, sim.getColor(), game, depth, true);
            //Pass Move nog toevoegen (wordt voorgesteld door lege move)

            for (Move m : moves) {
                Move reverseMove = null;
                if (m.getSphere() != null) {
                    reverseMove = new Move(m.getSphere(), sim.getColor());
                    sim.removeSphere(m.getSphere());
                } else {
                    sim.pass();
                }

                if (depth < SEARCH_DEPTH) {
                    m.addChild(selectBestMove(sim, board, game, depth + 1));
                } else {
                    m.setScore(evalBoard(board, currentColor));
                }

                if (reverseMove != null) {
                    sim.undoRemoveSecondSphere(reverseMove.getSphere(), reverseMove.getLocation(), PylosGameState.REMOVE_SECOND, currentColor);
                } else {
                    sim.undoPass(PylosGameState.REMOVE_SECOND, currentColor);
                }
            }
            return moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);

        }
        //Hier zouden we normaal niet mogen komen
        assert false;
        return null;

    }

    private List<Move> generateMoves(PylosBoard board, PylosPlayerColor color, PylosGameIF game, int depth) {
        List<PylosLocation> usableLocations = Arrays.stream(board.getLocations()).filter(PylosLocation::isUsable).collect(Collectors.toList());
        List<Move> moves = new ArrayList<>();


        PylosSphere reserveSphere = board.getReserve(color);
        for (PylosLocation loc : usableLocations) {
            if (depth != 1 || !game.moveSphereIsDraw(reserveSphere, loc)) {
                moves.add(new Move(reserveSphere, loc, color));
            }
            //Als de locatie niet op de grond ligt kan het zijn dat we deze kunnen vullen met spheres op het veld
            if (loc.Z > 0) {
                //Selecteer alle spheres die lager dan deze locatie liggen op het bord en kunnen bewegen
                List<PylosSphere> freeSpheresBelow = Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null && s.getLocation().Z < loc.Z && s.canMove() && !s.getLocation().isBelow(loc)).collect(Collectors.toList());
                for (PylosSphere s : freeSpheresBelow) {
                    if (depth != 1 || !game.moveSphereIsDraw(s, loc)) {
                        moves.add(new Move(s, loc, color));
                    }
                }
            }
        }

        return moves;
    }

    private List<Move> generateRemoves(PylosBoard board, PylosPlayerColor color, PylosGameIF game, int depth, boolean passAllowed) {
        List<PylosSphere> removeableSpheres = Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null && s.canMove()).collect(Collectors.toList());
        List<Move> moves = new ArrayList<>();

        for (PylosSphere s : removeableSpheres) {
            //TODO: dat moet hier echt vele duidelijker
            if (depth != 1 || game.getState() == PylosGameState.MOVE || !game.removeSphereIsDraw(s)) {
                moves.add(new Move(s, null, color));
            }
        }
        //Als er gepast mag worden, een extra lege move toevoegen
        if (passAllowed) {
            //Gewoon uit interesse
            //assert game.passIsDraw();
            moves.add(new Move(null, null, color));
        }

        return moves;
    }


    private int evalBoard(PylosBoard board, PylosPlayerColor color) {
        //Spel is voltooid
        if (board.getReservesSize(color) == 0 && board.getReservesSize(color.other()) == 0) {
            return board.getBoardLocation(0, 0, 3).getSphere().PLAYER_COLOR == color ? Integer.MAX_VALUE : Integer.MIN_VALUE;
        }
        if (board.getReservesSize(color) == 0) {
            return Integer.MIN_VALUE;
        }
        if (board.getReservesSize(color.other()) == 0) {
            return Integer.MAX_VALUE;
        }


        //Spel is nog bezig
        //Voorlopig gewoon eigen reserve - reserve van ander
        return board.getReservesSize(color) - board.getReservesSize(color.other());
    }


}