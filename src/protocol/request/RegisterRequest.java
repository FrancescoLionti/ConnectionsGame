package protocol.request;

public class RegisterRequest extends Request {

    public String username;
    public String psw;

    public RegisterRequest() {
        super("register");
    }

    public RegisterRequest(String username, String psw) {
        super("register");
        this.username = username;
        this.psw = psw;
    }

}
