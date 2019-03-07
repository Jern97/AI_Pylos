package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class StudentPlayerRandomFit extends PylosPlayer {

    private static final int SEARCH_DEPTH = 3;

    private PylosGameSimulator pylosGameSimulator;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        pylosGameSimulator = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        Action bestAction = getNextBestAction(board, new Score(), this.PLAYER_COLOR, this.PLAYER_COLOR, 0, null).getAction();
        Action action = bestAction;
        List<Action> exclude = new ArrayList<>();
        while (game.moveSphereIsDraw(action.getAbstractSphere().getSphere(), action.getDestination())) {
            exclude.add(action);
            action = getNextBestAction(board, new Score(), this.PLAYER_COLOR, this.PLAYER_COLOR, 0, exclude).getAction();
            if (action == null) {
                bestAction = getNextBestAction(board, new Score(), this.PLAYER_COLOR, this.PLAYER_COLOR, 0, null).getAction();
                break;
            }
        }

        game.moveSphere(bestAction.getAbstractSphere().getSphere(), bestAction.getDestination());
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        pylosGameSimulator = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        Action bestAction = getNextBestAction(board, new Score(), this.PLAYER_COLOR, this.PLAYER_COLOR, 0, null).getAction();
        Action action = bestAction;
        List<Action> exclude = new ArrayList<>();
        while (game.removeSphereIsDraw(action.getAbstractSphere().getSphere())) {
            exclude.add(action);
            action = getNextBestAction(board, new Score(), this.PLAYER_COLOR, this.PLAYER_COLOR, 0, exclude).getAction();
            if (action == null) {
                bestAction = getNextBestAction(board, new Score(), this.PLAYER_COLOR, this.PLAYER_COLOR, 0, null).getAction();
                break;
            }
        }

        game.removeSphere(bestAction.getAbstractSphere().getSphere());

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        pylosGameSimulator = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        Action bestAction = getNextBestAction(board, new Score(), this.PLAYER_COLOR, this.PLAYER_COLOR, 0, null).getAction();
        Action action = bestAction;
        List<Action> exclude = new ArrayList<>();

        while ((action.getType() == ActionType.REMOVE) ? game.removeSphereIsDraw(action.getAbstractSphere().getSphere()) : game.passIsDraw()) {
            exclude.add(action);
            action = getNextBestAction(board, new Score(), this.PLAYER_COLOR, this.PLAYER_COLOR, 0, exclude).getAction();
            if (action == null) {
                bestAction = getNextBestAction(board, new Score(), this.PLAYER_COLOR, this.PLAYER_COLOR, 0, null).getAction();
                break;
            }
        }

        if (bestAction.getType() == ActionType.REMOVE) {
            game.removeSphere(bestAction.getAbstractSphere().getSphere());
        } else {
            game.pass();
        }
    }

    private Score getNextBestAction(PylosBoard board, Score score, PylosPlayerColor prevPlayer, PylosPlayerColor currentPlayer, int depth, List<Action> exclude) {

        score.setOwner(currentPlayer);
        List<Action> allActions;
        //Increate depth++
        if (prevPlayer != currentPlayer) depth++;

        switch (pylosGameSimulator.getState()) {

            case MOVE: {
                allActions = generateAllMoves(board, currentPlayer);
                if (exclude != null) allActions.removeAll(exclude);
                if (!allActions.isEmpty()) score.setAction(allActions.get(0));

                for (Action action : allActions) {
                    //DO
                    pylosGameSimulator.moveSphere(action.getAbstractSphere().getSphere(), action.getDestination());

                    //Go deeper
                    goDeeper(score, depth, board, currentPlayer, action);

                    //UNDO
                    if (action.getAbstractSphere() instanceof ReserveSphere)
                        pylosGameSimulator.undoAddSphere(action.getAbstractSphere().getSphere(), PylosGameState.MOVE, currentPlayer);
                    else
                        pylosGameSimulator.undoMoveSphere(action.getAbstractSphere().getSphere(), action.getOrigin(), PylosGameState.MOVE, currentPlayer);

                    if (score.doPrune()) {
                        //System.out.println("PRUNY BOIIII: " + (SEARCH_DEPTH - depth) + " diepte vermeden");
                        //break;
                    }
                }


                break;
            }
            case REMOVE_FIRST: {
                allActions = generateAllRemoves(board, currentPlayer, false);
                if (exclude != null) allActions.removeAll(exclude);
                if (!allActions.isEmpty()) score.setAction(allActions.get(0));

                for (Action action : allActions) {
                    //DO
                    pylosGameSimulator.removeSphere(action.getAbstractSphere().getSphere());

                    //Go deeper
                    goDeeper(score, depth, board, currentPlayer, action);

                    //UNDO
                    pylosGameSimulator.undoRemoveFirstSphere(action.getAbstractSphere().getSphere(), action.getOrigin(), PylosGameState.REMOVE_FIRST, currentPlayer);

                    if (score.doPrune()) {
                        //System.out.println("PRUNY BOIIII: " + (SEARCH_DEPTH - depth) + " diepte vermeden");
                        //break;
                    }
                }

                break;
            }
            case REMOVE_SECOND: {
                allActions = generateAllRemoves(board, currentPlayer, true);
                if (exclude != null) allActions.removeAll(exclude);
                if (!allActions.isEmpty()) score.setAction(allActions.get(0));

                for (Action action : allActions) {
                    //DO
                    if (action.getType() == ActionType.REMOVE)
                        pylosGameSimulator.removeSphere(action.getAbstractSphere().getSphere());
                    else pylosGameSimulator.pass();

                    //Go deeper
                    goDeeper(score, depth, board, currentPlayer, action);

                    //UNDO
                    if (action.getType() == ActionType.REMOVE)
                        pylosGameSimulator.undoRemoveSecondSphere(action.getAbstractSphere().getSphere(), action.getOrigin(), PylosGameState.REMOVE_SECOND, currentPlayer);
                    else pylosGameSimulator.undoPass(PylosGameState.REMOVE_SECOND, currentPlayer);

                    if (score.doPrune()) {
                        //System.out.println("PRUNY BOIIII: " + (SEARCH_DEPTH - depth) + " diepte vermeden");
                        //break;
                    }
                }

                break;
            }
            case COMPLETED: {
                if (currentPlayer == this.PLAYER_COLOR)
                    score.setAlfa(Double.MAX_VALUE);
                else score.setBeta(-Double.MAX_VALUE);
                score.setAction(new Action(ActionType.COMPLETED));
                break;
            }

        }

        if (score.getAction() == null) {
            System.out.println();
        }

        return score;
    }

    private void goDeeper(Score score, int depth, PylosBoard board, PylosPlayerColor currentPlayer, Action action) {
        //Go deeper.jpg
        Score deepScore;
        if (depth < SEARCH_DEPTH) {
            // je mag verdergaan
            deepScore = getNextBestAction(board, new Score(score), currentPlayer, pylosGameSimulator.getColor(), depth, null);
        } else {
            deepScore = evaluate(board, currentPlayer, action);
        }

        adjustScore(score, deepScore, action);
    }

    private Score evaluate(PylosBoard board, PylosPlayerColor player, Action action) {
        PylosPlayer currentPlayer = player == this.PLAYER_COLOR ? this : this.OTHER;

        double evalScore = (this.PLAYER_COLOR == player) ? board.getReservesSize(player) - board.getReservesSize(player.other()) : board.getReservesSize(player.other()) - board.getReservesSize(player);

        double blockWeight = 1;
        double makeWeight = 0.5;
        double heightWeight = 1;

        if (this != currentPlayer) {
            blockWeight = -blockWeight;
            makeWeight = -makeWeight;
            heightWeight = -heightWeight;
        }


        if(action.getDestination() != null) {
            evalScore += action.getDestination().getMaxInSquare(currentPlayer.OTHER) == 3 ? blockWeight : 0;
            evalScore += action.getDestination().getMaxInSquare(currentPlayer) == 3 ? makeWeight : 0;
        }
        if (action.getOrigin() != null && action.getDestination() !=null){
            evalScore += action.getDestination().Z > action.getOrigin().Z ? heightWeight : 0;
        }


        if (this.PLAYER_COLOR == player) {
            return new Score(evalScore, Double.MAX_VALUE, player);
        } else {
            return new Score(-Double.MAX_VALUE, evalScore, player);
        }
    }

    private void adjustScore(Score score, Score deepScore, Action action) {
        if (score.getOwner() == this.PLAYER_COLOR && deepScore.getOwner() == this.PLAYER_COLOR && score.getAlfa() < deepScore.getAlfa()) {
            score.setAlfa(deepScore.getAlfa());
            score.setAction(action);
        } else if (score.getOwner() == this.PLAYER_COLOR && deepScore.getOwner() == this.PLAYER_COLOR.other() && score.getAlfa() < deepScore.getBeta()) {
            score.setAlfa(deepScore.getBeta());
            score.setAction(action);
        } else if (score.getOwner() == this.PLAYER_COLOR.other() && deepScore.getOwner() == this.PLAYER_COLOR.other() && score.getBeta() > deepScore.getBeta()) {
            score.setBeta(deepScore.getBeta());
            score.setAction(action);
        } else if (score.getOwner() == this.PLAYER_COLOR.other() && deepScore.getOwner() == this.PLAYER_COLOR && score.getBeta() > deepScore.getAlfa()) {
            score.setBeta(deepScore.getAlfa());
            score.setAction(action);
        }

    }

    private List<Action> generateAllMoves(PylosBoard board, PylosPlayerColor player) {
        List<Action> allMoves = new ArrayList<>();
        Arrays.stream(board.getLocations()).filter(PylosLocation::isUsable).forEach(l -> {
                    Arrays.stream(board.getSpheres(player))
                            .filter(s -> !s.isReserve() && s.getLocation().Z < l.Z && s.canMove() && !s.getLocation().isBelow(l))
                            .forEach(s -> allMoves.add(new Action(ActionType.MOVE, new BoardSphere(s), s.getLocation(), l)));
                    allMoves.add(new Action(ActionType.MOVE, new ReserveSphere(board, player), null, l));
                }
        );
        Collections.shuffle(allMoves);
        return allMoves;
    }

    private List<Action> generateAllRemoves(PylosBoard board, PylosPlayerColor player, boolean pass) {
        List<Action> allRemoves = new ArrayList<>();
        Arrays.stream(board.getSpheres(player)).filter(PylosSphere::canRemove).forEach(s -> allRemoves.add(new Action(ActionType.REMOVE, new BoardSphere(s), s.getLocation(), null)));
        if (pass) allRemoves.add(new Action(ActionType.PASS));
        Collections.shuffle(allRemoves);
        return allRemoves;
    }
}


class Score {
    private double alfa;
    private double beta;
    private Action action;
    private PylosPlayerColor owner;

    public Score(double alfa, double beta, PylosPlayerColor owner) {
        this.alfa = alfa;
        this.beta = beta;
        this.owner = owner;
    }

    public Score() {
        this.alfa = -Double.MAX_VALUE;
        this.beta = Double.MAX_VALUE;
    }

    public Score(Score s) {
        this.alfa = s.alfa;
        this.beta = s.beta;
    }

    public boolean doPrune() {
        return alfa >= beta;
    }

    public PylosPlayerColor getOwner() {
        return owner;
    }

    public void setOwner(PylosPlayerColor owner) {
        this.owner = owner;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public double getAlfa() {
        return alfa;
    }

    public void setAlfa(double alfa) {
        this.alfa = alfa;
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }
}

class Action {

    private AbstractSphere abstractSphere;
    private PylosLocation origin;
    private PylosLocation destination;
    private ActionType type;

    /**
     * MOVE: new Action(MOVE, AbstractSphere, null, NotNull)
     * REMOVE: new ACTION(REMOVE, AbstractSphere, NotNull, null)
     *
     * @param type
     * @param abstractSphere
     * @param origin
     * @param destination
     */
    public Action(ActionType type, AbstractSphere abstractSphere, PylosLocation origin, PylosLocation destination) {
        //If the type is MOVE than the location will be where the abstractSphere will be moved to

        this.abstractSphere = abstractSphere;
        this.origin = origin;
        this.destination = destination;
        this.type = type;
    }

    public Action(ActionType type) {
        this.type = type;
    }

    public AbstractSphere getAbstractSphere() {
        return abstractSphere;
    }

    public PylosLocation getOrigin() {
        return origin;
    }

    public PylosLocation getDestination() {
        return destination;
    }


    public ActionType getType() {
        return type;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Action)) return false;
        Action action = (Action) o;
        return Objects.equals(origin, action.origin) &&
                Objects.equals(destination, action.destination) &&
                type == action.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, destination, type);
    }

}

enum ActionType {
    MOVE, REMOVE, PASS, DRAW, COMPLETED
}

abstract class AbstractSphere {

    public AbstractSphere() {
    }

    abstract PylosSphere getSphere();
}

class BoardSphere extends AbstractSphere {

    PylosSphere sphere;

    public BoardSphere(PylosSphere sphere) {
        this.sphere = sphere;
    }

    @Override
    PylosSphere getSphere() {
        return sphere;
    }
}

class ReserveSphere extends AbstractSphere {

    private static HashMap<PylosPlayerColor, ReserveSphere> instance = new HashMap<>();

    PylosPlayerColor player;
    PylosBoard board;
    PylosSphere sphere;

    public ReserveSphere(PylosBoard board, PylosPlayerColor player) {
        this.board = board;
        this.player = player;
    }

    @Override
    PylosSphere getSphere() {
        if (sphere == null) sphere = board.getReserve(player);
        return sphere;
    }

    static ReserveSphere getInstance(PylosBoard board, PylosPlayerColor player) {
        if (!instance.containsKey(player)) {
            ReserveSphere newSphere = new ReserveSphere(board, player);
            instance.put(player, newSphere);
            return newSphere;
        } else return instance.get(player);
    }
}