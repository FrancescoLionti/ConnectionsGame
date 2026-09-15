package client;

import java.util.List;

import protocol.response.GameInfoResponse;
import protocol.response.GameStatsResponse;
import protocol.response.LeaderboardResponse;
import protocol.response.LoginResponse;
import protocol.response.PlayerStatsResponse;
import protocol.response.ProposalResponse;
import protocol.response.Response;
import server.model.Group;
import server.model.UserStats;

/**
 * Classe di utilità per l'impostazione grafica del gioco. Trasforma le risposte
 * grezze ricevute dal server in tabelle, banner e schermate graficamente
 * gradevoli usando caratteri ASCII.
 */
public class GameUI {

    /**
     * Metodo helper per stampare gli errori formattati all'interno di un box.
     * Viene richiamato da tutti gli altri metodi se il codice di risposta è >=
     * 400.
     */
    private static void printError(int code, String message) {
        String errorMsg = String.format(" ERRORE %d: %s ", code, message);
        int length = errorMsg.length();

        // Crea una linea orizzontale dinamica della lunghezza esatta del messaggio
        String line = "─".repeat(length);

        System.out.println("\n┌" + line + "┐");
        System.out.println("│" + errorMsg + "│");
        System.out.println("└" + line + "┘\n");
    }

    /**
     * Stampa un banner di benvenuto o un errore (usato post-configurazione
     * UDP).
     */
    public static void printPresentation(Response response) {
        if (response.responseCode >= 400) {
            printError(response.responseCode, response.errorMessage);
        } else {
            System.out.println(Menu.TITLE);
        }
    }

    /**
     * Mostra le statistiche globali (live o storiche) di una specifica partita.
     */
    public static void printGameStats(GameStatsResponse stats) {
        if (stats.responseCode >= 400) {
            printError(stats.responseCode, stats.errorMessage);
            return;
        }

        System.out.println("\n═════════════════════════════════════════");
        System.out.println("         STATISTICHE DELLA PARTITA");
        System.out.println("═════════════════════════════════════════\n");

        if (stats.finish) {
            System.out.println("  • Partecipanti totali: " + stats.totalParticipants);
            System.out.println("  • Giocatori che hanno vinto: " + stats.playersWon);
            System.out.println("  • Giocatori che hanno terminato: " + stats.playersFinished);
            System.out.println("  • Media punti partita: " + stats.averageScore);
        } else {
            System.out.println("  • Tempo rimanente: " + Tempo.displayTempo(stats.remainingTime));
            System.out.println("  • Giocatori ancora in partita: " + stats.playersInGame);
            System.out.println("  • Giocatori che hanno vinto: " + stats.playersWon);
            System.out.println("  • Giocatori che hanno terminato: " + stats.playersFinished);
        }
        System.out.println("═══════════════════════════════════════\n");
    }

    /**
     * Mostra le informazioni della partita. Differenzia tra la stampa delle
     * soluzioni finali (se conclusa) e lo stato di gioco corrente (parole
     * rimanenti, punteggio live).
     */
    public static void printGameInfo(GameInfoResponse info) {
        if (info.responseCode >= 400) {
            printError(info.responseCode, info.errorMessage);
            return;
        }
        if (info.isGameEnded) {
            if (info.solutions instanceof List<Group>) {
                System.out.println("\n═════════════════════════════════════════════════════════");
                System.out.println("         INFORMAZIONI PERSONALI (PARTITA ID: " + info.errorMessage + ")");
                System.out.println("═════════════════════════════════════════════════════════\n");

                System.out.println("Soluzioni:");
                for (Group elem : info.solutions) {
                    System.out.printf("  • %s: %s%n", elem.getTheme().toUpperCase(), String.join(", ", elem.getWords()));
                }

                System.out.println("\nStatistiche:");
                System.out.println("  • Proposte corrette: " + info.numCorrect);
                System.out.println("  • Errori: " + info.mistakes);
                System.out.println("  • Punteggio: " + info.score);
                System.out.println();
            }
        } else {
            System.out.println("\n═════════════════════════════════════════════════════════");
            System.out.println("         INFORMAZIONI PERSONALI (PARTITA CORRENTE)");
            System.out.println("═════════════════════════════════════════════════════════\n");
            System.out.println(" • Tempo rimanente: " + Tempo.displayTempo(info.remainingTime));
            System.out.println(" • Parole rimanenti: " + info.remainingWords);
            System.out.println(" • Proposte corrette: " + info.correctGroups);
            System.out.println(" • Punteggio corrente: " + info.score);
            System.out.println(" • Numero di errori: " + info.mistakes);
        }

        System.out.println("═══════════════════════════════════════\n");
    }

    /**
     * Mostra a schermo all'utente sull'esito della sua ultima proposta. Mostra
     * anche i banner di vittoria o sconfitta se opporturno.
     */
    public static void printProposal(Response response) {
        if (response.responseCode >= 400) {
            printError(response.responseCode, response.errorMessage);
        } else {
            if (response instanceof ProposalResponse proposalResponse) {

                if (proposalResponse.win) {
                    System.out.println(Menu.VICTORY);
                    return;
                }
                if (proposalResponse.lose) {
                    System.out.println(Menu.DEFEAT);
                    return;
                }

                System.out.println("\n════════════════════════════════════════════════");
                System.out.println("              RISULTATO PROPOSTA");
                System.out.println("════════════════════════════════════════════════\n");

                String messaggio = proposalResponse.correctPropose ? "CORRETTA" : "ERRATA";
                System.out.println("• Esito Proposta: " + messaggio);
                System.out.println("• Punteggio: " + proposalResponse.score);

            }
        }
    }

    /**
     * Stampa l'esito dell'aggiornamento credenziali.
     */
    public static void printUpdateCredential(Response response) {
        if (response.responseCode >= 400) {
            printError(response.responseCode, response.errorMessage);
        } else {
            System.out.println("\n══════════════════════════════════════════════════");
            System.out.println("         CREDENZIALI AGGIORNATE CORRETTAMEMTE");
            System.out.println("══════════════════════════════════════════════════\n");
        }
    }

    /**
     * Stampa l'esito del logout.
     *
     * @return Sempre false (usato dal chiamante per aggiornare il flag
     * isLogged).
     */
    public static boolean printLogout(Response response) {
        if (response.responseCode >= 400) {
            printError(response.responseCode, response.errorMessage);
        } else {
            System.out.println("\n══════════════════════════════════════════════════");
            System.out.println("             LOGOUT ESEGUITO");
            System.out.println("══════════════════════════════════════════════════\n");
        }
        return false;
    }

    /**
     * Mostra i dati iniziali di gioco dopo un login riuscito.
     *
     * @return true se il login ha avuto successo, false altrimenti.
     */
    public static boolean printLogin(LoginResponse login) {
        if (login.responseCode >= 400) {
            printError(login.responseCode, login.errorMessage);
            return false;
        } else {
            System.out.println("\n═════════════════════════════════════════════════════════════");
            System.out.println("           DATI PARTITA CORRENTE (ID: " + login.errorMessage + ")");
            System.out.println("═════════════════════════════════════════════════════════════\n");
            System.out.println("• Tempo rimanente: " + Tempo.displayTempo(login.remainingTime));
            System.out.println("• Parole: " + login.words);
            System.out.println("• Proposte corrette: " + login.correctProposals);
            System.out.println("• Numero errori: " + login.mistakes);
            System.out.println("• Punteggio corrente: " + login.score);
            return true;
        }
    }

    /**
     * mostra a schermo l'esito della registrazione di un nuovo utente.
     */
    public static void printRegister(Response register) {
        if (register.responseCode >= 400) {
            printError(register.responseCode, register.errorMessage);
        } else {
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("         REGISTRAZIONE ESEGUITA");
            System.out.println("═══════════════════════════════════════\n");
        }
    }

    /**
     * Stampa a schermo la Leaderboard globale sotto forma di tabella, o la
     * singola posizione se richiesta la classifica relativa.
     */
    public static void printLeaderboard(LeaderboardResponse rank) {
        if (rank.responseCode >= 400) {
            printError(rank.responseCode, rank.errorMessage);
            return;
        }

        if (!rank.global) {
            // Posizione singola
            System.out.println("\n═══════════════════════════════════════");
            System.out.println("• Nome Giocatore: " + rank.username + ", Posizione: " + rank.posUser);
            System.out.println("═══════════════════════════════════════\n");

        } else {
            // Classifica globale/Top K
            System.out.println("\n" + "═".repeat(70));
            System.out.println("                      CLASSIFICA GLOBALE");
            System.out.println("═".repeat(70));

            List<UserStats> classifica = rank.leaderboard;

            // Header tabella
            System.out.printf("| %-3s | %-15s | %-10s | %-8s | %-14s |%n",
                    "POS", "GIOCATORE", "PUNTEGGIO", "WIN%", "CURRENT_STREAK");
            System.out.println("|" + "─".repeat(68) + "|");

            // Riga per ogni giocatore
            for (int i = 0; i < classifica.size(); i++) {
                UserStats player = classifica.get(i);
                String winRateStr = String.format("%.1f%%", player.winRate * 100);
                int pos = i + 1;
                String posStr = String.format("%3d", pos);

                System.out.printf("| %s | %-15s | %10d | %8s | %14d |%n",
                        posStr,
                        player.username,
                        player.totalScore,
                        winRateStr,
                        player.currentStreak);
            }

            System.out.println("═".repeat(70));
            System.out.printf("Giocatori totali: %d%n", classifica.size());
            System.out.println();
        }

    }

    /**
     * Mostra la scheda delle statistiche personali, con 'istogramma degli
     * errori,ecc
     */
    public static void printPlayerStats(PlayerStatsResponse stats) {
        if (stats.responseCode >= 400) {
            printError(stats.responseCode, stats.errorMessage);
            return;
        }

        System.out.println("\n" + "═".repeat(55));
        System.out.println("            STATISTICHE PERSONALI");
        System.out.println("═".repeat(55));

        // Statistiche generali
        System.out.printf("| %-30s | %18s |%n", "Puzzle completati", stats.puzzlesCompleted);
        System.out.printf("| %-30s | %17.1f%% |%n", "Vittorie", stats.winRate * 100);
        System.out.printf("| %-30s | %17.1f%% |%n", "Sconfitte", stats.lossRate * 100);
        System.out.printf("| %-30s | %18d |%n", "Streak corrente", stats.currentStreak);
        System.out.printf("| %-30s | %18d |%n", "Streak massima", stats.maxStreak);
        System.out.printf("| %-30s | %18d |%n", "Puzzle perfetti", stats.perfectPuzzles);

        // Istogramma errori
        if (stats.mistakeHistogram != null && !stats.mistakeHistogram.isEmpty()) {
            System.out.println("|" + "─".repeat(53) + "|");
            System.out.printf("| %-51s |%n", "DISTRIBUZIONE ERRORI");
            System.out.println("|" + "─".repeat(53) + "|");

            for (int i = 0; i <= 4; i++) {
                int count = stats.mistakeHistogram.getOrDefault(i, 0);
                String label = String.format("%d errori", i);
                System.out.printf("| %-30s | %18d |%n", label, count);
            }
        }

        System.out.println("═".repeat(55));
        System.out.println();
    }

}
