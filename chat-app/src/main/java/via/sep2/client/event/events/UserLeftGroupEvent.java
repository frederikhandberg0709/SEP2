package via.sep2.client.event.events;

import via.sep2.shared.dto.UserDTO;

public class UserLeftGroupEvent {

    private final int roomId;
    private final UserDTO user;
    private final boolean wasRemoved;
    private final String removedBy;

    public UserLeftGroupEvent(int roomId, UserDTO user, boolean wasRemoved, String removedBy) {
        this.roomId = roomId;
        this.user = user;
        this.wasRemoved = wasRemoved;
        this.removedBy = removedBy;
    }

    public int getRoomId() {
        return roomId;
    }

    public UserDTO getUser() {
        return user;
    }

    public boolean wasRemoved() {
        return wasRemoved;
    }

    public String getRemovedBy() {
        return removedBy;
    }
}
