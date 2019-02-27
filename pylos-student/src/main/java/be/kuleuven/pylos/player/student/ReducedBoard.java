package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosLocation;
import be.kuleuven.pylos.game.PylosPlayerColor;
import be.kuleuven.pylos.game.PylosSphere;

import java.util.HashMap;

public class ReducedBoard {

    private long state;
    private byte lightReserve;
    private byte darkReserve;


    //Gekopieerde code van Jan Christiaens
    private final static long[][][][] BIT_MASK_FOR_OR;    // [z][x][y][0/1 color]
    private final static long[][][][] BIT_MASK_FOR_AND;

    static {
        int index = 0;
        BIT_MASK_FOR_OR = new long[4][][][];
        BIT_MASK_FOR_AND = new long[4][][][];
        for (int z = 0; z < 4; z++) {
            BIT_MASK_FOR_OR[z] = new long[4 - z][][];
            BIT_MASK_FOR_AND[z] = new long[4 - z][][];
            for (int x = 0; x < BIT_MASK_FOR_OR[z].length; x++) {
                BIT_MASK_FOR_OR[z][x] = new long[4 - z][];
                BIT_MASK_FOR_AND[z][x] = new long[4 - z][];
                for (int y = 0; y < BIT_MASK_FOR_OR[z].length; y++) {
                    BIT_MASK_FOR_OR[z][x][y] = new long[2];
                    BIT_MASK_FOR_OR[z][x][y][0] = 1l << index;
                    BIT_MASK_FOR_OR[z][x][y][1] = 1l << (index + 1);
                    BIT_MASK_FOR_AND[z][x][y] = new long[2];
                    BIT_MASK_FOR_AND[z][x][y][0] = ~BIT_MASK_FOR_OR[z][x][y][0];
                    BIT_MASK_FOR_AND[z][x][y][1] = ~BIT_MASK_FOR_OR[z][x][y][1];
                    index += 2;
                }
            }
        }
    }


    public ReducedBoard(PylosBoard pylosBoard) {
        state = pylosBoard.toLong();
        lightReserve = (byte) pylosBoard.getReservesSize(PylosPlayerColor.LIGHT);
        darkReserve = (byte) pylosBoard.getReservesSize(PylosPlayerColor.DARK);
    }

    //Copy constructor
    public ReducedBoard(ReducedBoard toCopy) {
        state = toCopy.state;
        lightReserve = toCopy.lightReserve;
        darkReserve = toCopy.darkReserve;
    }

    //Checken als een sphere op een bepaalde locatie staat
    public boolean containsSphere(int x, int y, int z, PylosPlayerColor color) {
        long result = BIT_MASK_FOR_AND[z][x][y][color.ordinal()] & state;
        return result != 0;
    }

    //Checken als een sphere op een bepaalde locatie staat
    public PylosPlayerColor containsSphere(int x, int y, int z) {
        if ((BIT_MASK_FOR_AND[z][x][y][0] & state) != 0) return PylosPlayerColor.LIGHT;
        if ((BIT_MASK_FOR_AND[z][x][y][1] & state) != 0) return PylosPlayerColor.DARK;
        return null;
    }

    public void setSphere(int x, int y, int z, PylosPlayerColor color) {
        state |= BIT_MASK_FOR_OR[z][x][y][color.ordinal()];
        if (color == PylosPlayerColor.LIGHT) lightReserve--;
        else darkReserve--;
    }

    public void removeSphere(int x, int y, int z, PylosPlayerColor color) {
        state &= BIT_MASK_FOR_AND[z][x][y][color.ordinal()];
        if (color == PylosPlayerColor.LIGHT) lightReserve++;
        else darkReserve++;
    }

    private boolean isSupported(int x, int y, int z) {
        if (z == 0) return true;
        //Checken als er onder een bepaalde coordinaat 4 lichte of zwarte spheres liggen.
        return ((BIT_MASK_FOR_AND[z][x][y][0] & state) != 0 || (BIT_MASK_FOR_AND[z][x][y][1] & state) != 0) &&
                ((BIT_MASK_FOR_AND[z][x+1][y][0] & state) != 0 || (BIT_MASK_FOR_AND[z][x+1][y][1] & state) != 0) &&
                ((BIT_MASK_FOR_AND[z][x][y+1][0] & state) != 0 || (BIT_MASK_FOR_AND[z][x][y+1][1] & state) != 0) &&
                ((BIT_MASK_FOR_AND[z][x+1][y+1][0] & state) != 0 || (BIT_MASK_FOR_AND[z][x+1][y+1][1] & state) != 0);

    }

    //Alle moves overlopen waarbij een element uit de reserve kan worden genomen
    public long[] getAllReserveMoves(PylosPlayerColor player, PylosBoard board) {
        //Alle velden overlopen
        for (int z = 0; z < 4; z++) {
            for (int y = 0; y < 4 - z; y++) {
                for (int x = 0; x < 4 - x; x++) {
                    if (containsSphere(x, y, z) != null && isSupported(x, y, z)) {

                    }
                }
            }
        }
    }


}
