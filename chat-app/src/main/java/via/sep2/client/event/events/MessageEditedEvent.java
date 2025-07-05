package via.sep2.client.event.events;

import via.sep2.shared.dto.MessageDTO;

public class MessageEditedEvent {

    private final MessageDTO message;

    public MessageEditedEvent(MessageDTO message) {
        this.message = message;
    }

    public MessageDTO getMessage() {
        return message;
    }
}
