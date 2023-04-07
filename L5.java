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
import java.util.Collections;
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

    
    public int eval(CXBoard board, int col) {
        /*funzione che valuta, in base alla mossa appena fatta, un "punteggio":
        versione 1: si restituisce un valore che dipende dalla concentrazione maggiore
        di tasselli vicino a dove e' stata compiuta la mossa; +1 per un proprio tassello,
        +0 per una cella vuota, interruzione del conto de c' e' un tassello dell'avversario

        costo approssimato: 7 * n dove n sono i pezzi da connettere, e 7 sono tutti i 
        singoli check per stabilire spazi liberi*/
        int best=0;
        int secondbest=0;

        //problema: come accedere alla cella [i, j]? in particolare, conosciamo la
        //colonna i, ma non la altezza j a cui finisce un tassello inseritya nella colonna i
        CXCellState s = B.cellState(i, j);
		int n;

		// Useless pedantic check
		if (s == CXCellState.FREE)
			return false;
		// Horizontal check
		n = 1;
		for (int k = 1; j-k >= 0 && (B.cellState(i, j-k) == s || B.cellState(i, j-k) == FREE); k++){
            if (B.cellState(i, j-k) == s) n++;}  // backward check
		for (int k = 1; j+k <  N && (B.cellState(i, j+k) == s || B.cellState(i, j+k) == FREE); k++){
            if (B.cellState(i, j+k) == s) n++;}  // forward check
		best=n;

		// Vertical check
		n = 1;
		for (int k = 1; i+k <  B.M && (B.cellState(i+k, j) == s || B.cellState(i+k, j) == FREE); k++){
            if (B.cellState(i+k, j) == s) n++;}
        best=Math.max(best, n);
		secondbest=Math.min(best, n);

		// Diagonal check
		n = 1;
		for (int k = 1; i-k >= 0 && j-k >= 0 && (B.cellState(i-k, j-k) == s || B.cellState(i-k, j-k) == s); k++){
            if (B.cellState(i-k, j-k) == s) n++;}// backward check
		for (int k = 1; i+k <  B.M && j+k <  B.N && (B.cellState(i+k, j+k) == s || B.cellState(i+k, j+k) == s); k++){
            if (B.cellState(i+k, j+k) == s) n++;} // forward check
		if(n > secondbest){
            best=Math.max(best, n);
		    secondbest=Math.min(best, n);
        }
        
		// Anti-diagonal check
		n = 1;
		for (int k = 1; i-k >= 0 && j+k <  N && (B.cellState(i-k, j+k) == s || B.cellState(i-k, j+k) == s); k++){
            if (B.cellState(i-k, j+k) == s) n++;} // backward check
		for (int k = 1; i+k <  M && j-k >= 0 && (B.cellState(i+k, j-k) == s || B.cellState(i+k, j-k) == s); k++){
            if (B.cellState(i+k, j-k) == s) n++;} // forward check
        if(n > secondbest){
            best=Math.max(best, n);
            secondbest=Math.min(best, n);            
        }
        if(best>=B.X) return 9999; //in alternativa, Integer.MAX_VALUE
        else{
        int sol= (best+secondbest*0.5);
		return sol;
        }
    }

    public int alpahabeta(int depth, int alpha, int beta, boolean maximizingPlayer) {
        int score = transpositionTable.get(depth, alpha, beta); // verifico subito se
        // il mio valore è già presente nella
        // transposition table

        if (score != TranspositionTable.ERROR) {
            return score; // se c'è lo ritorno
        } else {
            score = 1;
        }

        if (depth == 0 || B.gameState() != CXGameState.OPEN) { // se sono arrivato a profondità 0 valuto il valore con
                                                               // evaluation, lo salvo nella tt e lo ritorno
            // replace with evaluation function
            // transpositionTable.put(depth, best_move, score,
            // transpositionTable.flag_correct);
            return score;
        }
        int bestScore = maximizingPlayer ? Integer.MIN_VALUE : Integer.MAX_VALUE; // verifico se il mio player
                                                                                  // massimizza o minimizza
        for (int i : B.getAvailableColumns()) { // scorro sulle colonne libere
            markColumnHash(i);
            int currentScore = alpahabeta(depth - 1, alpha, beta, maximizingPlayer);
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

    public int selectColumn(CXBoard B) {
        START = System.currentTimeMillis(); // Save starting time
        Integer[] L = B.getAvailableColumns();
        int save = L[rand.nextInt(L.length)]; // Save a random column
        int bestValue = -1;
        int depth = B.getAvailableColumns().length;
        int bestMove = -1;
        System.out.println("Trying");
        for (int i : B.getAvailableColumns()) {
            checktime();
            markColumnHash(i);
            if (B.gameState() == myWin) {
                return i;
            }
            int value = alpahabeta(depth, Integer.MIN_VALUE, Integer.MAX_VALUE, false);
            unmarkColumnHash();
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
        } else {
            return bestMove;
        }
    }

    private boolean checktime() {
        return (System.currentTimeMillis() - START) / 1000.0 > TIMEOUT * (99.0 / 100.0);
    }

    public String playerName() {
        return "CC";
    }

}
