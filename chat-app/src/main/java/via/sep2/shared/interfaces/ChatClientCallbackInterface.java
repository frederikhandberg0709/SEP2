package via.sep2.shared.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.DirectChatDTO;
import via.sep2.shared.dto.MessageDTO;
import via.sep2.shared.dto.UserDTO;

public interface ChatClientCallbackInterface extends Remote {
    // Message notifications
    void onMessageReceived(MessageDTO message) throws RemoteException;

    void onMessageEdited(MessageDTO message) throws RemoteException;

    void onMessageDeleted(int messageId, int roomId) throws RemoteException;

    // Direct chat notifications
    void onDirectChatCreated(DirectChatDTO directChat) throws RemoteException;

    // Group chat notifications
    void onGroupChatCreated(ChatRoomDTO groupChat) throws RemoteException;

    void onUserJoinedGroup(int roomId, UserDTO user, String invitedBy)
        throws RemoteException;

    void onUserLeftGroup(
        int roomId,
        UserDTO user,
        boolean wasRemoved,
        String removedBy
    ) throws RemoteException;

    // Admin notifications
    void onPromotedToAdmin(int roomId, UserDTO user, String promotedBy)
        throws RemoteException;

    void onDemotedFromAdmin(int roomId, UserDTO user, String demotedBy)
        throws RemoteException;

    // System notifications
    void onDisconnect(String reason) throws RemoteException;
}
