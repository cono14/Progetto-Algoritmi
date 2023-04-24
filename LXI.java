package connectx.LXI;

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

public class LXI implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private long START;
    private int[][] transposition_table;
    private static int SIZE = 1024 * 1024 * 6;
    private long[][][] zobrist;
    private int TIMEOUT_VALUE = 999999;
    private static int ERROR = Integer.MAX_VALUE;
    private static int flag_exact = 0;
    private static int flag_lower = -1;
    private static int flag_upper = 1;
    private long hash_value;
    int[][] killerMoves;

    public LXI() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
        zobrist = new long[M][N][2];
        transposition_table = new int[SIZE][];
        hash_value = 0;
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) {
                zobrist[i][j][0] = rand.nextLong();
                zobrist[i][j][1] = rand.nextLong();
            }
        }
        killerMoves = new int[N + 1][2];
        for (int i = 0; i < N + 1; i++) {
            killerMoves[i][0] = -1;
            killerMoves[i][1] = -1;
        }
    }

    private CXGameState doMove(CXBoard B, int y) {
        int x = getX(B, y);
        hash_value ^= zobrist[x][y][B.currentPlayer()];
        return B.markColumn(y);
    }

    private void undoMove(CXBoard B) {
        CXCell c = B.getMarkedCells()[B.getMarkedCells().length - 1];
        hash_value ^= zobrist[c.i][c.j][B.currentPlayer() == 1 ? 0 : 1]; // la mossa è del player precedente
        B.unmarkColumn();
    }

    private int Index() {
        return Math.abs((int) (hash_value % SIZE));
    }

    private void put(int depth, int bestMove, int value, int flag_type) {
        int firstHash = (int) (hash_value >> 32), secondHash = (int) hash_value;
        if (transposition_table[Index()] == null) { // avoiding collisions
            transposition_table[Index()] = new int[] { firstHash, secondHash, depth, bestMove, value, flag_type };
        } else {
            int i = 1;
            while ((Index() + i < SIZE) && transposition_table[Index() + i] != null) {
                i++;
            }
            if (Index() + i >= SIZE) { // devo sovrascrivere un dato che avevo salvato
                // lo faccio solo se: 1- ha profondità maggiore 2- se non ha profondità maggiore
                // ma ha un flag migliore
                int[] prec_element = transposition_table[Index()];
                if (depth >= prec_element[2]) {
                    transposition_table[Index()] = new int[] { firstHash, secondHash, depth, bestMove, value,
                            flag_type };
                } else if (flag_type != flag_exact && prec_element[5] == flag_exact) {
                    transposition_table[Index()] = new int[] { firstHash, secondHash, depth, bestMove, value,
                            flag_type };
                }
            } else { // occupo la prima posizione libera
                transposition_table[Index() + i] = new int[] { firstHash, secondHash, depth, bestMove, value,
                        flag_type };
            }

        }

    }

    private int get(int depth, int alpha, int beta) {
        int[] t_element = transposition_table[Index()];
        int firstHash = (int) (hash_value >> 32), secondHash = (int) hash_value;
        if (t_element != null && firstHash == t_element[0] && secondHash == t_element[1] && (t_element[2] >= depth)) {
            if (t_element[5] == flag_exact) {
                return t_element[4];
            }
            if (t_element[5] == flag_lower && t_element[4] <= alpha) {
                return alpha;
            }
            if (t_element[5] == flag_upper && t_element[4] >= beta) {
                return beta;
            }

        }
        return ERROR;
    }

    public static int getX(CXBoard B, int y) {
        int count = 0;
        for (int i = 1; i < B.M; i++) {
            if (B.cellState(i, y) != CXCellState.FREE) {
                count++;
            } else {
                break;
            }
        }
        return count;
    }
    // usare un array per salvare l'altezza raggiunta in ogni colonna

    public int eval(CXBoard board, int col) {

        int best = 0;
        int secondbest = 0;
        int last_row = getX(board, col);
        CXCellState s = board.cellState(last_row, col);
        int n = 1;

        for (int k = 1; col - k >= 0 && (board.cellState(last_row, col - k) == s
                || board.cellState(last_row, col - k) == CXCellState.FREE); k++) {
            if (board.cellState(last_row, col - k) == s)
                n++;
        } // backward check
        for (int k = 1; col + k < board.N && (board.cellState(last_row, col + k) == s
                || board.cellState(last_row, col + k) == CXCellState.FREE); k++) {
            if (board.cellState(last_row, col + k) == s)
                n++;
        } // forward check
        best = n;

        // Vertical check
        n = 1;
        for (int k = 1; last_row + k < board.M && (board.cellState(last_row + k, col) == s
                || board.cellState(last_row + k, col) == CXCellState.FREE); k++) {
            if (board.cellState(last_row + k, col) == s)
                n++;
        }
        best = Math.max(best, n);
        secondbest = Math.min(best, n);

        // Diagonal check
        n = 1;
        for (int k = 1; last_row - k >= 0 && col - k >= 0
                && (board.cellState(last_row - k, col - k) == s
                        || board.cellState(last_row - k, col - k) == CXCellState.FREE); k++) {
            if (board.cellState(last_row - k, col - k) == s)
                n++;
        } // backward check
        for (int k = 1; last_row + k < board.M && col + k < board.N
                && (board.cellState(last_row + k, col + k) == s
                        || board.cellState(last_row + k, col + k) == CXCellState.FREE); k++) {
            if (board.cellState(last_row + k, col + k) == s)
                n++;
        } // forward check
        if (n > secondbest) {
            best = Math.max(best, n);
            secondbest = Math.min(best, n);
        }

        // Anti-diagonal check
        n = 1;
        for (int k = 1; last_row - k >= 0 && col + k < board.N
                && (board.cellState(last_row - k, col + k) == s || board.cellState(last_row - k, col
                        + k) == CXCellState.FREE); k++) {
            if (board.cellState(last_row - k, col + k) == s)
                n++;
        } // backward check
        for (int k = 1; last_row + k < board.M && col - k >= 0
                && (board.cellState(last_row + k, col - k) == s || board.cellState(last_row + k, col
                        - k) == CXCellState.FREE); k++) {
            if (board.cellState(last_row + k, col - k) == s)
                n++;
        } // forward check
        if (n > secondbest) {
            best = Math.max(best, n);
            secondbest = Math.min(best, n);
        }
        if (best >= board.X)
            return best + secondbest; // in alternativa, Integer.MAX_VALUE
        else {
            int sol = (int) (best + secondbest * 0.5);
            return sol;
        }
    }

    private int[] getOrderedColumns(CXBoard B, int depth) {
        int[] orderedColumns = new int[B.getAvailableColumns().length + 3];
        int j = 0;

        if (depth > 0) {
            for (int i = 0; i < 2; i++) {
                int killerMove = killerMoves[depth][i];
                if (killerMove != -1 && !B.fullColumn(killerMove)) {
                    orderedColumns[j] = killerMove;
                    j++;
                }
            }
        } // controlliamo se alla profondità precedente abbiamo trovato delle killer moves
          // e aggiorniamo

        for (int col : B.getAvailableColumns()) {
            if (!Arrays.asList(orderedColumns).contains(col)) { // controlliamo di non aggiungere due volte le killer
                                                                // moves
                orderedColumns[j] = col;
                j++;
            }
        }
        return Arrays.copyOf(orderedColumns, j);
    }

    public int alphabeta(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer) {
        int value, valueKind = flag_exact;
        int localBestMove = -1;
        value = get(depth, alpha, beta);
        if (value != ERROR) {
            return value;
        }
        if (checktime()) {
            return TIMEOUT_VALUE;
        }
        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            if (board.gameState() != CXGameState.OPEN && board.gameState() == myWin) {
                return Integer.MAX_VALUE; // win
            } else if (board.gameState() != CXGameState.OPEN && board.gameState() == yourWin) {
                return Integer.MIN_VALUE; // loss
            } else {
                value = eval(board, board.getLastMove().j);
                put(depth, board.getLastMove().j, value, valueKind);
                return value; // draw

            }
        }
        // Massimizzare la valutazione per il giocatore corrente
        if (maximizingPlayer) {
            int bestValue = Integer.MIN_VALUE;

            for (int column : getOrderedColumns(board, depth)) {
                // Effettuare la mossa sulla colonna selezionata

                doMove(board, column);

                // Calcolare il valore della mossa
                value = alphabeta(board, depth - 1, alpha, beta, false);
                bestValue = Math.max(bestValue, value);

                if (value > bestValue) {
                    localBestMove = column;
                    bestValue = value;
                }
                // Aggiornare il valore di alpha
                if (bestValue >= beta) {
                    valueKind = flag_upper;
                    killerMoves[depth][1] = killerMoves[depth][0];
                    killerMoves[depth][0] = column;
                } else if (bestValue <= alpha) {
                    valueKind = flag_lower;
                } else {
                    valueKind = flag_exact;
                }
                alpha = Math.max(alpha, bestValue);

                // Annullare la mossa effettuata sulla colonna selezionata
                // undoMove(board);
                board.unmarkColumn();

                // Verificare se si può tagliare il ramo
                if (beta <= alpha) {
                    break;
                }
            }
            put(depth, localBestMove, bestValue, valueKind);
            return bestValue;
        }

        // Minimizzare la valutazione per l'avversario
        else {
            int bestValue = Integer.MAX_VALUE;

            for (int column : getOrderedColumns(board, depth)) {
                // Effettuare la mossa sulla colonna selezionata dall'avversario
                doMove(board, column);

                // Calcolare il valore della mossa
                value = alphabeta(board, depth - 1, alpha, beta, true);
                if (value < bestValue) {
                    localBestMove = column;
                    bestValue = value;

                }

                // Aggiornare il valore di beta
                if (bestValue >= beta) {
                    valueKind = flag_upper;
                } else if (bestValue <= alpha) {
                    valueKind = flag_lower;
                    killerMoves[depth][1] = killerMoves[depth][0];
                    killerMoves[depth][0] = column;
                } else {
                    valueKind = flag_exact;
                }
                beta = Math.min(beta, bestValue);

                // Annullare la mossa effettuata sulla colonna selezionata
                undoMove(board);

                // Verificare se si può tagliare il ramo
                if (beta <= alpha) {
                    break;
                }
            }
            put(depth, localBestMove, bestValue, valueKind);
            return bestValue;
        }
    }

    public int center(CXBoard B) {
        if (B.getMarkedCells().length == 0 || B.getMarkedCells().length == 1) {
            doMove(B, B.N / 2);
            return B.N / 2;
        }
        return -1;
    }

    public int LosingColumn(CXBoard B) {
        int col = B.getAvailableColumns()[0];
        // doMove(B, col);
        doMove(B, col);
        for (int k = 1; k < B.getAvailableColumns().length; k++) {
            if (k != col && B.gameState() == CXGameState.OPEN) { // per evitare l'errore 'Game'
                int col_2 = B.getAvailableColumns()[k];
                doMove(B, col_2);
                if (B.gameState() == yourWin) {
                    System.out.println("Sconfitta evitata");
                    undoMove(B);// UNDO YOUR MOVE
                    undoMove(B); // undo my move
                    doMove(B, col_2); // steal your move
                    return col_2;
                } else {
                    undoMove(B);
                }
            }
        }
        undoMove(B);
        return -1;
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time

        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)]; // Save a random column
        int bestValue = -1;
        int best_Move = -1;
        int col = center(B);
        if (col != -1)
            return col;

        int possible_lose = LosingColumn(B);
        if (possible_lose != -1) {
            return possible_lose;
        }

        int current_depth_limit = 3;
        int max_depth = B.getAvailableColumns().length;
        while (current_depth_limit <= max_depth && !checktime()) {
            for (int i : L) {
                doMove(B, i);

                if (B.gameState() == myWin) {
                    System.out.println("Serve a qualcosa"); // serve a qualcosa
                    return i;
                }

                int value = alphabeta(B, current_depth_limit, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
                undoMove(B);
                if (value > bestValue) {
                    bestValue = value;
                    best_Move = i;
                }
            }
            max_depth = B.getAvailableColumns().length;
            current_depth_limit++;
        }

        // Select a random column if no valid moves are available
        if (best_Move == -1) {
            System.err.println("No");

            return save;
        }
        System.out.println("At least one valid move calculated");

        return best_Move;
    }

    /**
     * Check if we can block adversary's victory
     *
     * Returns a blocking column if there is one, otherwise a random one
     */

    private boolean checktime() {
        return (System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0);
    }

    public String playerName() {
        return "LXI";
    }
}