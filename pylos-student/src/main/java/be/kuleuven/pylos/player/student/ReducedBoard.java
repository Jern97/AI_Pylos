package be.kuleuven.pylos.player.student;

import be.kuleuven.pylos.game.PylosBoard;
import be.kuleuven.pylos.game.PylosPlayerColor;

import java.util.List;

public class ReducedBoard {

    private long state;
    private byte lightReserve;
    private byte darkReserve;
    private boolean turn; //0 = light, 1 = dark
    private List<ReducedBoard> children;

    //Gekopieerde code van Jan Christiaens
    private final static long[][][][] BIT_MASK;    // [z][x][y][0/1 color]
    private final static long[][][][] BIT_MASK_I;  //Inversed bitmask

    static {
        int index = 0;
        BIT_MASK = new long[4][][][];
        BIT_MASK_I = new long[4][][][];
        for (int z = 0; z < 4; z++) {
            BIT_MASK[z] = new long[4 - z][][];
            BIT_MASK_I[z] = new long[4 - z][][];
            for (int x = 0; x < BIT_MASK[z].length; x++) {
                BIT_MASK[z][x] = new long[4 - z][];
                BIT_MASK_I[z][x] = new long[4 - z][];
                for (int y = 0; y < BIT_MASK[z].length; y++) {
                    BIT_MASK[z][x][y] = new long[2];
                    BIT_MASK[z][x][y][0] = 1l << index;
                    BIT_MASK[z][x][y][1] = 1l << (index + 1);
                    BIT_MASK_I[z][x][y] = new long[2];
                    BIT_MASK_I[z][x][y][0] = ~BIT_MASK[z][x][y][0];
                    BIT_MASK_I[z][x][y][1] = ~BIT_MASK[z][x][y][1];
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
        long result = BIT_MASK[z][x][y][color.ordinal()] & state;
        return result != 0;
    }

    //Checken als een sphere op een bepaalde locatie staat
    public PylosPlayerColor containsSphere(int x, int y, int z) {
        if ((BIT_MASK[z][x][y][0] & state) != 0) return PylosPlayerColor.LIGHT;
        if ((BIT_MASK[z][x][y][1] & state) != 0) return PylosPlayerColor.DARK;
        return null;
    }

    public void setSphere(int x, int y, int z, PylosPlayerColor color) {
        state |= BIT_MASK[z][x][y][color.ordinal()];
        if (color == PylosPlayerColor.LIGHT) lightReserve--;
        else darkReserve--;
    }

    public void removeSphere(int x, int y, int z, PylosPlayerColor color) {
        state &= BIT_MASK_I[z][x][y][color.ordinal()];
        if (color == PylosPlayerColor.LIGHT) lightReserve++;
        else darkReserve++;
    }

    private boolean isSupported(int x, int y, int z) {
        if (z == 0) return true;
        //Checken als er onder een bepaalde coordinaat 4 lichte of zwarte spheres liggen.
        return ((BIT_MASK[z][x][y][0] & state) != 0 || (BIT_MASK[z][x][y][1] & state) != 0) &&
                ((BIT_MASK[z][x + 1][y][0] & state) != 0 || (BIT_MASK[z][x + 1][y][1] & state) != 0) &&
                ((BIT_MASK[z][x][y + 1][0] & state) != 0 || (BIT_MASK[z][x][y + 1][1] & state) != 0) &&
                ((BIT_MASK[z][x + 1][y + 1][0] & state) != 0 || (BIT_MASK[z][x + 1][y + 1][1] & state) != 0);

    }

    private boolean resultsInSquare(int x, int y, int z, PylosPlayerColor color) {
        int c = color.ordinal();
        /*
        4 mogelijkheden tot vierkant
        O O O
        O X O
        O O O
         */

        //links boven
        if (x != 0 && y != 0) {
            (BIT_MASK[z][x-1][y][c] | BIT_MASK[z][x][y-1][c] | BIT_MASK[z][x-1][y-1][c]) & state !=
        }
        //rechts boven
        if (x != 3 - z && y != 0) {

        }

        //links onder
        if (x != 0 && y != 3 - z) {

        }
        //rechts onder
        if (x != 3 - z && y != 3 - z) {

        }

        return (BIT_MASK[z][x + 1][y][color.ordinal()] | state) != 0 &&
                (BIT_MASK[z][x][y + 1][color.ordinal()] & state) != 0 &&
                (BIT_MASK[z][x + 1][y + 1])

    }

    //Alle moves overlopen waarbij een element uit de reserve kan worden genomen
    public long[] getAllMoves(PylosPlayerColor player, PylosBoard board) {
        //Alle velden overlopen
        for (int z = 0; z < 4; z++) {
            for (int y = 0; y < 4 - z; y++) {
                for (int x = 0; x < 4 - z; x++) {
                    if (containsSphere(x, y, z) != null && isSupported(x, y, z)) {

                    }
                }
            }
        }
    }


}
