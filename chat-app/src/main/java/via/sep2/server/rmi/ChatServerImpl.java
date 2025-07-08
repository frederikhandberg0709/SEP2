package via.sep2.server.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import via.sep2.server.dao.DirectChatDAO;
import via.sep2.server.dao.GroupChatDAO;
import via.sep2.server.dao.MessageDAO;
import via.sep2.server.dao.UserDAO;
import via.sep2.server.model.AuthModel;
import via.sep2.server.model.AuthModelManager;
import via.sep2.shared.dto.ChatMemberDTO;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.DirectChatDTO;
import via.sep2.shared.dto.MemberRole;
import via.sep2.shared.dto.MessageDTO;
import via.sep2.shared.dto.UserDTO;
import via.sep2.shared.exception.AuthenticationException;
import via.sep2.shared.interfaces.ChatClientCallbackInterface;
import via.sep2.shared.interfaces.ChatServerInterface;

public class ChatServerImpl
        extends UnicastRemoteObject
        implements ChatServerInterface {

    private static final Logger logger = Logger.getLogger(
            ChatServerImpl.class.getName());

    private final AuthModel authModel;

    private final DirectChatDAO directChatDAO;
    private final GroupChatDAO groupChatDAO;
    private final MessageDAO messageDAO;

    private final Map<String, ChatClientCallbackInterface> clients = new ConcurrentHashMap<>();

    public ChatServerImpl() throws RemoteException {
        super();
        this.authModel = new AuthModelManager();

        this.directChatDAO = DirectChatDAO.getInstance();
        this.groupChatDAO = GroupChatDAO.getInstance();
        this.messageDAO = MessageDAO.getInstance();

        logger.info(
                "Chat server implementation initialized with database DAOs");
    }

    @Override
    public UserDTO login(String username, String password)
            throws RemoteException {
        try {
            logger.info("Login attempt for user: " + username);

            UserDTO user = authModel.login(username, password);

            logger.info("User logged in successfully: " + username);
            return user;
        } catch (AuthenticationException e) {
            logger.warning(
                    "Login failed for user " + username + ": " + e.getMessage());
            throw new RemoteException(e.getMessage());
        } catch (Exception e) {
            logger.severe("Unexpected error during login: " + e.getMessage());
            throw new RemoteException("Login failed due to server error");
        }
    }

    @Override
    public UserDTO createAccount(
            String username,
            String password,
            String firstName,
            String lastName) throws RemoteException {
        try {
            logger.info("Account creation attempt for user: " + username);

            UserDTO newUser = authModel.createAccount(
                    username,
                    password,
                    firstName,
                    lastName);

            logger.info("Account created successfully: " + username);
            return newUser;
        } catch (AuthenticationException e) {
            logger.warning(
                    "Account creation failed for user " +
                            username +
                            ": " +
                            e.getMessage());
            throw new RemoteException(e.getMessage());
        } catch (Exception e) {
            logger.severe(
                    "Unexpected error during account creation: " + e.getMessage());
            throw new RemoteException(
                    "Account creation failed due to server error");
        }
    }

    @Override
    public boolean usernameExists(String username) throws RemoteException {
        try {
            return authModel.usernameExists(username);
        } catch (Exception e) {
            logger.severe("Error checking username: " + e.getMessage());
            return true;
        }
    }

    @Override
    public void logout(String username) throws RemoteException {
        logger.info("User logging out: " + username);
        unregisterClient(username);
    }

    @Override
    public List<UserDTO> searchUsers(String searchTerm, int limit)
            throws RemoteException {
        try {
            logger.info("Searching for users with term: " + searchTerm);
            return UserDAO.getInstance().searchUsers(searchTerm, limit);
        } catch (SQLException e) {
            logger.severe("Error searching users: " + e.getMessage());
            throw new RemoteException("Error searching users");
        }
    }

    @Override
    public DirectChatDTO createDirectChat(String user1, String user2)
            throws RemoteException {
        try {
            logger.info(
                    "Creating direct chat between " + user1 + " and " + user2);
            DirectChatDTO chat = directChatDAO.createDirectChat(user1, user2);

            notifyDirectChatCreated(chat);

            return chat;
        } catch (SQLException e) {
            logger.severe("Error creating direct chat: " + e.getMessage());
            throw new RemoteException("Error creating direct chat");
        }
    }

    @Override
    public DirectChatDTO getDirectChat(String user1, String user2)
            throws RemoteException {
        try {
            return directChatDAO.getDirectChat(user1, user2);
        } catch (SQLException e) {
            logger.severe("Error getting direct chat: " + e.getMessage());
            throw new RemoteException("Error getting direct chat");
        }
    }

    @Override
    public List<DirectChatDTO> getUserDirectChats(String username)
            throws RemoteException {
        try {
            return directChatDAO.getUserDirectChats(username);
        } catch (SQLException e) {
            logger.severe("Error getting user direct chats: " + e.getMessage());
            throw new RemoteException("Error getting user direct chats");
        }
    }

    @Override
    public ChatRoomDTO createGroupChat(
            String roomName,
            String creatorUsername,
            String description,
            boolean isPrivate,
            int maxMembers) throws RemoteException {
        try {
            logger.info(
                    "Creating group chat: " + roomName + " by " + creatorUsername);
            ChatRoomDTO room = groupChatDAO.createGroupChat(
                    roomName,
                    creatorUsername,
                    description,
                    isPrivate,
                    maxMembers);

            if (!isPrivate) {
                notifyGroupChatCreated(room);
            }

            return room;
        } catch (SQLException e) {
            logger.severe("Error creating group chat: " + e.getMessage());
            throw new RemoteException("Error creating group chat");
        }
    }

    @Override
    public List<ChatRoomDTO> getPublicGroupChats() throws RemoteException {
        try {
            return groupChatDAO.getPublicGroupChats();
        } catch (SQLException e) {
            logger.severe(
                    "Error getting public group chats: " + e.getMessage());
            throw new RemoteException("Error getting public group chats");
        }
    }

    @Override
    public List<ChatRoomDTO> getUserGroupChats(String username)
            throws RemoteException {
        try {
            return groupChatDAO.getUserGroupChats(username);
        } catch (SQLException e) {
            logger.severe("Error getting user group chats: " + e.getMessage());
            throw new RemoteException("Error getting user group chats");
        }
    }

    @Override
    public void joinGroupChat(String username, int roomId)
            throws RemoteException {
        try {
            logger.info("User " + username + " joining group " + roomId);

            ChatRoomDTO room = groupChatDAO.getGroupChatById(roomId);
            if (room == null) {
                throw new RemoteException("Group chat not found");
            }

            if (groupChatDAO.isUserInGroup(username, roomId)) {
                return;
            }

            groupChatDAO.addMemberToGroup(
                    roomId,
                    username,
                    MemberRole.MEMBER,
                    "system");

            UserDTO user = authModel.getUserById(getUserIdByUsername(username));
            if (user != null) {
                notifyUserJoinedGroup(roomId, user);
            }
        } catch (SQLException e) {
            logger.severe("Error joining group chat: " + e.getMessage());
            throw new RemoteException("Error joining group chat");
        }
    }

    @Override
    public void leaveGroupChat(String username, int roomId)
            throws RemoteException {
        try {
            logger.info("User " + username + " leaving group " + roomId);
            groupChatDAO.removeMemberFromGroup(roomId, username);

            UserDTO user = authModel.getUserById(getUserIdByUsername(username));
            if (user != null) {
                notifyUserLeftGroup(roomId, user);
            }
        } catch (SQLException e) {
            logger.severe("Error leaving group chat: " + e.getMessage());
            throw new RemoteException("Error leaving group chat");
        }
    }

    @Override
    public List<ChatMemberDTO> getGroupChatMembers(int roomId)
            throws RemoteException {
        try {
            return groupChatDAO.getGroupMembers(roomId);
        } catch (SQLException e) {
            logger.severe(
                    "Error getting group chat members: " + e.getMessage());
            throw new RemoteException("Error getting group chat members");
        }
    }

    @Override
    public void promoteToAdmin(
            String promoterUsername,
            String targetUsername,
            int roomId) throws RemoteException {
        try {
            MemberRole promoterRole = groupChatDAO.getUserRole(
                    promoterUsername,
                    roomId);
            if (promoterRole != MemberRole.CREATOR &&
                    promoterRole != MemberRole.ADMIN) {
                throw new RemoteException(
                        "Only admins and creators can promote users");
            }

            groupChatDAO.updateMemberRole(
                    roomId,
                    targetUsername,
                    MemberRole.ADMIN);

            UserDTO targetUser = authModel.getUserById(
                    getUserIdByUsername(targetUsername));
            if (targetUser != null) {
                notifyUserPromotedToAdmin(roomId, targetUser, promoterUsername);
            }
        } catch (SQLException e) {
            logger.severe("Error promoting user to admin: " + e.getMessage());
            throw new RemoteException("Error promoting user to admin");
        }
    }

    @Override
    public void demoteFromAdmin(
            String demoterUsername,
            String targetUsername,
            int roomId) throws RemoteException {
        try {
            MemberRole demoterRole = groupChatDAO.getUserRole(
                    demoterUsername,
                    roomId);
            if (demoterRole != MemberRole.CREATOR) {
                throw new RemoteException("Only the creator can demote admins");
            }

            groupChatDAO.updateMemberRole(
                    roomId,
                    targetUsername,
                    MemberRole.MEMBER);

            UserDTO targetUser = authModel.getUserById(
                    getUserIdByUsername(targetUsername));
            if (targetUser != null) {
                notifyUserDemotedFromAdmin(roomId, targetUser, demoterUsername);
            }
        } catch (SQLException e) {
            logger.severe("Error demoting user from admin: " + e.getMessage());
            throw new RemoteException("Error demoting user from admin");
        }
    }

    @Override
    public void updateGroupName(int roomId, String newName) throws RemoteException {
        try {
            logger.info("Updating group name for room " + roomId + " to: " + newName);

            ChatRoomDTO room = groupChatDAO.getGroupChatById(roomId);
            if (room == null) {
                throw new RemoteException("Group chat not found");
            }

            groupChatDAO.updateGroupName(roomId, newName);

            logger.info("Successfully updated group name for room " + roomId);

        } catch (SQLException e) {
            logger.severe("Error updating group name: " + e.getMessage());
            throw new RemoteException("Error updating group name: " + e.getMessage());
        }
    }

    @Override
    public void addUserToGroup(int roomId, String username) throws RemoteException {
        try {
            logger.info("Adding user " + username + " to group " + roomId);

            ChatRoomDTO room = groupChatDAO.getGroupChatById(roomId);
            if (room == null) {
                throw new RemoteException("Group chat not found");
            }

            UserDTO user = UserDAO.getInstance().findByUsername(username);
            if (user == null) {
                throw new RemoteException("User not found");
            }

            if (groupChatDAO.isUserInGroup(username, roomId)) {
                throw new RemoteException("User is already in the group");
            }

            groupChatDAO.addMemberToGroup(roomId, username, MemberRole.MEMBER, "admin");

            notifyUserJoinedGroup(roomId, user);

            logger.info("Successfully added user " + username + " to group " + roomId);

        } catch (SQLException e) {
            logger.severe("Error adding user to group: " + e.getMessage());
            throw new RemoteException("Error adding user to group: " + e.getMessage());
        }
    }

    @Override
    public void removeUserFromGroup(int roomId, String username) throws RemoteException {
        try {
            logger.info("Removing user " + username + " from group " + roomId);

            ChatRoomDTO room = groupChatDAO.getGroupChatById(roomId);
            if (room == null) {
                throw new RemoteException("Group chat not found");
            }

            if (!groupChatDAO.isUserInGroup(username, roomId)) {
                throw new RemoteException("User is not in the group");
            }

            UserDTO user = UserDAO.getInstance().findByUsername(username);

            groupChatDAO.removeMemberFromGroup(roomId, username);

            if (user != null) {
                notifyUserLeftGroup(roomId, user);
            }

            logger.info("Successfully removed user " + username + " from group " + roomId);

        } catch (SQLException e) {
            logger.severe("Error removing user from group: " + e.getMessage());
            throw new RemoteException("Error removing user from group: " + e.getMessage());
        }
    }

    @Override
    public void sendMessage(MessageDTO message) throws RemoteException {
        try {
            logger.info(
                    "Message from " +
                            message.getSenderUsername() +
                            " in room " +
                            message.getRoomId());

            MessageDTO savedMessage = messageDAO.saveMessage(message);

            if (savedMessage.isDirectMessage()) {
                notifyDirectMessage(savedMessage);
            } else {
                notifyGroupMessage(savedMessage);
            }
        } catch (SQLException e) {
            logger.severe("Error sending message: " + e.getMessage());
            throw new RemoteException("Error sending message");
        }
    }

    @Override
    public List<MessageDTO> getGroupChatMessages(int roomId, int limit)
            throws RemoteException {
        try {
            return messageDAO.getGroupChatMessages(roomId, limit);
        } catch (SQLException e) {
            logger.severe(
                    "Error getting group chat messages: " + e.getMessage());
            throw new RemoteException("Error getting group chat messages");
        }
    }

    @Override
    public List<MessageDTO> getDirectChatMessages(int directChatId, int limit)
            throws RemoteException {
        try {
            return messageDAO.getDirectChatMessages(directChatId, limit);
        } catch (SQLException e) {
            logger.severe(
                    "Error getting direct chat messages: " + e.getMessage());
            throw new RemoteException("Error getting direct chat messages");
        }
    }

    @Override
    public void registerClient(
            String username,
            ChatClientCallbackInterface client) throws RemoteException {
        logger.info("Registering client callback for user: " + username);
        clients.put(username, client);
    }

    @Override
    public void unregisterClient(String username) throws RemoteException {
        logger.info("Unregistering client callback for user: " + username);
        clients.remove(username);
    }

    @Override
    public void editMessage(
            int messageId,
            String newContent,
            String editorUsername) throws RemoteException {
        try {
            logger.info(
                    "User " + editorUsername + " editing message " + messageId);

            MessageDTO originalMessage = messageDAO.getMessageById(messageId);
            if (originalMessage == null) {
                throw new RemoteException("Message not found");
            }

            messageDAO.editMessage(messageId, newContent, editorUsername);

            MessageDTO updatedMessage = messageDAO.getMessageById(messageId);
            if (updatedMessage != null) {
                if (updatedMessage.isDirectMessage()) {
                    notifyDirectMessageEdited(updatedMessage);
                } else {
                    notifyGroupMessageEdited(updatedMessage);
                }
            }
        } catch (SQLException e) {
            logger.severe("Error editing message: " + e.getMessage());
            throw new RemoteException(
                    "Error editing message: " + e.getMessage());
        }
    }

    @Override
    public void deleteMessage(int messageId, String deleterUsername)
            throws RemoteException {
        try {
            logger.info(
                    "User " + deleterUsername + " deleting message " + messageId);

            MessageDTO originalMessage = messageDAO.getMessageById(messageId);
            if (originalMessage == null) {
                throw new RemoteException("Message not found");
            }

            int roomId = originalMessage.getRoomId();
            boolean isDirectMessage = originalMessage.isDirectMessage();

            messageDAO.deleteMessage(messageId, deleterUsername);

            if (isDirectMessage) {
                notifyDirectMessageDeleted(messageId, roomId);
            } else {
                notifyGroupMessageDeleted(messageId, roomId);
            }
        } catch (SQLException e) {
            logger.severe("Error deleting message: " + e.getMessage());
            throw new RemoteException(
                    "Error deleting message: " + e.getMessage());
        }
    }

    private int getUserIdByUsername(String username) {
        try {
            UserDAO userDAO = UserDAO.getInstance();
            UserDTO user = userDAO.findByUsername(username);
            return user != null ? user.getId() : -1;
        } catch (Exception e) {
            logger.warning("Could not get user ID for username: " + username);
            return -1;
        }
    }

    private void notifyDirectChatCreated(DirectChatDTO chat) {
        notifyUser(chat.getUser1Username(), client -> client.onDirectChatCreated(chat));
        notifyUser(chat.getUser2Username(), client -> client.onDirectChatCreated(chat));
    }

    private void notifyGroupChatCreated(ChatRoomDTO room) {
        clients.forEach((username, client) -> {
            try {
                client.onGroupChatCreated(room);
            } catch (RemoteException e) {
                logger.warning(
                        "Failed to notify client " +
                                username +
                                ": " +
                                e.getMessage());
                clients.remove(username);
            }
        });
    }

    private void notifyUserJoinedGroup(int roomId, UserDTO user) {
        try {
            List<ChatMemberDTO> members = groupChatDAO.getGroupMembers(roomId);
            for (ChatMemberDTO member : members) {
                if (!member.getUsername().equals(user.getUsername())) {
                    notifyUser(member.getUsername(), client -> client.onUserJoinedGroup(roomId, user, "system"));
                }
            }
        } catch (SQLException e) {
            logger.severe("Error notifying group members: " + e.getMessage());
        }
    }

    private void notifyUserLeftGroup(int roomId, UserDTO user) {
        try {
            List<ChatMemberDTO> members = groupChatDAO.getGroupMembers(roomId);
            for (ChatMemberDTO member : members) {
                notifyUser(member.getUsername(), client -> client.onUserLeftGroup(roomId, user, false, null));
            }
        } catch (SQLException e) {
            logger.severe("Error notifying group members: " + e.getMessage());
        }
    }

    private void notifyUserPromotedToAdmin(
            int roomId,
            UserDTO user,
            String promotedBy) {
        try {
            List<ChatMemberDTO> members = groupChatDAO.getGroupMembers(roomId);
            for (ChatMemberDTO member : members) {
                notifyUser(member.getUsername(), client -> client.onPromotedToAdmin(roomId, user, promotedBy));
            }
        } catch (SQLException e) {
            logger.severe("Error notifying group members: " + e.getMessage());
        }
    }

    private void notifyUserDemotedFromAdmin(
            int roomId,
            UserDTO user,
            String demotedBy) {
        try {
            List<ChatMemberDTO> members = groupChatDAO.getGroupMembers(roomId);
            for (ChatMemberDTO member : members) {
                notifyUser(member.getUsername(), client -> client.onDemotedFromAdmin(roomId, user, demotedBy));
            }
        } catch (SQLException e) {
            logger.severe("Error notifying group members: " + e.getMessage());
        }
    }

    private void notifyDirectMessage(MessageDTO message) {
        try {
            DirectChatDTO chat = directChatDAO.getDirectChatById(
                    Math.abs(message.getRoomId()));
            if (chat != null) {
                notifyUser(chat.getUser1Username(), client -> client.onMessageReceived(message));
                notifyUser(chat.getUser2Username(), client -> client.onMessageReceived(message));
            }
        } catch (SQLException e) {
            logger.severe("Error notifying direct message: " + e.getMessage());
        }
    }

    private void notifyGroupMessage(MessageDTO message) {
        try {
            List<ChatMemberDTO> members = groupChatDAO.getGroupMembers(
                    message.getRoomId());
            for (ChatMemberDTO member : members) {
                notifyUser(member.getUsername(), client -> client.onMessageReceived(message));
            }
        } catch (SQLException e) {
            logger.severe("Error notifying group message: " + e.getMessage());
        }
    }

    private void notifyDirectMessageEdited(MessageDTO message) {
        try {
            DirectChatDTO chat = directChatDAO.getDirectChatById(
                    Math.abs(message.getRoomId()));
            if (chat != null) {
                notifyUser(chat.getUser1Username(), client -> client.onMessageEdited(message));
                notifyUser(chat.getUser2Username(), client -> client.onMessageEdited(message));
            }
        } catch (SQLException e) {
            logger.severe(
                    "Error notifying direct message edit: " + e.getMessage());
        }
    }

    private void notifyGroupMessageEdited(MessageDTO message) {
        try {
            List<ChatMemberDTO> members = groupChatDAO.getGroupMembers(
                    message.getRoomId());
            for (ChatMemberDTO member : members) {
                notifyUser(member.getUsername(), client -> client.onMessageEdited(message));
            }
        } catch (SQLException e) {
            logger.severe(
                    "Error notifying group message edit: " + e.getMessage());
        }
    }

    private void notifyDirectMessageDeleted(int messageId, int roomId) {
        try {
            DirectChatDTO chat = directChatDAO.getDirectChatById(
                    Math.abs(roomId));
            if (chat != null) {
                notifyUser(chat.getUser1Username(), client -> client.onMessageDeleted(messageId, roomId));
                notifyUser(chat.getUser2Username(), client -> client.onMessageDeleted(messageId, roomId));
            }
        } catch (SQLException e) {
            logger.severe(
                    "Error notifying direct message deletion: " + e.getMessage());
        }
    }

    private void notifyGroupMessageDeleted(int messageId, int roomId) {
        try {
            List<ChatMemberDTO> members = groupChatDAO.getGroupMembers(roomId);
            for (ChatMemberDTO member : members) {
                notifyUser(member.getUsername(), client -> client.onMessageDeleted(messageId, roomId));
            }
        } catch (SQLException e) {
            logger.severe(
                    "Error notifying group message deletion: " + e.getMessage());
        }
    }

    private void notifyUser(String username, ClientNotification notification) {
        ChatClientCallbackInterface client = clients.get(username);
        if (client != null) {
            try {
                notification.notify(client);
            } catch (RemoteException e) {
                logger.warning(
                        "Failed to notify client " +
                                username +
                                ": " +
                                e.getMessage());
                clients.remove(username);
            }
        }
    }

    @FunctionalInterface
    private interface ClientNotification {
        void notify(ChatClientCallbackInterface client) throws RemoteException;
    }
}
