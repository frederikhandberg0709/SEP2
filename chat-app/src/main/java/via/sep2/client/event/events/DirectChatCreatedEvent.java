package via.sep2.client.event.events;

import via.sep2.shared.dto.DirectChatDTO;

public class DirectChatCreatedEvent {

    private final DirectChatDTO directChat;

    public DirectChatCreatedEvent(DirectChatDTO directChat) {
        this.directChat = directChat;
    }

    public DirectChatDTO getDirectChat() {
        return directChat;
    }
}
