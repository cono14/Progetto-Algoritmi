package connectx.L5;

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
import connectx.L5.TranspositionTable;

public class L5 implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private CXBoard B;
    private int TIMEOUT;
    private long START;
    private TranspositionTable transpositionTable;
    private int best_move;

    public L5() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        B = new CXBoard(M, N, K);
        transpositionTable = new TranspositionTable(B);
        best_move = -1;
    }

    /*
     * voglio sapere l'hash aggiornato
     */
    private CXGameState markColumnHash(int col) {
        transpositionTable.currenthash ^= transpositionTable.zobrist[col][B.currentPlayer()];
        return B.markColumn(col);
    }

    private void unmarkColumnHash() {
        CXCell c = B.getMarkedCells()[B.getMarkedCells().length - 1];
        transpositionTable.currenthash ^= transpositionTable.zobrist[c.j][B.currentPlayer() == 1 ? 0 : 1]; // la mossa è
                                                                                                           // del player
        // precedente
        B.unmarkColumn();
    }

    public int alpahabeta(int depth, int alpha, int beta, boolean maximizingPlayer) {
        int score = transpositionTable.get(depth, alpha, beta); // verifico subito se il mio valore è già presente nella
                                                                // transposition table
        if (score != transpositionTable.ERROR) {
            return score; // se c'è lo ritorno
        }
        if (depth == 0 || B.gameState() != CXGameState.OPEN) { // se sono arrivato a profondità 0 valuto il valore con
                                                               // evaluation, lo salvo nella tt e lo ritorno
            score = 1; // replace with evaluation function
            transpositionTable.put(depth, best_move, score, transpositionTable.flag_correct);
            return score;
        }
        int bestScore = maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE; // verifico se il mio player
                                                                                  // massimizza o minimizza
        for (int i : B.getAvailableColumns()) { // scorro sulle colonne libere
            markColumnHash(i);
            int currentScore = alpahabeta(depth - 1, alpha, beta, !maximizingPlayer);
            unmarkColumnHash();

            if (maximizingPlayer) {
                if (currentScore > bestScore) {
                    bestScore = currentScore;
                    alpha = Math.max(alpha, bestScore);
                    best_move = i;
                }
            } else {
                if (currentScore < bestScore) {
                    bestScore = currentScore;
                    beta = Math.min(beta, bestScore);
                }
            }
            if (alpha >= beta) {
                transpositionTable.put(depth, best_move, bestScore,
                        beta < currentScore ? TranspositionTable.flag_lower : TranspositionTable.flag_upper);
                return bestScore;
            }

        }
        transpositionTable.put(depth, best_move, bestScore, TranspositionTable.flag_correct); // cache score in
                                                                                              // transposition table
        return bestScore;

    }

    public int AlphaBetaRoot(int depth) {
        int bestColumn = -1;
        int alpha = Integer.MIN_VALUE;
        int beta = Integer.MAX_VALUE;
        int bestValue = -1;

        Integer[] L = B.getAvailableColumns();
        for (int i : L) {
            checktime();
            markColumnHash(i);
            if (B.gameState() == myWin) {
                return i;
            }
            int value = alpahabeta(depth, alpha, beta, false);
            unmarkColumnHash();
            if (value > bestValue) {
                bestValue = value;
                bestColumn = i;
            }
        }
        return bestColumn;
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis();
        int depth = 1;
        int bestMove = -1;
        int newColumn = -1;
        int maxDepth = B.getAvailableColumns().length;

        while (!checktime() && depth <= maxDepth) {
            newColumn = AlphaBetaRoot(depth);
            if (newColumn != -1) {
                bestMove = newColumn;
            }
            depth++;
        }
        return bestMove;
    }

    private boolean checktime() {
        return (System.currentTimeMillis() - START) / 1000.0 > TIMEOUT * (93.0 / 100.0);
    }

    public String playerName() {
        return "CC";
    }

}
