package protocol.request;

import java.util.List;

public class SubmitProposalRequest extends Request {

    public List<String> words;

    public SubmitProposalRequest() {
        super("submitProposal");
    }

    public SubmitProposalRequest(List<String> words) {
        super("submitProposal");
        this.words = words;
    }

}
