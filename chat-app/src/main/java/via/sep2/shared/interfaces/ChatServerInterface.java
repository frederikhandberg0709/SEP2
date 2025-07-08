package via.sep2.shared.interfaces;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import via.sep2.shared.dto.ChatMemberDTO;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.DirectChatDTO;
import via.sep2.shared.dto.MessageDTO;
import via.sep2.shared.dto.UserDTO;

public interface ChatServerInterface extends Remote {
        // Authentication
        UserDTO login(String username, String password) throws RemoteException;

        UserDTO createAccount(
                        String username,
                        String password,
                        String firstName,
                        String lastName) throws RemoteException;

        void logout(String username) throws RemoteException;

        boolean usernameExists(String username) throws RemoteException;

        // Search
        List<UserDTO> searchUsers(String searchTerm, int limit)
                        throws RemoteException;

        // Direct chat
        DirectChatDTO createDirectChat(String user1, String user2)
                        throws RemoteException;

        DirectChatDTO getDirectChat(String user1, String user2)
                        throws RemoteException;

        List<DirectChatDTO> getUserDirectChats(String username)
                        throws RemoteException;

        // Group chat
        ChatRoomDTO createGroupChat(
                        String roomName,
                        String creatorUsername,
                        String description,
                        boolean isPrivate,
                        int maxMembers) throws RemoteException;

        ChatRoomDTO getGroupChatById(int roomId) throws RemoteException;

        List<ChatRoomDTO> getPublicGroupChats() throws RemoteException;

        List<ChatRoomDTO> getUserGroupChats(String username) throws RemoteException;

        void updateGroupName(int roomId, String newName) throws RemoteException;

        void addUserToGroup(int roomId, String username, String inviterUsername) throws RemoteException;

        void removeUserFromGroup(int roomId, String username, String removerUsername) throws RemoteException;

        // Group chat membership
        void joinGroupChat(String username, int roomId) throws RemoteException;

        void leaveGroupChat(String username, int roomId) throws RemoteException;

        List<ChatMemberDTO> getGroupChatMembers(int roomId) throws RemoteException;

        // Admin
        void promoteToAdmin(
                        String promoterUsername,
                        String targetUsername,
                        int roomId) throws RemoteException;

        void demoteFromAdmin(
                        String demoterUsername,
                        String targetUsername,
                        int roomId) throws RemoteException;

        // Message
        void sendMessage(MessageDTO message) throws RemoteException;

        void editMessage(int messageId, String newContent, String editorUsername)
                        throws RemoteException;

        void deleteMessage(int messageId, String deleterUsername)
                        throws RemoteException;

        List<MessageDTO> getGroupChatMessages(int roomId, int limit)
                        throws RemoteException;

        List<MessageDTO> getDirectChatMessages(int directChatId, int limit)
                        throws RemoteException;

        // Client callback registration
        void registerClient(String username, ChatClientCallbackInterface client)
                        throws RemoteException;

        void unregisterClient(String username) throws RemoteException;
}
