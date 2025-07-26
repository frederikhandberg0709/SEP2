package via.sep2.client.event.events;

public class MessageDeletedEvent {

    private final int messageId;
    private final int roomId;

    public MessageDeletedEvent(int messageId, int roomId) {
        this.messageId = messageId;
        this.roomId = roomId;
    }

    public int getMessageId() {
        return messageId;
    }

    public int getRoomId() {
        return roomId;
    }
}
