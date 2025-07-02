package via.sep2.client.event.events;

import via.sep2.shared.dto.UserDTO;

public class UserPromotedEvent {

    private final int roomId;
    private final UserDTO user;
    private final String promotedBy;

    public UserPromotedEvent(int roomId, UserDTO user, String promotedBy) {
        this.roomId = roomId;
        this.user = user;
        this.promotedBy = promotedBy;
    }

    public int getRoomId() {
        return roomId;
    }

    public UserDTO getUser() {
        return user;
    }

    public String getPromotedBy() {
        return promotedBy;
    }
}
