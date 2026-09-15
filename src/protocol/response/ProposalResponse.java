package protocol.response;

/**
 * Risposta inviata dal server dopo la valutazione di una proposta di parole
 * (gruppo di 4). Indica al client l'esito della singola mossa e gli eventuali
 * cambiamenti di stato della partita.
 */
public class ProposalResponse extends Response {

    public boolean win; // Indica se con questa proposta il giocatore ha completato il gioco vincendo
    public boolean lose; // Indica se con questa proposta il giocatore ha raggiunto il quarto errore, perdendo
    public int score; // Il nuovo punteggio cumulativo aggiornato dopo aver valutato la proposta (+6 o -4)
    public boolean correctPropose; // Esito della giocata: true se il gruppo inviato è corretto, false se è sbagliato

    public ProposalResponse(int responseCode, String errorMessage, boolean win, boolean lose, int score, boolean correctPropose) {
        super("submitProposal", responseCode, errorMessage);
        this.responseCode = responseCode;
        this.errorMessage = errorMessage;
        this.win = win;
        this.lose = lose;
        this.score = score;
        this.correctPropose = correctPropose;
    }
}
