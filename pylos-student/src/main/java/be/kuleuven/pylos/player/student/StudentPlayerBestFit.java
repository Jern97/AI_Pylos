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
        //Actions opvragen
        List<Action> actions = selectBestMove(sim, board, game, 0);
        //Action met hoogste score selecteren
        Action bestAction = actions.stream().max(Comparator.comparing(Action::getScore)).orElseThrow(NoSuchElementException::new);

        game.moveSphere(bestAction.getSphere(), bestAction.getTo());
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        List<Action> actions = selectBestMove(sim, board, game, 0);
        Action bestAction = actions.stream().max(Comparator.comparing(Action::getScore)).orElseThrow(NoSuchElementException::new);

        game.removeSphere(bestAction.getSphere());
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        PylosGameSimulator sim = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);
        List<Action> actions = selectBestMove(sim, board, game, 0);
        Action bestAction = actions.stream().max(Comparator.comparing(Action::getScore)).orElseThrow(NoSuchElementException::new);

        if (bestAction.getSphere() != null) game.removeSphere(bestAction.getSphere());
        else game.pass();
    }

    private List<Action> selectBestMove(PylosGameSimulator sim, PylosBoard board, PylosGameIF game, int depth) {

        /*
            Overloopt recursief alle mogelijkheden tot op een bepaalde diepte en gebruikt minimax om de beste action uit te kiezen.
         */

        PylosPlayerColor currentColor = sim.getColor();

        if (sim.getState() == PylosGameState.MOVE) {
            /*
                Er wordt gevraagd om een sphere te moven
             */
            List<Action> actions = generateActions(board, currentColor, game, depth);

            for (Action m : actions) {
                //Inverse move opslaan om het bord te kunnen herstellen
                Action reverseAction = new Action(m.getSphere(), m.getSphere().getLocation(), currentColor);

                sim.moveSphere(m.getSphere(), m.getTo());

                if (sim.getState() == PylosGameState.REMOVE_FIRST) {
                    //Zelfde speler blijft aan de beurt
                    m.addChildren(selectBestMove(sim, board, game, depth));
                } else {
                    //Het is de beurt aan de andere
                    if (depth < SEARCH_DEPTH && sim.getState() != PylosGameState.COMPLETED) {
                        //Target depth is nog niet bereikt: dieper zoeken
                        m.addChildren(selectBestMove(sim, board, game, depth + 1));
                    } else {
                        //Target depth is bereikt, bord moet geëvalueerd worden
                        m.setScore(evalBoard(board, currentColor));
                    }
                }
                //Bord herstellen
                if (reverseAction.getTo() == null) {
                    sim.undoAddSphere(reverseAction.getSphere(), PylosGameState.MOVE, currentColor);
                } else
                    sim.undoMoveSphere(reverseAction.getSphere(), reverseAction.getTo(), PylosGameState.MOVE, currentColor);
            }
            return actions;
        }
        if (sim.getState() == PylosGameState.REMOVE_FIRST) {
            /*
                Er wordt gevraagd om een sphere te removen
             */
            List<Action> actions = generateRemoves(board, currentColor, game, depth, false);

            for (Action m : actions) {
                Action reverseAction = new Action(m.getSphere(), m.getSphere().getLocation(), sim.getColor());
                sim.removeSphere(m.getSphere());
                //Dieper zoeken
                m.addChildren(selectBestMove(sim, board, game, depth));

                //Bord herstellen
                sim.undoRemoveFirstSphere(reverseAction.getSphere(), reverseAction.getTo(), PylosGameState.REMOVE_FIRST, currentColor);
            }
            return actions;
        }
        if (sim.getState() == PylosGameState.REMOVE_SECOND) {
            /*
                Er wordt gevraagd om een sphere te removen of te passen
             */
            List<Action> actions = generateRemoves(board, sim.getColor(), game, depth, true);

            for (Action m : actions) {
                Action reverseAction = null;
                //Passen wordt voorgesteld door een move met sphere en location = null
                if (m.getSphere() != null) {
                    reverseAction = new Action(m.getSphere(), m.getSphere().getLocation(), sim.getColor());
                    sim.removeSphere(m.getSphere());
                } else {
                    sim.pass();
                }

                if (depth < SEARCH_DEPTH) {
                    //Target depth is nog niet bereikt, dieper zoeken
                    m.addChildren(selectBestMove(sim, board, game, depth + 1));
                } else {
                    //Bord evalueren en score instellen
                    m.setScore(evalBoard(board, currentColor));
                }

                if (reverseAction != null) {
                    //Sphere verwijderen
                    sim.undoRemoveSecondSphere(reverseAction.getSphere(), reverseAction.getTo(), PylosGameState.REMOVE_SECOND, currentColor);
                } else {
                    //Passen
                    sim.undoPass(PylosGameState.REMOVE_SECOND, currentColor);
                }
            }
            return actions;

        }
        return null;

    }

    private List<Action> generateActions(PylosBoard board, PylosPlayerColor color, PylosGameIF game, int depth) {
        /*
            Er zijn 2 mogelijkheden voor Actions:
            1. Leg een bal vanuit de reserve op een usable location
            2. Leg een bal vanop het bord naar een usable location die hoger is dan de positie van de bal voorlopig
         */
        List<PylosLocation> usableLocations = Arrays.stream(board.getLocations()).filter(PylosLocation::isUsable).collect(Collectors.toList());
        List<Action> actions = new ArrayList<>();

        PylosSphere reserveSphere = board.getReserve(color);
        for (PylosLocation loc : usableLocations) {
            //Als we op depth 0 zitten moet gecheckt worden als de move niet voor een draw zorgt.
            if (depth != 0 || !game.moveSphereIsDraw(reserveSphere, loc)) {
                //Actions toevoegen waar een sphere uit de reserve wordt geplaatst op usable location
                actions.add(new Action(reserveSphere, loc, color));
            }
            //Als de locatie niet op de grond ligt kan het zijn dat we deze kunnen vullen met spheres op het veld
            if (loc.Z > 0) {
                //Selecteer alle spheres die lager dan deze locatie liggen op het bord, kunnen bewegen en niet recht onder de location liggen
                List<PylosSphere> freeSpheresBelow = Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null &&
                        s.getLocation().Z < loc.Z && s.canMove() && !s.getLocation().isBelow(loc)).collect(Collectors.toList());
                for (PylosSphere s : freeSpheresBelow) {
                    //Als we op depth 0 zitten moet gecheckt worden als de move niet voor een draw zorgt.
                    if (depth != 0 || !game.moveSphereIsDraw(s, loc)) {
                        actions.add(new Action(s, loc, color));
                    }
                }
            }
        }
        //Actions shufflen zodat bot niet altijd op dezelfde manier reageert
        Collections.shuffle(actions);
        return actions;
    }

    private List<Action> generateRemoves(PylosBoard board, PylosPlayerColor color, PylosGameIF game, int depth, boolean passAllowed) {
        //Elke Sphere die kan bewegen op het bord kan verwijderd worden
        List<PylosSphere> removableSpheres = Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null && s.canMove()).collect(Collectors.toList());
        List<Action> actions = new ArrayList<>();
        List<Action> drawActions = new ArrayList<>();

        for (PylosSphere s : removableSpheres) {
            //Checken voor draw move kan enkel in de bovenste laag en bij move state.
            if (depth == 0 && game.getState() != PylosGameState.MOVE) {
                if (!game.removeSphereIsDraw(s)) {
                    actions.add(new Action(s, null, color));
                } else {
                    //Actions die voor een draw zorgen ook opvangen in het geval er geen andere zijn
                    drawActions.add(new Action(s, null, color));
                }
            } else {
                actions.add(new Action(s, null, color));
            }
        }
        //Als er gepast mag worden, een extra lege move toevoegen
        if (passAllowed) {
            actions.add(new Action(null, null, color));
        }

        //Als er geen enkele move is die niet voor een draw zorgt worden de draw actions gereturnd
        if (!actions.isEmpty()) {
            //Actions shufflen zodat bot niet altijd op dezelfde manier reageert
            Collections.shuffle(actions);
            return actions;
        } else {
            Collections.shuffle(drawActions);
            return drawActions;
        }
    }

    private int evalBoard(PylosBoard board, PylosPlayerColor color) {

        //Spel is voltooid: sowieso verloren of gewonnen
        if (board.getReservesSize(color) == 0 && board.getReservesSize(color.other()) == 0) {
            return board.getBoardLocation(0, 0, 3).getSphere().PLAYER_COLOR == color ? 99999 : -99999;
        } else if (board.getReservesSize(color) == 0) {
            return -99999;
        } else if (board.getReservesSize(color.other()) == 0) {
            return 99999;
        }

        //Spel is nog bezig

        /*
            3 factoren die meespelen in de evaluatie:
            * Het verschil in reserveSpheres tussen beide spelers
            * Het verschil in aantal Spheres die kunnen bewegen op het veld tussen beide spelers
            * Aan elk vierkant wordt ook een score toegekend indien er niet al een bal van beide spelers ligt
         */
        int nReserves = board.getReservesSize(color);
        int nReservesOpp = board.getReservesSize(color.other());

        int nMovableSpheres = (int) Arrays.stream(board.getSpheres(color)).filter(s -> s.getLocation() != null && s.canMove()).count();
        int nMovableSpheresOpp = (int) Arrays.stream(board.getSpheres(color.other())).filter(s -> s.getLocation() != null && s.canMove()).count();

        int squareFactor = 0;
        int squareFactorOpp = 0;
        for (PylosSquare square : board.getAllSquares()) {
            /*

             */
            if (square.getInSquare(color.other()) == 0) {
                switch (square.getInSquare(color)) {
                    case 1: squareFactor += 1; break;
                    case 2: squareFactor += 4; break;
                    case 3: squareFactor += 16; break;
                    default: squareFactor += 0;
                }
            }

            if (square.getInSquare(color) == 0) {
                switch (square.getInSquare(color.other())) {
                    case 1: squareFactorOpp += 1; break;
                    case 2: squareFactorOpp += 4; break;
                    case 3: squareFactorOpp += 16; break;
                    default: squareFactorOpp += 0;
                }
            }
        }
        //Zero-sum game: verlies voor de tegenstander is winst en omgekeerd. Daarom telkens kijken naar beide spelers
        return 20 * (nReserves - nReservesOpp) + 1 * (squareFactor - squareFactorOpp) + 1 * (nMovableSpheres - nMovableSpheresOpp);
    }

    private class Action {
        /*
            Stelt een actie voor op het bord: moves, removes, pass
         */

        private PylosSphere sphere;
        private PylosLocation from;
        private PylosLocation to;
        private PylosPlayerColor color;
        private int score;
        private boolean hasScore = false;
        private List<Action> children = new ArrayList<>();

        public Action(PylosSphere sphere, PylosLocation to, PylosPlayerColor color) {
            this.sphere = sphere;
            if (sphere != null) this.from = sphere.getLocation();
            this.to = to;
            this.color = color;
        }

        public PylosSphere getSphere() {
            return sphere;
        }

        public PylosLocation getTo() {
            return to;
        }

        public List<Action> getChildren() {
            return children;
        }

        public void setScore(int score) {
            this.score = score;
            this.hasScore = true;
        }

        public int getScore() {
            return score;
        }

        public void addChildren(List<Action> children) {
            //Bij het toevoegen van de kinderen worden hun scores bekeken
            if(children.get(0).getColor() != color){
                /*
                    Kinderen zijn van een andere speler:
                    Scores omkeren (zero sum game) en de minst optimistische score nemen
                 */
                if (!hasScore) {
                    score = children.get(0).getScore() * -1;
                    hasScore = true;
                }
                for (Action m : children) {
                    this.children.add(m);
                    score = Math.min(score, m.getScore() * -1);
                }
            } else {
                /*
                    Kinderen zijn van dezelfde speler:
                    Scores niet omdraaien en de meest optimistische score nemen
                 */

                if (!hasScore) {
                    score = children.get(0).getScore();
                    hasScore = true;
                }
                for (Action m : children) {
                    this.children.add(m);
                    score = Math.max(score, m.getScore());
                }
            }
        }

        //Methode voor debugging
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


