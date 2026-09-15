package protocol.request;

public class RequestGameInfoRequest extends Request {

    public Integer gameId;
    public boolean inCorso;

    public RequestGameInfoRequest() {
        super("requestGameInfo");
    }

    public RequestGameInfoRequest(Integer gameId, boolean inCorso) {
        super("requestGameInfo");
        this.gameId = gameId;
        this.inCorso = inCorso;
    }
}
