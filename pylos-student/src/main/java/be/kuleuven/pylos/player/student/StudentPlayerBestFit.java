package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Ine on 25/02/2015.
 */
public class StudentPlayerBestFit extends PylosPlayer {

    private final static int SEARCH_DEPTH = 0;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        List<Move> moves = selectBestMove(sim, board, game, 0);
        Move bestMove = moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);

        game.moveSphere(bestMove.getSphere(), bestMove.getTo());
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        List<Move> moves = selectBestMove(sim, board, game, 0);
        //TODO: Soms geen moves gevonden???
        Move bestMove = moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);

        game.removeSphere(bestMove.getSphere());
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        List<Move> moves = selectBestMove(sim, board, game, 0);
        Move bestMove = moves.stream().max(Comparator.comparing(Move::getScore)).orElseThrow(NoSuchElementException::new);

        if (bestMove.getSphere() != null) game.removeSphere(bestMove.getSphere());
        else game.pass();
    }

    private List<Move> selectBestMove(PylosGameSimulator sim, PylosBoard board, PylosGameIF game, int depth) {

        PylosPlayerColor currentColor = sim.getColor();

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

            for (Move m : moves) {
                Move reverseMove = null;
                if (m.getSphere() != null) {
                    reverseMove = new Move(m.getSphere(), sim.getColor());
                    sim.removeSphere(m.getSphere());
                } else {
                    sim.pass();
                }

                if (depth < SEARCH_DEPTH) {
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
        return null;

    }

    private List<Move> generateMoves(PylosBoard board, PylosPlayerColor color, PylosGameIF game, int depth) {
        List<PylosLocation> usableLocations = Arrays.stream(board.getLocations()).filter(PylosLocation::isUsable).collect(Collectors.toList());
        List<Move> moves = new ArrayList<>();

        PylosSphere reserveSphere = board.getReserve(color);
        for (PylosLocation loc : usableLocations) {
            if (depth != 0 || !game.moveSphereIsDraw(reserveSphere, loc)) {
                moves.add(new Move(reserveSphere, loc, color));
            }
            //Als de locatie niet op de grond ligt kan het zijn dat we deze kunnen vullen met spheres op het veld
            if (loc.Z > 0) {
                //Selecteer alle spheres die lager dan deze locatie liggen op het bord en kunnen bewegen
                List<PylosSphere> freeSpheresBelow = Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null && s.getLocation().Z < loc.Z && s.canMove() && !s.getLocation().isBelow(loc)).collect(Collectors.toList());
                for (PylosSphere s : freeSpheresBelow) {
                    if (depth != 0 || !game.moveSphereIsDraw(s, loc)) {
                        moves.add(new Move(s, loc, color));
                    }
                }
            }
        }
        Collections.shuffle(moves);
        return moves;
    }

    private List<Move> generateRemoves(PylosBoard board, PylosPlayerColor color, PylosGameIF game, int depth, boolean passAllowed) {
        List<PylosSphere> removableSpheres = Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null && s.canMove()).collect(Collectors.toList());
        List<Move> moves = new ArrayList<>();
        List<Move> drawMoves = new ArrayList<>();

        for (PylosSphere s : removableSpheres) {
            //Checken voor draw move kan enkel in de bovenste laag en bij move state.
            if (depth == 0 && game.getState() != PylosGameState.MOVE) {
                if (!game.removeSphereIsDraw(s)) {
                    moves.add(new Move(s, null, color));
                } else {
                    //Moves die voor een draw zorgen ook opvangen in het geval er geen andere zijn
                    drawMoves.add(new Move(s, null, color));
                }
            } else {
                moves.add(new Move(s, null, color));
            }
        }
        //Als er gepast mag worden, een extra lege move toevoegen
        if (passAllowed) {
            moves.add(new Move(null, null, color));
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
                    case 1: squareFactor += 1; break;
                    case 2: squareFactor += 2; break;
                    case 3: squareFactor += 10; break;
                    default: squareFactor += 0;
                }
            }

            if (square.getInSquare(color) == 0) {
                switch (square.getInSquare(color.other())) {
                    case 1: squareFactorOpp += 1; break;
                    case 2: squareFactorOpp += 2; break;
                    case 3: squareFactorOpp += 10; break;
                    default: squareFactorOpp += 0;
                }
            }
        }
        return 20 * (nReserves - nReservesOpp) + 1 * (squareFactor - squareFactorOpp) + 1 * (nMovableSpheres - nMovableSpheresOpp);
    }

    private class Move {
        private PylosSphere sphere;
        private PylosLocation from;
        private PylosLocation to;
        private PylosPlayerColor color;
        private int score;
        private boolean hasScore = false;
        private List<Move> children = new ArrayList<>();

        public Move(PylosSphere sphere, PylosLocation to, PylosPlayerColor color) {
            this.sphere = sphere;
            if (sphere != null) this.from = sphere.getLocation();
            this.to = to;
            this.color = color;
        }

        //Constructor voor reverseMove
        public Move(PylosSphere sphere, PylosPlayerColor color) {
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

        public void setScore(int score) {
            this.score = score;
            this.hasScore = true;
        }

        public int getScore() {
            return score;
        }

        public void addChildren(List<Move> children) {
            if(children.get(0).getColor() != color){
                if (!hasScore) {
                    score = children.get(0).getScore() * -1;
                    hasScore = true;
                }
                for (Move m : children) {
                    this.children.add(m);
                    score = Math.min(score, m.getScore() * -1);
                }
            } else {
                if (!hasScore) {
                    score = children.get(0).getScore();
                    hasScore = true;
                }
                for (Move m : children) {
                    this.children.add(m);
                    score = Math.max(score, m.getScore());
                }
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(" + score + ")");
            if (from != null) {
                sb.append("[" + from.X + ", " + from.Y + ", " + from.Z + "]");
            } else {
                sb.append("[reserve]");
            }
            sb.append(" TO ");
            if (to != null) {
                sb.append("[" + to.X + ", " + to.Y + ", " + to.Z + "]");
            } else {
                sb.append("[reserve]");
            }
            return sb.toString();
        }

        public PylosPlayerColor getColor() {
            return color;
        }
    }
}


