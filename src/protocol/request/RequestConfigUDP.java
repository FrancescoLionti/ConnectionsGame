package protocol.request;

public class RequestConfigUDP extends Request {

    public int udpClientPort;

    public RequestConfigUDP(int udpClientPort) {
        super("presentationUDP");
        this.udpClientPort = udpClientPort;
    }
}
