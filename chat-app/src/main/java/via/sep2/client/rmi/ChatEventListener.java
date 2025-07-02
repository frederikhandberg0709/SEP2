package via.sep2.client.rmi;

import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.DirectChatDTO;
import via.sep2.shared.dto.MessageDTO;
import via.sep2.shared.dto.UserDTO;

public interface ChatEventListener {
    void onMessageReceived(MessageDTO message);

    void onDirectChatCreated(DirectChatDTO directChat);

    void onGroupChatCreated(ChatRoomDTO groupChat);

    void onUserJoinedGroup(int roomId, UserDTO user, String invitedBy);

    void onUserLeftGroup(int roomId, UserDTO user, boolean wasRemoved, String removedBy);

    void onPromotedToAdmin(int roomId, UserDTO user, String promotedBy);

    void onDemotedFromAdmin(int roomId, UserDTO user, String demotedBy);

    void onDisconnect(String reason);
}
