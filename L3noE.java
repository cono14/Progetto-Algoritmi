package connectx.L3noE;

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

public class L3noE implements CXPlayer {
    private Random rand;
    private CXGameState myWin;
    private CXGameState yourWin;
    private int TIMEOUT;
    private long START;

    public L3noE() {

    }

    public void initPlayer(int M, int N, int K, boolean first, int timeout_in_secs) {
        // New random seed for each game
        rand = new Random(System.currentTimeMillis());
        myWin = first ? CXGameState.WINP1 : CXGameState.WINP2;
        yourWin = first ? CXGameState.WINP2 : CXGameState.WINP1;
        TIMEOUT = timeout_in_secs;
    }

    public int eval(CXBoard board, int col, boolean maximizingPlayer) {
        /*funzione che valuta, in base alla mossa appena fatta, un "punteggio":
        versione 1: maggiore e' la concentrazione di pezzi dello stesso giocatore,
        dopo aver mosso, maggiore sara' il punteggio*/
        int best=0;
        int secondbest=0;
        CXCellState s = B[i][j];
		int n;

		// Useless pedantic check
		if (s == CXCellState.FREE)
			return false;

		// Horizontal check
		n = 1;
		for (int k = 1; j-k >= 0 && B[i][j-k] == s; k++) n++; // backward check
		for (int k = 1; j+k <  N && B[i][j+k] == s; k++) n++; // forward check
		best=n;

		// Vertical check
		n = 1;
		for (int k = 1; i+k <  M && B[i+k][j] == s; k++) n++;
        best=Math.max(best, n);
		secondbest=Math.min(best, n);

		// Diagonal check
		n = 1;
		for (int k = 1; i-k >= 0 && j-k >= 0 && B[i-k][j-k] == s; k++) n++; // backward check
		for (int k = 1; i+k <  M && j+k <  N && B[i+k][j+k] == s; k++) n++; // forward check
		if(n > best){
            secondbest=best;
            best=n;
        }
        else {
            if(n > secondbest){
                secondbest=n;
            }
        }

		// Anti-diagonal check
		n = 1;
		for (int k = 1; i-k >= 0 && j+k <  N && B[i-k][j+k] == s; k++) n++; // backward check
		for (int k = 1; i+k <  M && j-k >= 0 && B[i+k][j-k] == s; k++) n++; // forward check
		if(n > best){
            secondbest=best;
            best=n;
        }
        else {
            if(n > secondbest){
                secondbest=n;
            }
        }
        int sol= (best+secondbest*0.5);
		return sol;

    }


    public int alphabeta(CXBoard board, int depth, int alpha, int beta, boolean maximizingPlayer) {
        if (depth == 0 || board.gameState() != CXGameState.OPEN) {
            if (board.gameState() != CXGameState.OPEN && board.gameState() == myWin) {
                return Integer.MAX_VALUE; // win
            } else if (board.gameState() != CXGameState.OPEN && board.gameState() == yourWin) {
                return Integer.MIN_VALUE; // loss
            } else {
                return 0; // draw --> funzione eval
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
                bestValue = Math.min(bestValue, value);

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

    public static int getX(CXBoard B, int y) {
        int count = 0;
        for (int i = 0; i < B.M; i++) {
            if (B.cellState(i, y) != CXCellState.FREE) {
                count++;
            } else {
                break;
            }
        }
        if (count == 0) {
            return 0;
        } else {
            return count + 1;
        }
    }

    public static int winningColumn(CXBoard B, int y, int numToConnect) {
        int x = getX(B, y);
        CXCellState player = B.cellState(x, y);
        int count = 0;
        int column = -1;
        // controllo orizzontale
        for (int i = Math.max(0, y - numToConnect + 1); i <= Math.min(B.M - 1, y + numToConnect - 1); i++) {
            if (B.cellState(x, i) == player) {
                count++;
                if (count == numToConnect - 1) {
                    if (i + 1 < B.M && B.cellState(x, i + 1) == CXCellState.FREE) {
                        column = i + 1;
                        return column; // se ho x-1 pezzi allineati e metterlo a destra porterebbe ad una vittoria mia
                                       // o dell'avversario => seleziono la colonna a destra
                    } else if (i - numToConnect >= 0 && B.cellState(x, i - numToConnect) == CXCellState.FREE) {
                        column = i - 1;
                        return column; // se ho x-1 pezzi allineati e metterlo a destra porterebbe ad una vittoria mia
                                       // o dell'avversario => seleziono la colonna a sinistra
                    }
                }
            } else {
                break;
            }
        }
        count = 0;
        // Controllo verticale
        for (int i = Math.max(0, x - numToConnect + 1); i <= Math.min(B.N - 1, x + numToConnect - 1); i++) {
            if (B.cellState(i, y) == player) {
                count++;
                if (count == numToConnect - 1) {
                    // controlla se una mossa successiva porta alla vittoria
                    if (i + 1 < B.N && B.cellState(i + 1, y) == CXCellState.FREE) {
                        return y; // se ho uno spazio sopra libero allora posiziono
                    }
                    if (i - numToConnect >= 0 && B.cellState(i - numToConnect, y) == CXCellState.FREE) {
                        return y;
                    }
                }
            } else {
                break;
            }
        }

        count = 0;

        // Controllo diagonale verso destra
        for (int j = -numToConnect + 1; j <= numToConnect - 1; j++) {
            int r = x + j;
            int c = y + j;
            if (r >= 0 && r < B.N && c >= 0 && c < B.M) {
                if (B.cellState(r, c) == player) {
                    count++;
                    if (count == numToConnect - 1) {
                        // Controllo se posso inserire una pedina nella casella in alto a destra
                        // rispetto all'ultima pedina selezionata
                        if (r + 1 <= B.N && c + 1 < B.M && B.cellState(r + 1, c + 1) == CXCellState.FREE
                                && (B.cellState(r, c + 1) == CXCellState.P1
                                        || B.cellState(r, c + 1) == CXCellState.P2)) {
                            column = c + 1;
                            return column;
                        }
                        // Controllo se posso inserire una pedina nella casella in basso a sinistra
                        // rispetto all'ultima pedina selezionata
                        if (r - 1 >= 0 && c - 1 >= 0 && B.cellState(r - 1, c - 1) == CXCellState.FREE) {
                            if (r - 2 > 0) {
                                if (B.cellState(r - 2, c - 1) == CXCellState.P1
                                        || B.cellState(r - 2, c - 1) == CXCellState.P2) {
                                    column = c - 1;
                                    return column;
                                }
                            } else if (r - 2 == 0) {
                                column = c - 1;
                                return column;
                            }
                        }
                    }
                } else {
                    break;
                }
            }
        }

        count = 0;

        // Controllo diagonale verso sinistra
        for (int j = -numToConnect + 1; j <= numToConnect - 1; j++) {
            int r = x + j;
            int c = y - j;
            if (r >= 0 && r < B.N && c >= 0 && c < B.M) {
                if (B.cellState(r, c) == player) {
                    count++;
                    if (count == numToConnect - 1) {
                        if (r + 1 < B.N && c - 1 >= 0 && B.cellState(r + 1, c - 1) == CXCellState.FREE
                                && (B.cellState(r, c - 1) == CXCellState.P1
                                        || B.cellState(r, c - 1) == CXCellState.P2)) {
                            column = c - 1;
                            return column;
                        }
                        if (r - 1 >= 0 && c + 1 <= B.M && B.cellState(r - 1, c + 1) == CXCellState.FREE) {
                            if (r - 2 > 0) {
                                if (B.cellState(r - 2, c + 1) == CXCellState.P1
                                        || B.cellState(r - 2, c + 1) == CXCellState.P2) {
                                    column = c + 1;
                                    return column;
                                }
                            } else if (r - 2 == 0) {
                                column = c + 1;
                                return column;
                            }

                        }
                    }
                } else {
                    break;
                }
            }
        }
        return column;
    }

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time

        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)]; // Save a random column
        int bestValue = -1;

        try {
            int bestMove = -1;
            System.out.println("Trying");
            for (int i : L) {
                checktime();
                B.markColumn(i);
                if (B.gameState() == myWin) {
                    return i;
                } else if (B.gameState() == yourWin) {
                    System.out.println("Avoided");
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
                System.err.println("No valid moves available! Random column selected.");
                return save;
            }
            System.out.println("At least one valid move calculated");
            return bestMove;
        } catch (TimeoutException e) {
            System.err.println("Timeout!!! Random column selected");
            return save;
        }
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
        return "L3";
    }

}


/*
 * Idea -> scrivere un algoritmo che conta il numero di numero di pedine
 * allineate, se sono uguali a numToconnect - 1 => posizioniamo la pedina
 * direttamente in quella posizione senza chiederci per chi porti alla vittoria.
 * In questo modo si risolve il problema del dover implementare
 * singolmoveWin e singlemoveBlock che sono troppo expensive.
 */