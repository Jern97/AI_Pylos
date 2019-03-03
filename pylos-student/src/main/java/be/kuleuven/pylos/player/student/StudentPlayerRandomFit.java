package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.util.*;
import java.util.stream.Collectors;

public class StudentPlayerRandomFit extends PylosPlayer {


    private static final int SEARCH_DEPTH = 1;

    private HashMap<Integer, Double> depthScore = new HashMap<Integer, Double>();
    boolean initialized = false;
    private HashMap<PylosPlayerColor, List<Action>> allPossibleMoves = new HashMap<>(2);
    private HashMap<PylosPlayerColor, List<Action>> allPossibleRemoves = new HashMap<>(2);
    private List<PylosSphere> otherBoardSpheresBefore = new LinkedList<>();
    private double worstDepthScore = Double.MAX_VALUE;
    PylosGameSimulator pGameSim;

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        initialise(game, board);
        //Initialize the all of the possible moves in the starting state of the game for both players. After each action the lists will be adjusted.
        //Also get a game simulator object
        //Get the best action
        Action bestAction;
        List<Action> exclude = new ArrayList<>();
        do {
            bestAction = getNextAction(board, this, 0, exclude);
            exclude.add(bestAction);
        } while (bestAction != null && game.moveSphereIsDraw(bestAction.getAbstractSphere().getSphere(), bestAction.getDestination()));

        if (bestAction == null)
            bestAction = getNextAction(board, this, 0, null);


        game.moveSphere(bestAction.getAbstractSphere().getSphere(), bestAction.getDestination());
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        initialise(game, board);
        Action bestAction;
        List<Action> exclude = new ArrayList<>();
        do {
            bestAction = getNextAction(board, this, 0, exclude);
            exclude.add(bestAction);
        } while (bestAction != null && game.removeSphereIsDraw(bestAction.getAbstractSphere().getSphere()));

        if (bestAction == null)
            bestAction = getNextAction(board, this, 0, null);

        game.removeSphere(bestAction.getAbstractSphere().getSphere());
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        initialise(game, board);

        Action bestAction;
        List<Action> exclude = new ArrayList<>();
        boolean loopyBOI = true;

        do {
            bestAction = getNextAction(board, this, 0, exclude);
            if (bestAction == null) {
                game.pass();
                break;
            }
            exclude.add(bestAction);
            if (bestAction.getType() == ActionType.PASS) {
                loopyBOI = game.passIsDraw();
            } else {
                loopyBOI = game.removeSphereIsDraw(bestAction.getAbstractSphere().getSphere());
            }
        } while (loopyBOI);

        if (!loopyBOI) {
            if (bestAction.getType() == ActionType.PASS) {
                game.pass();
            } else {
                game.removeSphere(bestAction.getAbstractSphere().getSphere());
            }
        }
    }

    private void initialise(PylosGameIF game, PylosBoard board) {
        worstDepthScore = Double.MAX_VALUE;
        allPossibleMoves.clear();
        allPossibleRemoves.clear();
        allPossibleMoves.put(this.PLAYER_COLOR, generateAllMoves(board, this));
        allPossibleMoves.put(OTHER.PLAYER_COLOR, generateAllMoves(board, OTHER));
        allPossibleRemoves.put(this.PLAYER_COLOR, generateAllRemoves(board, this));
        allPossibleRemoves.put(OTHER.PLAYER_COLOR, generateAllRemoves(board, this.OTHER));
        pGameSim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
    }

    private void modifyAvailableMoves(PylosBoard board, Action action, PylosPlayer player, boolean reverse) {

        if (action.getType() == ActionType.MOVE) {
            if (!reverse) {

                //remove all of the moves that has one of the lower locations as origin
                Optional<PylosSquare> square = Arrays.stream(board.getAllSquares()).filter(s -> s.getTopLocation() == action.getDestination()).findAny();
                if (square.isPresent()) {
                    List<PylosLocation> squareLocations = Arrays.asList(square.get().getLocations());
                    allPossibleMoves.forEach((key, value) -> value.removeIf(a -> squareLocations.contains(a.getOrigin())));
                }

                //remove all of the moves with that destination
                allPossibleMoves.forEach(((key, value) ->
                        value.removeIf(a -> a.getDestination() == action.getDestination())
                ));

                //Add all possible destinations for this ball as origin
                Arrays.stream(board.getLocations()).filter(l -> l.Z > action.getDestination().Z && l.isUsable()).forEach(l -> allPossibleMoves.get(player.PLAYER_COLOR).add(new Action(ActionType.MOVE, new BoardSphere(action.getAbstractSphere().getSphere()), action.getDestination(), l)));

                //Add a remove to the player to which the ball belongs
                allPossibleRemoves.get(player.PLAYER_COLOR).add(new Action(ActionType.REMOVE, new BoardSphere(action.getAbstractSphere().getSphere()), action.getDestination(), null));

                //Add move possibilities to top locations that may have become available
                List<PylosSphere> lowerSpheres = Arrays.stream(board.getSpheres()).filter(s -> !s.isReserve() && s.getLocation().Z <= action.getDestination().Z && s.canMove()).collect(Collectors.toList());
                getTopLocations(board, action.getDestination()).stream().filter(PylosLocation::isUsable).forEach(l -> {
                    allPossibleMoves.forEach((key, value) -> value.add(new Action(ActionType.MOVE, new ReserveSphere(board, key), null, l)));
                    lowerSpheres.forEach(s -> allPossibleMoves.get(s.PLAYER_COLOR).add(new Action(ActionType.MOVE, new BoardSphere(s), s.getLocation(), l)));
                });

            } else {
                modifyAvailableMoves(board, new Action(ActionType.REMOVE, new BoardSphere(action.getAbstractSphere().getSphere()), action.getDestination(), null), player, false);
            }

        } else if (action.getType() == ActionType.REMOVE) {

            if (!reverse) {

                //remove all of the moves with this as origin
                allPossibleMoves.forEach(((key, value) ->
                        value.removeIf(a -> a.getOrigin() == action.getOrigin())
                ));

                //add moves with this as destination
                List<PylosSphere> lowerSpheres = Arrays.stream(board.getSpheres()).filter(s -> !s.isReserve() && s.getLocation().Z < action.getOrigin().Z && s.canMove()).collect(Collectors.toList());
                //from reserve
                allPossibleMoves.forEach((key, value) -> value.add(new Action(ActionType.MOVE, new ReserveSphere(board, key), null, action.getOrigin())));
                //from board
                lowerSpheres.forEach(s -> allPossibleMoves.get(s.PLAYER_COLOR).add(new Action(ActionType.MOVE, new BoardSphere(s), s.getLocation(), action.getOrigin())));


                //Remove the remove action from the player to which that ball belongs
                allPossibleRemoves.get(player.PLAYER_COLOR).remove(action);

                //Remove all of the moves that have one of the top locations as destination
                List<PylosLocation> topLocations = getTopLocations(board, action.getOrigin());
                allPossibleMoves.forEach((key, value) -> value.removeAll(value.stream().filter(a -> topLocations.contains(a.getDestination())).collect(Collectors.toList())));


            } else {
                modifyAvailableMoves(board, new Action(ActionType.MOVE, new ReserveSphere(board, player.PLAYER_COLOR), null, action.getOrigin()), player, false);
            }

        }

    }

    private void modifyAvailableMoves(PylosBoard board, Action action, PylosPlayer player, boolean reverse, boolean print) {
        modifyAvailableMoves(board, action, player, reverse);
    }

    private List<Action> generateAllMoves(PylosBoard board, PylosPlayer player) {
        List<Action> allMoves = new ArrayList<>();
        Arrays.stream(board.getLocations()).filter(PylosLocation::isUsable).forEach(l -> {
                    Arrays.stream(board.getSpheres(player)).filter(s -> !s.isReserve() && s.getLocation().Z < l.Z && s.canMove() && !s.getLocation().isBelow(l)).forEach(s -> allMoves.add(new Action(ActionType.MOVE, new BoardSphere(s), s.getLocation(), l)));
                    allMoves.add(new Action(ActionType.MOVE, new ReserveSphere(board, player.PLAYER_COLOR), null, l));
                }
        );
        Collections.shuffle(allMoves);
        return allMoves;
    }

    private List<Action> generateAllRemoves(PylosBoard board, PylosPlayer player) {
        List<Action> allRemoves = new ArrayList<>();
        Arrays.stream(board.getSpheres(player)).filter(PylosSphere::canRemove).forEach(s -> allRemoves.add(new Action(ActionType.REMOVE, new BoardSphere(s), s.getLocation(), null)));
        Collections.shuffle(allRemoves);
        return allRemoves;
    }

    private List<PylosLocation> getTopLocations(PylosBoard board, PylosLocation location) {
        return Arrays.stream(board.getLocations()).filter(l -> l.Z - location.Z == 1 && location.isBelow(l)).collect(Collectors.toList());
    }


    private PylosSphere getBestRemove(PylosBoard board, PylosPlayer player, boolean force) {
        /*
        Generate all possible removes
         */
        //Identify all the moves that the current player can do
        Optional<PylosSphere> sphere = Arrays.stream(board.getSpheres(player)).filter(PylosSphere::canRemove).min(new Comparator<PylosSphere>() {
            @Override
            public int compare(PylosSphere o1, PylosSphere o2) {
                return o1.getLocation().Z - o2.getLocation().Z;
            }
        });

        return sphere.orElse(null);
    }

    private List<Action> generateMoves(PylosBoard board, PylosPlayer player) {

        List<PylosLocation> usableLocations = Arrays.stream(board.getLocations()).filter(PylosLocation::isUsable).collect(Collectors.toList());
        List<Action> allActions = new ArrayList<>();

        //First generate all of the moves that can be done form the reserve to a spot on the field
        if (board.getReservesSize(player) > 0) {
            ReserveSphere reserveSphere = ReserveSphere.getInstance(board, player.PLAYER_COLOR);
            for (PylosLocation usableLocation : usableLocations) {
                allActions.add(new Action(ActionType.MOVE, reserveSphere, null, usableLocation));
            }
        }

        //In the initial state of the game there are no spheres on the field and so it is not possible
        //to move a abstractSphere on the field to a higher level

        return allActions;
    }

    private Action getNextAction(PylosBoard board, PylosPlayer currentPlayer, int depth, List<Action> exclude) {
        PylosGameState state = pGameSim.getState();
        Action bestAction = null;
        List<Action> deepActions = new ArrayList<>();
        List<Action> actions = new ArrayList<>();
        if (currentPlayer != this) {
            depth++;
        }

        switch (state) {

            case MOVE: {

                //actions = generateAllMoves(board, currentPlayer);
                actions = new ArrayList<>(allPossibleMoves.get(currentPlayer.PLAYER_COLOR));
                if (exclude != null) actions.removeAll(exclude);
                //Iterate over every possible move
                for (Action move : actions) {
                    //Simulate what happens with each move

                    modifyAvailableMoves(board, move, currentPlayer, false, true);
                    pGameSim.moveSphere(move.getAbstractSphere().getSphere(), move.getDestination());

                    if (depth < SEARCH_DEPTH) {

                        Action deepAction = null;

                        //We don't need to switch players if the current player needs to remove a sphere
                        if (pGameSim.getState() == PylosGameState.REMOVE_FIRST)
                            deepAction = getNextAction(board, currentPlayer, depth, null);
                        else deepAction = getNextAction(board, currentPlayer.OTHER, depth, null);

                        worstDepthScore = (worstDepthScore < deepAction.getScore()) ? worstDepthScore : deepAction.getScore();
                    } else {
                        double score = calculateScore(pGameSim.getState(), board, currentPlayer);
                        move.setScore(score);
                        if (score > worstDepthScore) return move;
                    }

                    //Undo the move
                    if (move.getAbstractSphere() instanceof ReserveSphere)
                        pGameSim.undoAddSphere(move.getAbstractSphere().getSphere(), state, currentPlayer.PLAYER_COLOR);
                    else
                        pGameSim.undoMoveSphere(move.getAbstractSphere().getSphere(), move.getOrigin(), state, currentPlayer.PLAYER_COLOR);

                    modifyAvailableMoves(board, move, currentPlayer, true, true);

                }

                break;
            }
            case REMOVE_FIRST: {

                //actions = generateAllRemoves(board, currentPlayer);
                actions = new ArrayList<>(allPossibleRemoves.get(currentPlayer.PLAYER_COLOR));
                if (exclude != null) actions.removeAll(exclude);

                for (Action remove : actions) {
                    pGameSim.removeSphere(remove.getAbstractSphere().getSphere());
                    modifyAvailableMoves(board, remove, currentPlayer, false);

                    if (depth < SEARCH_DEPTH) {
                        //We don't need to switch players because we will get into REMOVE_SECOND state
                        deepActions.add(getNextAction(board, currentPlayer, depth, null));
                    } else {
                        remove.setScore(calculateScore(pGameSim.getState(), board, currentPlayer));
                    }

                    pGameSim.undoRemoveFirstSphere(remove.getAbstractSphere().getSphere(), remove.getOrigin(), state, currentPlayer.PLAYER_COLOR);
                    modifyAvailableMoves(board, remove, currentPlayer, true);

                }

                break;
            }
            case REMOVE_SECOND: {

                //actions = generateAllRemoves(board, currentPlayer);
                actions = new ArrayList<>(allPossibleRemoves.get(currentPlayer.PLAYER_COLOR));
                actions.add(new Action(ActionType.PASS));
                if (exclude != null) actions.removeAll(exclude);


                for (Action remove : actions) {
                    if (remove.getType() == ActionType.REMOVE) {
                        pGameSim.removeSphere(remove.getAbstractSphere().getSphere());
                        modifyAvailableMoves(board, remove, currentPlayer, false);

                        //We need to switch players
                        if (depth < SEARCH_DEPTH) {
                            deepActions.add(getNextAction(board, currentPlayer.OTHER, depth, null));
                        } else {
                            remove.setScore(calculateScore(pGameSim.getState(), board, currentPlayer));
                        }
                        pGameSim.undoRemoveSecondSphere(remove.getAbstractSphere().getSphere(), remove.getOrigin(), state, currentPlayer.PLAYER_COLOR);
                        modifyAvailableMoves(board, remove, currentPlayer, true);

                    } else {
                        pGameSim.pass();
                        //We need to switch players
                        if (depth < SEARCH_DEPTH) {
                            deepActions.add(getNextAction(board, currentPlayer.OTHER, depth, null));
                        } else {
                            remove.setScore(calculateScore(pGameSim.getState(), board, currentPlayer));
                        }
                        pGameSim.undoPass(PylosGameState.REMOVE_SECOND, currentPlayer.PLAYER_COLOR);
                    }

                }

                break;
            }

            case DRAW: {
                //We need to get this as a negative infinity score
                Action action = new Action(ActionType.DRAW);
                action.setScore(Double.MIN_VALUE);
                actions.add(action);
            }
            case COMPLETED: {
                //We need to get this as positive infinity score
                Action action = new Action(ActionType.COMPLETED);
                action.setScore(Double.MAX_VALUE);
                return (action);
            }

        }


        if (actions.isEmpty()) return null;

        Action worstAction;
        if (currentPlayer == this) {
            //Als het deze speler is dan zijn we geinteresseerd in de move geassocieerd met de slechtste deep move
            worstAction = getWorstActionFromList(deepActions, currentPlayer);
            bestAction = actions.get(deepActions.indexOf(worstAction));
            bestAction.setScore(worstAction.getScore());

        } else {
            if (depth >= SEARCH_DEPTH) {
                bestAction = getBestActionFromList(actions, currentPlayer);
            } else {
                worstAction = getBestActionFromList(deepActions, currentPlayer);
                bestAction = actions.get(deepActions.indexOf(worstAction));
                bestAction.setScore(worstAction.getScore());
            }
        }

        return bestAction;
    }

    private double calculateScore(PylosGameState state, PylosBoard board, PylosPlayer currentPlayer) {
        switch (state) {
            case DRAW:
                return Double.MIN_VALUE;
            case COMPLETED:
                return Double.MAX_VALUE;
            default:
                return board.getReservesSize(currentPlayer) - board.getReservesSize(currentPlayer.OTHER);
        }
    }


    private Action getBestActionFromList(List<Action> actions, PylosPlayer currentPlayer) {
        Optional<Action> optional = actions.stream().max(new ScoreComparator());
        //System.out.println("Best action is: " + optional.get().getScore() + " out of " + actions.toString());
        return optional.orElse(null);
    }

    private Action getWorstActionFromList(List<Action> actions, PylosPlayer currentPlayer) {
        Optional<Action> optional = actions.stream().min(new ScoreComparator());
        //System.out.println("Worst action is: " + optional.get().getScore() + " out of " + actions.toString());
        return optional.orElse(null);
    }
}

class ScoreComparator implements Comparator<Action> {
    @Override
    public int compare(Action o1, Action o2) {
        double difference = o1.getScore() - o2.getScore();
        if (difference < 0) return -1;
        else if (difference > 0) return +1;
        else return 0;
    }
}

class Action {

    private AbstractSphere abstractSphere;
    private PylosLocation origin;
    private PylosLocation destination;
    private double score;
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

    public double getScore() {
        return score;
    }

    public ActionType getType() {
        return type;
    }

    public void setScore(double score) {
        this.score = score;
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

    @Override
    public String toString() {
        return String.valueOf(score);
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