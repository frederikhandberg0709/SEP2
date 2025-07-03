package via.sep2.client.event.events;

public class ConnectionLostEvent {

    private final String reason;

    public ConnectionLostEvent(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
