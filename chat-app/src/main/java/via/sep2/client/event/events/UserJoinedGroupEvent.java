package via.sep2.client.event.events;

import via.sep2.shared.dto.UserDTO;

public class UserJoinedGroupEvent {

    private final int roomId;
    private final UserDTO user;
    private final String invitedBy;

    public UserJoinedGroupEvent(int roomId, UserDTO user, String invitedBy) {
        this.roomId = roomId;
        this.user = user;
        this.invitedBy = invitedBy;
    }

    public int getRoomId() {
        return roomId;
    }

    public UserDTO getUser() {
        return user;
    }

    public String getInvitedBy() {
        return invitedBy;
    }
}
