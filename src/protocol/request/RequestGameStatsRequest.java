package protocol.request;

public class RequestGameStatsRequest extends Request {

    public Integer gameId; //se gameId negativo allora si tratta di partita corrente
    public Boolean inCorso;

    public RequestGameStatsRequest() {
        super("requestGameStats");
    }

    public RequestGameStatsRequest(Integer gameId, Boolean inCorso) {
        super("requestGameStats");
        this.gameId = gameId;
        this.inCorso = inCorso;
    }
}
