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

    Random r = new Random();

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        // waar zetten

        ArrayList<Move> possibleMoves = getPossibleMoves(game, board);

        // punten awarden adhv
        for (Move move : possibleMoves) {

            PylosGameSimulator simulator = new PylosGameSimulator(game.getState(), this.PLAYER_COLOR, board);

            PylosGameState oldGameState = game.getState();

            simulator.moveSphere(move.getPylosSphere(), move.getTo());

            move.setScore(calculateScore(simulator, this, game, board));

            if (move.getFrom() == null) {
                simulator.undoAddSphere(move.getPylosSphere(), oldGameState, this.PLAYER_COLOR);
            } else {
                simulator.undoMoveSphere(move.getPylosSphere(), move.getFrom(), oldGameState, this.PLAYER_COLOR);
            }
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

        // aantal reserve balletjes
        int myReserveSpheres = board.getReservesSize(player);
        int yourReserveSpheres = board.getReservesSize(player.PLAYER_COLOR.other());


        // aantal vierkantjes waar er geen van de andere in ligt
        int myScoreSquare = 0;
        PylosSquare[] squares = board.getAllSquares();
        for (PylosSquare square : squares) {

            boolean allesVanMij = true;

            int aantalVanMij = 0;
            for (PylosLocation location : square.getLocations()) {

                if (location.getSphere() == null) {
                    // lege locatie
                } else if (location.getSphere().PLAYER_COLOR == player.PLAYER_COLOR) {
                    aantalVanMij++;
                } else {
                    allesVanMij = false;
                }

            }

            if(allesVanMij){
                myScoreSquare += aantalVanMij;
            }

        }

        int yourScoreSquare = 0;
        for (PylosSquare square : squares) {

            boolean allesVanJou = true;

            int aantalVanJou = 0;
            for (PylosLocation location : square.getLocations()) {

                if (location.getSphere() == null) {
                    // lege locatie
                } else if (location.getSphere().PLAYER_COLOR.other() == player.PLAYER_COLOR.other()) {
                    aantalVanJou++;
                } else {
                    allesVanJou = false;
                }

            }

            if(allesVanJou){
                yourScoreSquare += aantalVanJou;
            }
        }



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


        int totaalScore = myFreeSpheres * 20
                + myFreeSpheres * 2 // die al op het bord liggen
                - yourFreeSpheres * 2
                + myScoreSquare * myScoreSquare // ^2
                - yourFreeSpheres * yourFreeSpheres;

        return totaalScore;
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
    }

}


