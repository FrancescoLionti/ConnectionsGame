package protocol.request;

public class RequestLeaderboardRequest extends Request {

    public String playerName;
    public int topPlayers;
    public boolean global;

    public RequestLeaderboardRequest() {
        super("requestLeaderboard");
    }

    public RequestLeaderboardRequest(boolean global, String playerName, int topPlayers) {
        super("requestLeaderboard");
        this.playerName = playerName;
        this.topPlayers = topPlayers;
        this.global = global;
    }

}
