package via.sep2.client.event.events;

import via.sep2.shared.dto.ChatRoomDTO;

public class GroupChatCreatedEvent {

    private final ChatRoomDTO groupChat;

    public GroupChatCreatedEvent(ChatRoomDTO groupChat) {
        this.groupChat = groupChat;
    }

    public ChatRoomDTO getGroupChat() {
        return groupChat;
    }
}
