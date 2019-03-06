package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.*;
import be.kuleuven.pylos.player.PylosPlayer;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Ine on 25/02/2015.
 */
/*
    regeltjes
        - vierkantjes met 1 balletje  in en rest leeg = 1 punt
        - vierkantjes met 2 balletjes in en rest leeg = 2 punten
        - vierkantjes met 3 balletjes in en rest leeg = 3 punten

        - hoogtes van de balletjes

        - aantal spheres die je kan bewegen op het bord (dus die je kan hoger zetten)

        - aantal reserve spheres

     huidige situatie beoordelen

 */

public class StudentPlayerBestFit extends PylosPlayer {

    /* deze Speler vs pylosBestFit, met parameters X X X  | wint .. %
                                                   20   10  1   |   91.9
                                                   20   10  5   |   70.90
                                                   25   10  1   |   90.40
                                                   25   5   1   |   92.80
                                                   30   5   1   |   93.9
                                                   35   5   1   |   94.9    -> best
                                                   50   5   1   |   93.80
                                                   50   30  10  |   68.8
                                                   35   5   2   |   93.8
     */
    final static int reserveSpheresWeight = 35;
    final static int freeSpheresOnBoardWeight = 5;
    final static int squaresScoreWeight = 1;

    Random r = new Random();

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {

        // waar zetten
        ArrayList<Move> possibleMoves = getPossibleMoves(game, board);

        // punten awarden adhv
        for (Move move : possibleMoves) {

            //simuleert de move, geeft de move punten, en undo't de move
            move.awardPointsToMove(this,game,board);
        }

        // move met de beste score ga ik houden
        Move besteMove = possibleMoves.stream().max(Comparator.comparing(Move::getScore)).orElse(null);

        game.moveSphere(besteMove.getPylosSphere(), besteMove.getTo());


    }

    private ArrayList<Move> getPossibleMoves(PylosGameIF game, PylosBoard board) {

        ArrayList<Move> possibleMoves = new ArrayList<>();

        // get all usable locations
        ArrayList<PylosLocation> allLocations = new ArrayList<>(Arrays.asList(board.getLocations()));
        List<PylosLocation> usableLocations = allLocations.stream().filter(PylosLocation::isUsable).collect(Collectors.toList());

        // get all balletjes that are free & deze die op bord liggen
        List<PylosSphere> ballsOnBoardMovable = Arrays.stream(board.getSpheres(this)).filter(PylosSphere::canMove).filter(s -> s.canMove() && s.getLocation() != null).collect(Collectors.toList());

        //der gaat altijd een reserveballetje zijn, aangezien dat als je nul balletjes hebt je verloren bent
        PylosSphere reserveSphere = board.getReserve(this);

        //moves toevoegen aan de lijst
        //reeks van moves a: reserveballetje naar bord
        for (PylosLocation usableLocation : usableLocations) {
            possibleMoves.add(new Move(this, null, usableLocation, reserveSphere));
        }

        //reeks van move b:
        // alle balletjes die vrij zijn, kunnen we naar een andere locatie brengen
        // niet elk balletje dat vrij is, kunnen we naar al de locaties doen
        // elke locatie die hoger is, usable is, en waar het balletje niet op steunt

        for (PylosSphere pylosSphere : ballsOnBoardMovable) {

            PylosLocation ballLocation = pylosSphere.getLocation();

            for (PylosLocation usableLocation : usableLocations) {

                // we moeten de move kunnen doen
                if (pylosSphere.canMoveTo(usableLocation)) {
                    possibleMoves.add(new Move(this, ballLocation, usableLocation, pylosSphere));
                }

            }

        }

        return possibleMoves;
    }

    public int calculateScore(PylosGameSimulator simulator, PylosPlayer player, PylosGameIF game, PylosBoard board) {

        // aantal reserve balletjes - deze die aan de zijkant liggen
        int myReserveSpheres = board.getReservesSize(player);
        int yourReserveSpheres = board.getReservesSize(player.PLAYER_COLOR.other());

        // aantal vierkantjes waar er geen van de andere in ligt
        PylosSquare[] allSquares = board.getAllSquares();
        int myScoreSquare = calculateScoreSquares(allSquares, player.PLAYER_COLOR);
        int yourScoreSquare = calculateScoreSquares(allSquares, player.PLAYER_COLOR.other());

        // aantal vrije balletjes
        int myFreeSpheres =
                Arrays.stream(board.getSpheres(this))
                        .filter(PylosSphere::canMove)
                        .filter(s -> s.canMove() && s.getLocation() != null)
                        .collect(Collectors.toList()).size();

        int yourFreeSpheres =
                Arrays.stream(board.getSpheres(this.PLAYER_COLOR.other()))
                        .filter(PylosSphere::canMove)
                        .filter(s -> s.canMove() && s.getLocation() != null)
                        .collect(Collectors.toList()).size();


        int totaalScore =       (myReserveSpheres - yourReserveSpheres ) * reserveSpheresWeight
                            +   (myFreeSpheres - yourFreeSpheres )       * freeSpheresOnBoardWeight
                            +   (myScoreSquare - yourScoreSquare )       * squaresScoreWeight;

        return totaalScore;
    }

    /**
     *
     * @param allSquares alle squares op het bord
     * @param player_color de speler waarvoor we moeten checken
     * @return aantal totaalpunten van alle squares op het bord
     */
    private int calculateScoreSquares(PylosSquare[] allSquares, PylosPlayerColor player_color) {

        int myScore = 0;
        for (PylosSquare square : allSquares) {
            myScore += calculateScoreOneSquare(square, player_color);
        }

        return myScore;
    }

    /**
     *  2 punt in situatie : x .   | 5 punten als   : x x | 10 punten: x x | 17 punten: x x
     *                       . .   |                : . . |            x . |            x x
     */
    private int calculateScoreOneSquare(PylosSquare square, PylosPlayerColor player_color) {

        int aantalBalletjesInSquare = 0;

        for (PylosLocation location : square.getLocations()) {
            if( location.getSphere() != null){
                if (location.getSphere().PLAYER_COLOR == player_color.other()) {
                    return 0;
                } else {
                    aantalBalletjesInSquare++;
                }
            }
        }
        //kwadrateren, want 3 van mezelf en een lege bal moet veel meer waard zijn dan 1 bal
        return aantalBalletjesInSquare * aantalBalletjesInSquare + 1;
    }


    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        // 1 terugpakken of 2 terugpakken
        List<PylosSphere> movableSpheres = Arrays.stream(board.getSpheres(this)).filter(s -> s.canMove() && s.getLocation() != null).collect(Collectors.toList());
        PylosSphere ps = movableSpheres.get(r.nextInt(movableSpheres.size()));

        game.removeSphere(ps);

    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        game.pass();
    }

    public void getCurrentPunten(PylosGameIF game, PylosBoard board) {

        // aantal vrije balletjes

    }

    private class Move {

        private PylosPlayer player;
        private PylosLocation from;
        private PylosLocation to;
        private PylosSphere pylosSphere;
        private int score; // hoe goed is deze zet

        public Move(PylosPlayer player, PylosLocation from, PylosLocation to, PylosSphere pylosSphere) {

            this.player = player;
            this.from = from;
            this.to = to;
            this.pylosSphere = pylosSphere;
            score = 0;

        }

        public PylosPlayer getPlayer() {
            return player;
        }

        public PylosLocation getFrom() {
            return from;
        }

        public PylosLocation getTo() {
            return to;
        }

        public PylosSphere getPylosSphere() {
            return pylosSphere;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public void awardPointsToMove(StudentPlayerBestFit studentPlayerBestFit, PylosGameIF game, PylosBoard board) {

            //maak simulator aan
            PylosGameSimulator simulator = new PylosGameSimulator(game.getState(), studentPlayerBestFit.PLAYER_COLOR, board);

            //sla oude gamestate op, is nodig om de zet terug te undo'en
            PylosGameState oldGameState = game.getState();

            // doe de zet
            simulator.moveSphere(this.getPylosSphere(), this.getTo());

            // bereken score en geef punten
            int scoreVanMove = calculateScore(simulator, studentPlayerBestFit, game, board);
            this.setScore(scoreVanMove);

            // undo move
            if (this.getFrom() == null) {
                simulator.undoAddSphere(this.getPylosSphere(), oldGameState, studentPlayerBestFit.PLAYER_COLOR);
            } else {
                simulator.undoMoveSphere(this.getPylosSphere(), this.getFrom(), oldGameState, studentPlayerBestFit.PLAYER_COLOR);
            }


        }
    }

}


