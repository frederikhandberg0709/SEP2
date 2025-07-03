package via.sep2.client.event.events;

import via.sep2.shared.dto.UserDTO;

public class UserDemotedEvent {

    private final int roomId;
    private final UserDTO user;
    private final String demotedBy;

    public UserDemotedEvent(int roomId, UserDTO user, String demotedBy) {
        this.roomId = roomId;
        this.user = user;
        this.demotedBy = demotedBy;
    }

    public int getRoomId() {
        return roomId;
    }

    public UserDTO getUser() {
        return user;
    }

    public String getDemotedBy() {
        return demotedBy;
    }
}
