package via.sep2.client.event.events;

public class GroupNameUpdatedEvent {

    private final int roomId;
    private final String newName;

    public GroupNameUpdatedEvent(int roomId, String newName) {
        this.roomId = roomId;
        this.newName = newName;
    }

    public int getRoomId() {
        return roomId;
    }

    public String getNewName() {
        return newName;
    }
}
