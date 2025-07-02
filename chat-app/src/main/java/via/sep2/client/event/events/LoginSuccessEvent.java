package via.sep2.client.event.events;

import via.sep2.shared.dto.UserDTO;

public class LoginSuccessEvent {

    private final UserDTO user;

    public LoginSuccessEvent(UserDTO user) {
        this.user = user;
    }

    public UserDTO getUser() {
        return user;
    }
}
