
/*
 *  Copyright (C) 2022 Lamberto Colazzo
 *  
 *  This file is part of the ConnectX software developed for the
 *  Intern ship of the course "Information technology", University of Bologna
 *  A.Y. 2021-2022.
 *
 *  ConnectX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This  is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details; see <https://www.gnu.org/licenses/>.
 */

package connectx.L2;

import connectx.CXPlayer;
import connectx.CXBoard;
import connectx.CXGameState;
import connectx.CXCell;
import java.util.TreeSet;
import java.util.Random;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

/**
 * Software player only a bit smarter than random.
 * <p>
 * It can detect a single-move win or loss. In all the other cases behaves
 * randomly.
 * </p>
 */
public class L2 implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private long START;

    /* Default empty constructor */
    public L2() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
    }


    public int alphabeta(CXBoard B, int depth, boolean maximizingPlayer, int alpha, int beta) throws TimeoutException {
        if (depth == 0 || B.gameState() != CXGameState.OPEN) {
            if (B.gameState() != CXGameState.OPEN && B.gameState() == myWin) {
                return Integer.MAX_VALUE; // win
            } else if (B.gameState() != CXGameState.OPEN && B.gameState() == yourWin) {
                return Integer.MIN_VALUE; // loss
            } else {
                return 0; // draw
            }
        } else if (maximizingPlayer) {
            int bestValue = Integer.MIN_VALUE;
            for (int i : B.getAvailableColumns()) {
                checktime();
                CXBoard child = B.clone();
                child.markColumn(i);
                int value = alphabeta(child, depth - 1, false, alpha, beta);
                bestValue = Math.max(bestValue, value);
                alpha = Math.max(alpha, value);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        } else {
            int bestValue = Integer.MAX_VALUE;
            for (int i : B.getAvailableColumns()) {
                checktime();
                CXBoard child = B.clone();
                child.markColumn(i);
                int value = alphabeta(child, depth - 1, true, alpha, beta);
                bestValue = Math.min(bestValue, value);
                beta = Math.min(beta, value);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        }
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();
        Integer[] L = B.getAvailableColumns();
        try {
            int win = singleMoveWin(B, L);
            if (win != -1)
                return win;
            int block = singleMoveBlock(B, L);
            if (block != -1)
                return block;
            int bestValue = Integer.MIN_VALUE;
            int bestMove = -1;
            for (int i : L) {
                checktime();
                CXBoard child = B.clone();
                child.markColumn(i);
                int value = alphabeta(child, 3, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
                if (value > bestValue) {
                    bestValue = value;
                    bestMove = i;
                }
            }
            return bestMove;
        } catch (TimeoutException e) {
            // Timeout: return a random column
            return L[rand.nextInt(L.length)];
        }
    }

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    /**
     * Check if we can win in a single move
     *
     * Returns the winning column if there is one, otherwise -1
     */
    private int singleMoveWin(CXBoard B, Integer[] L) throws TimeoutException {
        for (int i : L) {
            checktime(); // Check timeout at every iteration
            CXGameState state = B.markColumn(i);
            if (state == myWin)
                return i; // Winning column found: return immediately
            B.unmarkColumn();
        }
        return -1;
    }

    /**
     * Check if we can block adversary's victory
     *
     * Returns a blocking column if there is one, otherwise a random one
     */
    private int singleMoveBlock(CXBoard B, Integer[] L) throws TimeoutException {
        TreeSet<Integer> T = new TreeSet<Integer>(); // We collect here safe column indexes

        for (int i : L) {
            checktime();
            T.add(i); // We consider column i as a possible move
            B.markColumn(i);

            int j;
            boolean stop;

            for (j = 0, stop = false; j < L.length && !stop; j++) {
                // try {Thread.sleep((int)(0.2*1000*TIMEOUT));} catch (Exception e) {} //
                // Uncomment to test timeout
                checktime();
                if (!B.fullColumn(L[j])) {
                    CXGameState state = B.markColumn(L[j]);
                    if (state == yourWin) {
                        T.remove(i); // We ignore the i-th column as a possible move
                        stop = true; // We don't need to check more
                    }
                    B.unmarkColumn(); //
                }
            }
            B.unmarkColumn();
        }

        if (T.size() > 0) {
            Integer[] X = T.toArray(new Integer[T.size()]);
            return X[rand.nextInt(X.length)];
        } else {
            return L[rand.nextInt(L.length)];
        }
    }

    public String playerName() {
        return "Alphabeta";
    }
}
