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

    final static int SEARCH_DEPTH = 0;

    Random r = new Random();


    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        List<Move> moves = selectBestMove(sim, board, game, 0);
        Move bestMove = moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);

        game.moveSphere(bestMove.getSphere(), bestMove.getTo());
        //checkIfTreeIsFullyGrown(moves, 1);

        //System.out.println("stop");

    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        List<Move> moves = selectBestMove(sim, board, game, 0);
        //TODO: Soms geen moves gevonden???
        Move bestMove = moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);

        game.removeSphere(bestMove.getSphere());

        //System.out.println("stop");
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        List<Move> moves = selectBestMove(sim, board, game, 0);
        Move bestMove = moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);

        if (bestMove.getSphere() != null) game.removeSphere(bestMove.getSphere());
        else game.pass();

        //System.out.println("stop");
    }

    private List<Move> selectBestMove(PylosGameSimulator sim, PylosBoard board, PylosGameIF game, int depth) {

        PylosPlayerColor currentColor = sim.getColor();

        if (sim.getState() == PylosGameState.COMPLETED) {
            assert false;
            //Indien het spel vervolledigd is
            Move m = new Move(null, null);
            m.setScore(evalBoard(board, currentColor.other()));
            List<Move> moves = new ArrayList<>();
            moves.add(m);
            return moves;
        }


        if (sim.getState() == PylosGameState.MOVE) {

            List<Move> moves = generateMoves(board, currentColor, game, depth);

            for (Move m : moves) {
                Move reverseMove = new Move(m.getSphere(), currentColor);
                sim.moveSphere(m.getSphere(), m.getTo());

                if (sim.getState() == PylosGameState.REMOVE_FIRST) {
                    //Zelfde speler blijft aan de beurt
                    m.addChildren(selectBestMove(sim, board, game, depth));
                } else {
                    //Het is de beurt aan de andere
                    if (depth < SEARCH_DEPTH && sim.getState() != PylosGameState.COMPLETED) {
                        m.addChildren(selectBestMove(sim, board, game, depth + 1));
                    } else {
                        m.setScore(evalBoard(board, currentColor));
                    }
                }

                //Keer eke were
                if (reverseMove.getTo() == null) {
                    sim.undoAddSphere(reverseMove.getSphere(), PylosGameState.MOVE, currentColor);
                } else
                    sim.undoMoveSphere(reverseMove.getSphere(), reverseMove.getTo(), PylosGameState.MOVE, currentColor);

                if (sim.getState() != PylosGameState.MOVE) {
                    System.out.println("stop");
                }

            }

            //PRINTS VOOR DEBUG
            if (depth == 0) {
                /*
                Move bestMove = moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);
                System.out.println("***** AI:");
                for (Move m : moves) System.out.println(m);
                System.out.println("Move chosen: "+bestMove);
                System.out.println("***** Opponent:");
                for (Move m: bestMove.getChildren()) System.out.println(m);
                System.out.println();
                */
            }

            return moves;
        }
        if (sim.getState() == PylosGameState.REMOVE_FIRST) {
            List<Move> moves = generateRemoves(board, currentColor, game, depth, false);

            if (moves.isEmpty()) {
                System.out.println("stop");
            }

            for (Move m : moves) {
                Move reverseMove = new Move(m.getSphere(), sim.getColor());

                sim.removeSphere(m.getSphere());

                m.addChildren(selectBestMove(sim, board, game, depth));

                //Keer eke were
                sim.undoRemoveFirstSphere(reverseMove.getSphere(), reverseMove.getTo(), PylosGameState.REMOVE_FIRST, currentColor);
            }
            return moves;
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
                    //Kinderen moeten toegevoegd worden aan de parent Move (niet de remove moves)
                    m.addChildren(selectBestMove(sim, board, game, depth + 1));
                } else {
                    m.setScore(evalBoard(board, currentColor));
                }

                if (reverseMove != null) {
                    sim.undoRemoveSecondSphere(reverseMove.getSphere(), reverseMove.getTo(), PylosGameState.REMOVE_SECOND, currentColor);
                } else {
                    sim.undoPass(PylosGameState.REMOVE_SECOND, currentColor);
                }
            }
            return moves;

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
            if (depth != 0 || !game.moveSphereIsDraw(reserveSphere, loc)) {
                moves.add(new Move(reserveSphere, loc, color, depth));
            }
            //Als de locatie niet op de grond ligt kan het zijn dat we deze kunnen vullen met spheres op het veld
            if (loc.Z > 0) {
                //Selecteer alle spheres die lager dan deze locatie liggen op het bord en kunnen bewegen
                List<PylosSphere> freeSpheresBelow = Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null && s.getLocation().Z < loc.Z && s.canMove() && !s.getLocation().isBelow(loc)).collect(Collectors.toList());
                for (PylosSphere s : freeSpheresBelow) {
                    if (depth != 0 || !game.moveSphereIsDraw(s, loc)) {
                        moves.add(new Move(s, loc, color, depth));
                    }
                }
            }
        }
        Collections.shuffle(moves);
        return moves;
    }

    private List<Move> generateRemoves(PylosBoard board, PylosPlayerColor color, PylosGameIF game, int depth, boolean passAllowed) {
        List<PylosSphere> removableSpheres = Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null && s.canMove()).collect(Collectors.toList());

        if (!passAllowed && removableSpheres.isEmpty()) {
            System.out.println("stop");
        }
        List<Move> moves = new ArrayList<>();
        List<Move> drawMoves = new ArrayList<>();
        for (PylosSphere s : removableSpheres) {
            //TODO: dat moet hier echt vele duidelijker
            if (depth == 0 && game.getState() != PylosGameState.MOVE) {
                if (!game.removeSphereIsDraw(s)) {
                    moves.add(new Move(s, null, color, depth));
                } else {
                    drawMoves.add(new Move(s, null, color, depth));
                }
            } else {
                moves.add(new Move(s, null, color, depth));
            }
        }
        //Als er gepast mag worden, een extra lege move toevoegen
        if (passAllowed) {
            //Gewoon uit interesse
            //assert game.passIsDraw();
            moves.add(new Move(null, null, color, depth));
        }
        if (!moves.isEmpty()) {
            Collections.shuffle(moves);
            return moves;
        } else {
            Collections.shuffle(drawMoves);
            return drawMoves;
        }
    }


    private int evalBoard(PylosBoard board, PylosPlayerColor color) {
        //Spel is voltooid
        if (board.getReservesSize(color) == 0 && board.getReservesSize(color.other()) == 0) {
            return board.getBoardLocation(0, 0, 3).getSphere().PLAYER_COLOR == color ? 99999 : -99999;
        } else if (board.getReservesSize(color) == 0) {
            return -99999;
        } else if (board.getReservesSize(color.other()) == 0) {
            return 99999;
        }


        //Spel is nog bezig
        int nReserves = board.getReservesSize(color);
        int nReservesOpp = board.getReservesSize(color.other());

        int nMovableSpheres = (int) Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null && s.canMove()).count();
        int nMovableSpheresOpp = (int) Arrays.stream(board.getSpheres(color.other())).filter(s -> s.getLocation() != null && s.canMove()).count();

        int squareFactor = 0;
        int squareFactorOpp = 0;
        for (PylosSquare square : board.getAllSquares()) {
            if (square.getInSquare(color.other()) == 0) {
                switch (square.getInSquare(color)) {
                    case 0:
                        squareFactor += 0;
                        break;
                    case 1:
                        squareFactor += 1;
                        break;
                    case 2:
                        squareFactor += 4;
                        break;
                    case 3:
                        squareFactor += 16;
                        break;
                    case 4:
                        squareFactor += 25;
                        break;
                }
            }

            if (square.getInSquare(color) == 0) {
                switch (square.getInSquare(color.other())) {
                    case 0:
                        squareFactorOpp += 0;
                        break;
                    case 1:
                        squareFactorOpp += 1;
                        break;
                    case 2:
                        squareFactorOpp += 4;
                        break;
                    case 3:
                        squareFactorOpp += 16;
                        break;
                    case 4:
                        squareFactorOpp += 25;
                        break;
                }
            }
        }
        return 20 * (nReserves - nReservesOpp) + 1 * (squareFactor - squareFactorOpp) + 1 * (nMovableSpheres - nMovableSpheresOpp);

    }

}