package connectx.LX;

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

public class LX implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private long START;

    public LX() {
    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;

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

    int evalThreat(int n, bool i, bool j){
        if(n==CXPlayer.K-1 && i && j) return Integer.MAX_VALUE;
        if(n==CXPlayer.K-1 && (i || j)) return 300;
        if(n==CXPlayer.K-2 && i && j) return 650;
        if(n==CXPlayer.K-2 && (i || j)) return 100;
        else return n;
    }


    public int eval(CXBoard board, int col) {

        int best = 0;
        int last_row = getX(board, col);
        CXCellState s = board.cellState(last_row, col);
        int n = 1;
        bool open1=true;
        bool open2=true;
        int k = 1;
        for (k=1; col - k >= 0 && (board.cellState(last_row, col - k) == s); k++) {
            //if (board.cellState(last_row, col - k) == s)
                n++;
        } 
        if(col - k < 0) open1=false;
        else {if((board.cellState(last_row, col - k) != CXCellState.FREE))
            open1=false;
            }/* backward check:
            vengono contati i pezzi del player continui in una direzione, fino ad un muro, o 
            ad uno spazio occupato dall'avversario; si determina poi se si
            e' giunti ad un blocco ancora libero (situazione ideale) o ad uno occupato.
            si invoca poi una funzione di evaluation rispetto i pezzi continui contati
            e le loro estremita'
            */
        for (k=1; col + k >= 0 && (board.cellState(last_row, col + k) == s); k++) {
            if (board.cellState(last_row, col + k) == s)
                n++;
        } 
        if(col + k > board.N) open2=false;
        else {if(board.cellState(last_row, col + k) != CXCellState.FREE)
            open2=false;
            }// forward check
        best+=evalThreat(n, open1, open2);

        // Vertical check
        n = 1;
        open1=true;
        for (k = 1; last_row + k < board.M && (board.cellState(last_row + k, col) == s); k++) {
            //if (board.cellState(last_row + k, col) == s)
                n++;
        }
        if(last_row + k == board.M) open1=false;
        best +=evalThreat(n, open1, false); //nel check verticale, una estremita' e' sempre gia' occupata

        // Diagonal check
        n = 1;
        open1=true;
        open2=true;
        for (k = 1; last_row - k >= 0 && col - k >= 0 && (board.cellState(last_row - k, col - k) == s); k++) {
            //if (board.cellState(last_row - k, col - k) == s)
                n++;
        } 
        if(last_row - k <0 || col - k <0) open1=false;
        else{
            if(board.cellState(last_row - k, col - k) != CXCellState.FREE)
            open1=false;
            } // backward check
        for (k = 1; last_row + k < board.M && col + k < board.N && (board.cellState(last_row + k, col + k) == s); k++) {
                n++;
        } 
        if(last_row + k == board.M || col + k == board.N) open2=false;
        else{
            if(board.cellState(last_row + k, col + k) != CXCellState.FREE)
            open2=false;
            } // forward check
        best+=evalThreat(n, open1, open2);

        // Anti-diagonal check
        n = 1;
        open1=true;
        open2=true;
        for (k = 1; last_row - k >= 0 && col + k < board.N && (board.cellState(last_row - k, col + k) == s ); k++) {
                n++;
        }
        if(last_row - k < 0 || col + k == board.N) open1=false;
        else{if (board.cellState(last_row - k, col + k)!= CXCellState.FREE)
            open1=false;
        } // backward check
        for (k = 1; last_row + k < board.M && col - k >= 0 && (board.cellState(last_row + k, col - k) == s); k++) {
                n++;
        }
        if(last_row + k = board.M || col - k < 0)open2=false;
        else{if(board.cellState(last_row + k, col - k) != CXCellState.FREE)
            open2=false;
        } // forward check
        best+=evalThreat(n, open1, open2);
        return best;
    }

    public int alphabeta(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer) {

        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            if (board.gameState() != CXGameState.OPEN && board.gameState() == myWin) {
                return Integer.MAX_VALUE; // win
            } else if (board.gameState() != CXGameState.OPEN && board.gameState() == yourWin) {
                return Integer.MIN_VALUE; // loss
            } else {
                return eval(board, board.getLastMove().j); // draw

            }
        }
        // Massimizzare la valutazione per il giocatore corrente
        if (maximizingPlayer) {
            int bestValue = Integer.MIN_VALUE;
            for (int column : board.getAvailableColumns()) {
                // Effettuare la mossa sulla colonna selezionata
                board.markColumn(column);

                // Calcolare il valore della mossa
                int value = alphabeta(board, depth - 1, alpha, beta, false);

                bestValue = Math.max(bestValue, value);
                if (value > bestValue) {
                    bestValue = value;
                }
                // Aggiornare il valore di alpha
                alpha = Math.max(alpha, bestValue);

                // Annullare la mossa effettuata sulla colonna selezionata
                board.unmarkColumn();

                // Verificare se si può tagliare il ramo
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        }

        // Minimizzare la valutazione per l'avversario
        else {
            int bestValue = Integer.MAX_VALUE;

            for (int column : board.getAvailableColumns()) {
                // Effettuare la mossa sulla colonna selezionata dall'avversario
                board.markColumn(column);

                // Calcolare il valore della mossa
                int value = alphabeta(board, depth - 1, alpha, beta, true);
                if (value < bestValue) {
                    bestValue = value;

                }

                // Aggiornare il valore di beta
                beta = Math.min(beta, bestValue);

                // Annullare la mossa effettuata sulla colonna selezionata
                board.unmarkColumn();

                // Verificare se si può tagliare il ramo
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        }
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time

        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)]; // Save a random column
        int bestValue = -1;
        int bestMove = -1;
        for (int i : L) {
            B.markColumn(i);
            if (B.gameState() == myWin) {
                System.out.println("Serve a qualcosa"); // serve a qualcosa
                return i;
            }

            int value = alphabeta(B, 6, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            B.unmarkColumn();
            if (value > bestValue) {
                bestValue = value;
                bestMove = i;
            }
            System.out.println("got here2");
        }
        // Select a random column if no valid moves are available
        if (bestMove == -1) {
            System.err.println("No");

            return save;
        }
        System.out.println("At least one valid move calculated");

        return bestMove;
    }

    /**
     * Check if we can block adversary's victory
     *
     * Returns a blocking column if there is one, otherwise a random one
     */

    private void checktime() throws TimeoutException {
        if ((System.currentTimeMillis() - START) / 1000.0 >= TIMEOUT * (99.0 / 100.0))
            throw new TimeoutException();
    }

    public String playerName() {
        return "LX";
    }

}

/*
 * Idea -> iniziare sempre da colonne centrali, e quando si deve fare una mossa
 * considerare solo le colonne adiacenti
 * a quelle marcate
 */