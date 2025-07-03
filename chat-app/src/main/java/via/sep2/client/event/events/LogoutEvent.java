package via.sep2.client.event.events;

import via.sep2.shared.dto.UserDTO;

public class LogoutEvent {

    private final UserDTO user;
    private final String reason;

    public LogoutEvent(UserDTO user) {
        this.user = user;
        this.reason = "User requested logout";
    }

    public LogoutEvent(UserDTO user, String reason) {
        this.user = user;
        this.reason = reason;
    }

    public UserDTO getUser() {
        return user;
    }

    public String getReason() {
        return reason;
    }
}
