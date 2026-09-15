package protocol.request;

public class UpdateCredentialRequest extends Request {

    public String oldUsername;
    public String newUsername;
    public String oldPsw;
    public String newPsw;

    public UpdateCredentialRequest() {
        super("updateCredentials");
    }

    public UpdateCredentialRequest(String oldUsername, String newUsername, String oldPsw, String newPsw) {
        super("updateCredentials");
        this.oldUsername = oldUsername;
        this.newUsername = newUsername;
        this.oldPsw = oldPsw;
        this.newPsw = newPsw;
    }
}
