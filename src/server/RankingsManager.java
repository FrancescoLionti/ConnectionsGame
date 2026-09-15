package server;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import server.model.Ranking;
import server.model.UserStats;

/**
 * Gestisce la persistenza e l'aggiornamento delle statistiche dei giocatori e
 * della classifica globale. Si occupa di caricare i dati da un file JSON
 * all'avvio e di salvarli ad ogni modifica, fornendo metodi per l'estrazione di
 * statistiche (ordinamenti, Top K, ecc.).
 */
public class RankingsManager {

    private final Gson gson;
    private final Ranking ranking; // La classe Ranking rappresenta la mappa in memoria degli username e delle rispettive statistiche
    private final String rankingFilePath;

    /**
     * Inizializza il gestore delle classifiche.
     *
     * @param rankingFilePath Il percorso in cui risiede il file JSON delle
     * statistiche.
     */
    public RankingsManager(String rankingFilePath) {
        // PrettyPrinting rende il JSON leggibile (con a capo e spazi)
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.rankingFilePath = rankingFilePath;
        this.ranking = loadRankings(); // Csrica in memoria la classifica presa dal DB JSON persistente
    }

    public Ranking getRanking() {
        return ranking;
    }

    /**
     * Carica la classifica dal file JSON all'avvio del server. Se il file non
     * esiste (es. primissimo avvio), restituisce un nuovo oggetto Ranking
     * vuoto.
     *
     * @return L'oggetto Ranking popolato, o vuoto in caso di file
     * mancante/errori.
     */
    private Ranking loadRankings() {
        File file = new File(rankingFilePath);
        if (!file.exists()) {
            return new Ranking();
        }

        try (Reader reader = new FileReader(file)) {
            // Deserializza l'intero contenuto del JSON in memoria
            Ranking loaded = gson.fromJson(reader, Ranking.class);
            if (loaded != null) {
                return loaded;
            } else {
                return new Ranking();
            }
        } catch (IOException e) {
            System.out.println("Errore nel caricamento della classifica" + e);
            return new Ranking();
        }
    }

    /**
     * Salva lo stato attuale della mappa "ranking" su file JSON.
     */
    public synchronized void saveRankings() {
        try (Writer writer = new FileWriter(rankingFilePath)) {
            gson.toJson(this.ranking, writer);
        } catch (IOException e) {
            System.out.println("Errore nell'aggiornamento della classifica" + e);
        }
    }

    /**
     * Aggiorna le statistiche personali di un utente al termine di una partita.
     * Se l'utente gioca per la prima volta, crea automaticamente una nuova
     * entry. Al termine delle operazioni di calcolo, invoca il salvataggio su
     * file. NB:non synchronized perche usata solo dentro metodi synchronized
     *
     * @param username Il nome del giocatore.
     * @param pointsToAdd I punti guadagnati (o persi) nella partita appena
     * conclusa.
     * @param won Booleano che indica se l'utente ha vinto o perso la partita.
     * @param numError Il numero di errori commessi durante la partita (da 0 a
     * 4).
     */
    public void updateScore(String username, int pointsToAdd, boolean won, int numError) {
        // Recupera o crea le statistiche per l'utente
        UserStats stats = ranking.computeIfAbsent(username, k -> new UserStats());

        // Aggiorna i dati
        stats.username = username;
        stats.totalScore += pointsToAdd;
        stats.puzzlesCompleted++;
        if (won) {
            if (numError == 0) {
                stats.perfectPuzzles++; // Vittoria senza alcun errore
            }
            stats.numWin++;
            stats.currentStreak++;
            stats.maxStreak = Math.max(stats.currentStreak, stats.maxStreak);
        } else {
            stats.currentStreak = 0;// In caso di sconfitta, la serie di vittorie di fila si interrompe
        }
        // Calcolo delle percentuali (con cast a float per mantenere i decimali)
        stats.winRate = stats.puzzlesCompleted == 0 ? 0.0f : (float) stats.numWin / stats.puzzlesCompleted;
        stats.lossRate = 1.0f - stats.winRate;
        // Aggiorna l'istogramma degli errori (quante volte ha fatto 0 errori, 1 errore, ecc.)
        stats.mistakeHistogram.put(numError, stats.mistakeHistogram.getOrDefault(numError, 0) + 1);
        // Salva le modifiche su disco
        saveRankings();
    }

    /**
     * Restituisce la classifica globale sotto forma di lista ordinata.
     *
     * @return Una lista di tutti i giocatori, ordinata in modo decrescente
     * rispetto al punteggio totale.
     */
    public List<UserStats> getSortedLeaderboard() {
        List<UserStats> list = new ArrayList<>(ranking.values());
        // Ordina per punteggio decrescente
        list.sort((s1, s2) -> Integer.compare(s2.totalScore, s1.totalScore));
        return list;
    }

    /**
     * Estrae i migliori "K" giocatori dalla classifica globale.
     *
     * @param k Il numero massimo di posizioni da restituire.
     * @return Una sottolista ordinata contenente i primi K elementi. Se K è
     * maggiore del totale iscritti, ritorna tutti.
     */
    public List<UserStats> getTopLeaderboard(int k) {
        List<UserStats> list = new ArrayList<>(ranking.values());

        // Ordina per punteggio decrescente (migliori primi)
        list.sort((s1, s2) -> Integer.compare(s2.totalScore, s1.totalScore));

        // Ritorna solo i primi K (o tutti se meno di K)
        int endIndex = Math.min(k, list.size());
        return list.subList(0, endIndex);
    }

    /**
     * Verifica se un determinato utente è presente nel ranking
     *
     * @param username Il nome dell'utente da cercare.
     * @return true se l'utente esiste in classifica, false altrimenti.
     */
    public boolean playerInRank(String username) {
        return ranking.containsKey(username);
    }

    /**
     * Calcola la posizione (il "ranking") di uno specifico giocatore.
     *
     * @param username Il nome del giocatore.
     * @return L'intero che rappresenta la posizione (1° = 1, 2° = 2, ecc.).
     * Ritorna -1 se il giocatore non si trova.
     */
    public int getPositionPlayer(String username) {
        List<UserStats> classifica = getSortedLeaderboard();

        for (int i = 0; i < classifica.size(); i++) {
            if (classifica.get(i).equals(ranking.get(username))) {
                return i + 1;
            }
        }
        return -1;
    }

    /**
     * Restituisce l'oggetto completo contenente le statistiche di un singolo
     * giocatore.
     *
     * @param username L'username del giocatore desiderato.
     * @return Le statistiche dell'utente (UserStats), oppure null se non
     * esiste.
     */
    public UserStats getPlayer(String username) {
        return ranking.get(username);
    }

}
