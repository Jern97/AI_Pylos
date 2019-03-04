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

        -

     huidige situatie beoordelen

 */

public class StudentPlayerBestFit extends PylosPlayer {

    @Override
    public void doMove(PylosGameIF game, PylosBoard board) {
        // waar zetten

        ArrayList<Move> possibleMoves = getPossibleMoves(game,board);

        // controleren als de move doen iets uitmaakt
        for (Move possibleMove : possibleMoves) {

            possibleMove.calculateResultMoves(board);

        }



    }

    private ArrayList<Move> getPossibleMoves(PylosGameIF game, PylosBoard board) {

        ArrayList<Move> possibleMoves = new ArrayList<>();

        // get all usable locations
        ArrayList<PylosLocation> allLocations = new ArrayList<>(Arrays.asList(board.getLocations()));
        List<PylosLocation> usableLocations = allLocations.stream().filter(PylosLocation::isUsable).collect(Collectors.toList());

        // get all balletjes that are free & deze die op bord liggen
        List<PylosSphere> ballsOnBoardMovable = Arrays.stream(board.getSpheres(this)).filter(PylosSphere::canMove).filter(s -> s.canMove() && s.getLocation() != null ).collect(Collectors.toList());

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
                if(pylosSphere.canMoveTo(usableLocation)){
                    possibleMoves.add(new Move(this, ballLocation, usableLocation, pylosSphere));
                }

            }

        }

        return possibleMoves;
    }

    @Override
    public void doRemove(PylosGameIF game, PylosBoard board) {
        // 1 terugpakken of 2 terugpakken
    }

    @Override
    public void doRemoveOrPass(PylosGameIF game, PylosBoard board) {
        //
    }

    public void getCurrentPunten(PylosGameIF game, PylosBoard board){

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
        }


    }

}


