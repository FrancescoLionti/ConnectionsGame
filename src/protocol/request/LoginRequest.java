package protocol.request;

public class LoginRequest extends Request {

    public String username;
    public String psw;

    public LoginRequest() {
        super("login");
    }

    public LoginRequest(String username, String psw) {
        super("login");
        this.username = username;
        this.psw = psw;
    }

}
