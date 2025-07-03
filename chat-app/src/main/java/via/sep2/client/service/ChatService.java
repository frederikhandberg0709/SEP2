package via.sep2.client.service;

import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import via.sep2.client.connection.ConnectionManager;
import via.sep2.shared.dto.ChatMemberDTO;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.DirectChatDTO;
import via.sep2.shared.dto.MessageDTO;

public class ChatService {

    private static final Logger logger = Logger.getLogger(ChatService.class.getName());
    private final ConnectionManager connectionManager;

    public ChatService() {
        this.connectionManager = ConnectionManager.getInstance();
    }

    // Testing
    ChatService(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    // Direct Chat Operations
    public CompletableFuture<List<DirectChatDTO>> getDirectChatsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            validateAuthenticated();
            try {
                return connectionManager.getRmiClient().getMyDirectChats();
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to get direct chats", e);
            }
        });
    }

    public CompletableFuture<DirectChatDTO> createDirectChatAsync(String otherUser) {
        return CompletableFuture.supplyAsync(() -> {
            validateAuthenticated();
            try {
                logger.info("Creating direct chat with user: " + otherUser);
                return connectionManager.getRmiClient().createDirectChat(otherUser);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to create direct chat", e);
            }
        });
    }

    public CompletableFuture<DirectChatDTO> getDirectChatAsync(String otherUser) {
        return CompletableFuture.supplyAsync(() -> {
            validateAuthenticated();
            try {
                return connectionManager.getRmiClient().getDirectChat(otherUser);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to get direct chat", e);
            }
        });
    }

    public CompletableFuture<List<MessageDTO>> getDirectChatMessagesAsync(int directChatId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            validateConnected();
            try {
                return connectionManager.getRmiClient().getDirectChatMessages(directChatId, limit);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to get direct chat messages", e);
            }
        });
    }

    public CompletableFuture<Void> sendDirectMessageAsync(int directChatId, String content) {
        return CompletableFuture.runAsync(() -> {
            validateAuthenticated();
            validateCanSendMessage();
            try {
                logger.info("Sending direct message to chat: " + directChatId);
                connectionManager.getRmiClient().sendDirectMessage(directChatId, content);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to send direct message", e);
            }
        });
    }

    // Group Chat Operations
    public CompletableFuture<List<ChatRoomDTO>> getPublicGroupChatsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            validateConnected();
            try {
                return connectionManager.getRmiClient().getPublicGroupChats();
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to get public group chats", e);
            }
        });
    }

    public CompletableFuture<List<ChatRoomDTO>> getMyGroupChatsAsync() {
        return CompletableFuture.supplyAsync(() -> {
            validateAuthenticated();
            try {
                return connectionManager.getRmiClient().getMyGroupChats();
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to get my group chats", e);
            }
        });
    }

    public CompletableFuture<ChatRoomDTO> createGroupChatAsync(String roomName, String description,
            boolean isPrivate, int maxMembers) {
        return CompletableFuture.supplyAsync(() -> {
            validateAuthenticated();
            validateCanCreateGroup();
            try {
                logger.info("Creating group chat: " + roomName);
                return connectionManager.getRmiClient().createGroupChat(roomName, description, isPrivate, maxMembers);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to create group chat", e);
            }
        });
    }

    public CompletableFuture<Void> joinGroupChatAsync(int roomId) {
        return CompletableFuture.runAsync(() -> {
            validateAuthenticated();
            validateCanJoinGroup();
            try {
                logger.info("Joining group chat: " + roomId);
                connectionManager.getRmiClient().joinGroupChat(roomId);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to join group chat", e);
            }
        });
    }

    public CompletableFuture<Void> leaveGroupChatAsync(int roomId) {
        return CompletableFuture.runAsync(() -> {
            validateAuthenticated();
            try {
                logger.info("Leaving group chat: " + roomId);
                connectionManager.getRmiClient().leaveGroupChat(roomId);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to leave group chat", e);
            }
        });
    }

    public CompletableFuture<List<MessageDTO>> getGroupChatMessagesAsync(int roomId, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            validateConnected();
            try {
                return connectionManager.getRmiClient().getGroupChatMessages(roomId, limit);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to get group chat messages", e);
            }
        });
    }

    public CompletableFuture<Void> sendGroupMessageAsync(int roomId, String content) {
        return CompletableFuture.runAsync(() -> {
            validateAuthenticated();
            validateCanSendMessage();
            try {
                logger.info("Sending group message to room: " + roomId);
                connectionManager.getRmiClient().sendGroupMessage(roomId, content);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to send group message", e);
            }
        });
    }

    public CompletableFuture<List<ChatMemberDTO>> getGroupChatMembersAsync(int roomId) {
        return CompletableFuture.supplyAsync(() -> {
            validateConnected();
            try {
                return connectionManager.getRmiClient().getGroupChatMembers(roomId);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to get group chat members", e);
            }
        });
    }

    // Admin Operations
    public CompletableFuture<Void> promoteToAdminAsync(String username, int roomId) {
        return CompletableFuture.runAsync(() -> {
            validateAuthenticated();
            try {
                logger.info("Promoting user " + username + " to admin in room " + roomId);
                connectionManager.getRmiClient().promoteToAdmin(username, roomId);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to promote user to admin", e);
            }
        });
    }

    public CompletableFuture<Void> demoteFromAdminAsync(String username, int roomId) {
        return CompletableFuture.runAsync(() -> {
            validateAuthenticated();
            try {
                logger.info("Demoting user " + username + " from admin in room " + roomId);
                connectionManager.getRmiClient().demoteFromAdmin(username, roomId);
            } catch (RemoteException e) {
                throw new RuntimeException("Failed to demote user from admin", e);
            }
        });
    }

    // Validation
    private void validateConnected() {
        if (!connectionManager.isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }
    }

    private void validateAuthenticated() {
        validateConnected();
        if (connectionManager.getCurrentUser() == null) {
            throw new IllegalStateException("Not authenticated");
        }
    }

    private void validateCanSendMessage() {
        if (!connectionManager.getSessionState().canSendMessage()) {
            throw new IllegalStateException("Cannot send message in current state: " +
                    connectionManager.getSessionState().getStateName());
        }
    }

    private void validateCanJoinGroup() {
        if (!connectionManager.getSessionState().canJoinGroup()) {
            throw new IllegalStateException("Cannot join group in current state: " +
                    connectionManager.getSessionState().getStateName());
        }
    }

    private void validateCanCreateGroup() {
        if (!connectionManager.getSessionState().canCreateGroup()) {
            throw new IllegalStateException("Cannot create group in current state: " +
                    connectionManager.getSessionState().getStateName());
        }
    }
}
