package connectx.L3;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import connectx.CXCellState;

import java.util.TreeSet;
import java.util.Random;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import connectx.CXCell;

public class L3 implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private long START;
    private int MAX_DEPTH;
    private int bestColumn;

    public L3() {

    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        bestColumn = -1;
    }

    public int alphabeta(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer) {
        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            if (board.gameState() != CXGameState.OPEN && board.gameState() == myWin) {
                return Integer.MAX_VALUE; // win
            } else if (board.gameState() != CXGameState.OPEN && board.gameState() == yourWin) {
                return Integer.MIN_VALUE; // loss
            } else {
                return 0; // draw
            }
        }

        int bestValue = maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (int column : board.getAvailableColumns()) {
            // Make the move on the selected column
            board.markColumn(column);

            // Calculate the value of the move
            int value = alphabeta(board, depth - 1, alpha, beta, !maximizingPlayer);

            // Update the best value and column
            if (maximizingPlayer && value > bestValue) {
                bestValue = value;
                bestColumn = column;
            } else if (!maximizingPlayer && value < bestValue) {
                bestValue = value;
                bestColumn = column;
            }

            // Update alpha or beta
            if (maximizingPlayer) {
                alpha = Math.max(alpha, bestValue);
            } else {
                beta = Math.min(beta, bestValue);
            }

            // Undo the move
            board.unmarkColumn();

            // Cut off the branch
            if (beta <= alpha) {
                break;
            }
        }

        return bestValue;
    }

    public int selectColumn(CXBoard board) {
        long start = System.currentTimeMillis();
        Integer[] L = board.getAvailableColumns();
        int best_Column = L[rand.nextInt(L.length)];
        int bestValue = Integer.MIN_VALUE;
        MAX_DEPTH = board.numOfFreeCells();
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int searchDepth = 1;
        while (searchDepth <= MAX_DEPTH) {

            if ((System.currentTimeMillis() - start) / 1000.0 >= TIMEOUT * (99.0 / 100.0)) {
                break;
            }
            int value = alphabeta(board, searchDepth, alpha, beta, true);
            System.out.println("Sta andando");
            if (value == Integer.MAX_VALUE || value == Integer.MIN_VALUE) {
                best_Column = bestColumn;
                break;
            }

            if (value > bestValue) {
                bestValue = value;
                best_Column = bestColumn;
            }

            // Reset alpha and beta
            alpha = Integer.MIN_VALUE;
            beta = Integer.MAX_VALUE;

            // Update search depth
            searchDepth++;
        }
        return best_Column;
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    public String playerName() {
        return "L3";
    }

}
