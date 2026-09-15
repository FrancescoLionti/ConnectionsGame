package client;

/**
 * classe che contiene le costanti per l'interfaccia testuale, e i menu di
 * navigazione per i diversi stati dell'utente.
 */
public class Menu {

    public static final String TITLE = "\n"
            + "╔═══════════════════════════════════════╗\n"
            + "║   ╔═╗╔═╗╔╗╔╔╗╔╔═╗╔═╗╔╦╗╦╔═╗╔╗╔╔═╗     ║\n"
            + "║   ║  ║ ║║║║║║║║╣ ║   ║ ║║ ║║║║╚═╗     ║\n"
            + "║   ╚═╝╚═╝╝╚╝╝╚╝╚═╝╚═╝ ╩ ╩╚═╝╝╚╝╚═╝     ║\n"
            + "║                                       ║\n"
            + "║          • Benvenuto! •               ║\n"
            + "╚═══════════════════════════════════════╝\n";

    public static final String NOT_LOGGED = "\n╔═══════════════════════════════════════╗\n"
            + "║         SELEZIONA AZIONE              ║\n"
            + "╠═══════════════════════════════════════╣\n"
            + "║  0 - Esci                             ║\n"
            + "║  1 - Registrati                       ║\n"
            + "║  2 - Accedi                           ║\n"
            + "║  3 - Aggiorna Credenziali             ║\n"
            + "╚═══════════════════════════════════════╝\n";

    public static final String LOGGED = "\n╔═══════════════════════════════════════╗\n"
            + "║         SELEZIONA AZIONE              ║\n"
            + "╠═══════════════════════════════════════╣\n"
            + "║  0 - Esci                             ║\n"
            + "║  1 - Aggiorna Credenziali             ║\n"
            + "║  2 - Logout                           ║\n"
            + "║  3 - Nuova Proposta                   ║\n"
            + "║  4 - Stato/Esito Partita              ║\n"
            + "║  5 - Statistiche Partita              ║\n"
            + "║  6 - Classifica Globale               ║\n"
            + "║  7 - Statistiche Personali            ║\n"
            + "╚═══════════════════════════════════════╝\n";

    public static final String VICTORY = "\n"
            + "╔═══════════════════════════════════════╗\n"
            + "║   ╦  ╦╦╔╦╗╔╦╗╔═╗╦═╗╦╔═╗  ╦            ║\n"
            + "║   ╚╗╔╝║ ║  ║ ║ ║╠╦╝║╠═╣  ║            ║\n"
            + "║    ╚╝ ╩ ╩  ╩ ╚═╝╩╚═╩╩ ╩  o            ║\n"
            + "║                                       ║\n"
            + "║        Puzzle completato!             ║\n"
            + "╚═══════════════════════════════════════╝\n";
    public static final String DEFEAT = "\n"
            + "╔═══════════════════════════════════════╗\n"
            + "║    ╔═╗╔═╗╔╦╗╔═╗  ╔═╗╦  ╦╔═╗╦═╗        ║\n"
            + "║    ║ ╦╠═╣║║║║╣   ║ ║╚╗╔╝║╣ ╠╦╝        ║\n"
            + "║    ╚═╝╩ ╩╩ ╩╚═╝  ╚═╝ ╚╝ ╚═╝╩╚═        ║\n"
            + "║                                       ║\n"
            + "║         Ritenta la prossima!          ║\n"
            + "╚═══════════════════════════════════════╝\n";

    public static final String GAME_ENDED = "\n"
            + "╔═══════════════════════════════════════╗\n"
            + "║   ╔═╗╔═╗╦═╗╔╦╗╦╔╦╗╔═╗  ╔═╗╦╔╗╔╦╔╦╗╔═╗ ║\n"
            + "║   ╠═╝╠═╣╠╦╝ ║ ║ ║ ╠═╣  ╠╣ ║║║║║ ║ ╠═╣ ║\n"
            + "║   ╩  ╩ ╩╩╚═ ╩ ╩ ╩ ╩ ╩  ╚  ╩╝╚╝╩ ╩ ╩ ╩ ║\n"
            + "║            Tempo scaduto              ║\n"
            + "╚═══════════════════════════════════════╝\n";

}
