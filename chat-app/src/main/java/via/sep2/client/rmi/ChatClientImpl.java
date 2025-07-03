package via.sep2.client.rmi;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import via.sep2.shared.dto.ChatMemberDTO;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.DirectChatDTO;
import via.sep2.shared.dto.MessageDTO;
import via.sep2.shared.dto.UserDTO;
import via.sep2.shared.interfaces.ChatClientCallbackInterface;
import via.sep2.shared.interfaces.ChatServerInterface;

public class ChatClientImpl extends UnicastRemoteObject implements ChatClientCallbackInterface {

    private static final Logger logger = Logger.getLogger(ChatClientImpl.class.getName());
    private static final String SERVER_NAME = "ChatServer";
    private static final String SERVER_HOST = "localhost";
    private static final int RMI_PORT = 1099;

    private ChatServerInterface server;
    private UserDTO currentUser;
    private boolean connected = false;

    private final List<ChatEventListener> eventListeners = new CopyOnWriteArrayList<>();

    public ChatClientImpl() throws RemoteException {
        super();
    }

    public void connect() throws RemoteException, NotBoundException {
        logger.info("Connecting to chat server...");

        Registry registry = LocateRegistry.getRegistry(SERVER_HOST, RMI_PORT);
        server = (ChatServerInterface) registry.lookup(SERVER_NAME);
        connected = true;

        logger.info("Connected to chat server");
    }

    public void disconnect() {
        if (connected && currentUser != null) {
            try {
                server.logout(currentUser.getUsername());
                logger.info("Disconnected from chat server");
            } catch (RemoteException e) {
                logger.warning("Error during disconnect: " + e.getMessage());
            }
        }
        connected = false;
        currentUser = null;
    }

    public boolean isConnected() {
        return connected && server != null;
    }

    public UserDTO login(String username, String password) throws RemoteException {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        currentUser = server.login(username, password);
        server.registerClient(username, this);

        logger.info("Logged in as: " + username);
        return currentUser;
    }

    public UserDTO createAccount(String username, String password, String firstName, String lastName)
            throws RemoteException {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        return server.createAccount(username, password, firstName, lastName);
    }

    public boolean usernameExists(String username) throws RemoteException {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        return server.usernameExists(username);
    }

    public void logout() throws RemoteException {
        if (!connected || currentUser == null) {
            return;
        }

        server.logout(currentUser.getUsername());
        currentUser = null;
        logger.info("Logged out successfully");
    }

    public void registerForChat(String username) throws RemoteException {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        server.registerClient(username, this);
        logger.info("Registered for chat notifications: " + username);
    }

    public DirectChatDTO createDirectChat(String otherUser) throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        return server.createDirectChat(currentUser.getUsername(), otherUser);
    }

    public DirectChatDTO getDirectChat(String otherUser) throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        return server.getDirectChat(currentUser.getUsername(), otherUser);
    }

    public List<DirectChatDTO> getMyDirectChats() throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        return server.getUserDirectChats(currentUser.getUsername());
    }

    public ChatRoomDTO createGroupChat(String roomName, String description, boolean isPrivate, int maxMembers)
            throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        return server.createGroupChat(roomName, currentUser.getUsername(), description, isPrivate, maxMembers);
    }

    public List<ChatRoomDTO> getPublicGroupChats() throws RemoteException {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        return server.getPublicGroupChats();
    }

    public List<ChatRoomDTO> getMyGroupChats() throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        return server.getUserGroupChats(currentUser.getUsername());
    }

    public void joinGroupChat(int roomId) throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        server.joinGroupChat(currentUser.getUsername(), roomId);
    }

    public void leaveGroupChat(int roomId) throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        server.leaveGroupChat(currentUser.getUsername(), roomId);
    }

    public List<ChatMemberDTO> getGroupChatMembers(int roomId) throws RemoteException {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        return server.getGroupChatMembers(roomId);
    }

    public void sendDirectMessage(int directChatId, String content) throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        MessageDTO message = new MessageDTO();
        message.setRoomId(-directChatId);
        message.setSenderUsername(currentUser.getUsername());
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis());

        server.sendMessage(message);
    }

    public void sendGroupMessage(int roomId, String content) throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        MessageDTO message = new MessageDTO();
        message.setRoomId(roomId);
        message.setSenderUsername(currentUser.getUsername());
        message.setContent(content);
        message.setTimestamp(System.currentTimeMillis());

        server.sendMessage(message);
    }

    public List<MessageDTO> getDirectChatMessages(int directChatId, int limit) throws RemoteException {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        return server.getDirectChatMessages(directChatId, limit);
    }

    public List<MessageDTO> getGroupChatMessages(int roomId, int limit) throws RemoteException {
        if (!connected) {
            throw new IllegalStateException("Not connected to server");
        }

        return server.getGroupChatMessages(roomId, limit);
    }

    public void promoteToAdmin(String username, int roomId) throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        server.promoteToAdmin(currentUser.getUsername(), username, roomId);
    }

    public void demoteFromAdmin(String username, int roomId) throws RemoteException {
        if (!connected || currentUser == null) {
            throw new IllegalStateException("Not connected or not logged in");
        }

        server.demoteFromAdmin(currentUser.getUsername(), username, roomId);
    }

    @Override
    public void onMessageReceived(MessageDTO message) throws RemoteException {
        logger.info("Received message from " + message.getSenderUsername());
        notifyListeners(listener -> listener.onMessageReceived(message));
    }

    @Override
    public void onDirectChatCreated(DirectChatDTO directChat) throws RemoteException {
        logger.info("Direct chat created: " + directChat.getId());
        notifyListeners(listener -> listener.onDirectChatCreated(directChat));
    }

    @Override
    public void onGroupChatCreated(ChatRoomDTO groupChat) throws RemoteException {
        logger.info("Group chat created: " + groupChat.getName());
        notifyListeners(listener -> listener.onGroupChatCreated(groupChat));
    }

    @Override
    public void onUserJoinedGroup(int roomId, UserDTO user, String invitedBy) throws RemoteException {
        logger.info("User " + user.getUsername() + " joined group " + roomId);
        notifyListeners(listener -> listener.onUserJoinedGroup(roomId, user, invitedBy));
    }

    @Override
    public void onUserLeftGroup(int roomId, UserDTO user, boolean wasRemoved, String removedBy) throws RemoteException {
        logger.info("User " + user.getUsername() + " left group " + roomId);
        notifyListeners(listener -> listener.onUserLeftGroup(roomId, user, wasRemoved, removedBy));
    }

    @Override
    public void onPromotedToAdmin(int roomId, UserDTO user, String promotedBy) throws RemoteException {
        logger.info("User " + user.getUsername() + " promoted to admin in group " + roomId);
        notifyListeners(listener -> listener.onPromotedToAdmin(roomId, user, promotedBy));
    }

    @Override
    public void onDemotedFromAdmin(int roomId, UserDTO user, String demotedBy) throws RemoteException {
        logger.info("User " + user.getUsername() + " demoted from admin in group " + roomId);
        notifyListeners(listener -> listener.onDemotedFromAdmin(roomId, user, demotedBy));
    }

    @Override
    public void onDisconnect(String reason) throws RemoteException {
        logger.warning("Disconnected from server: " + reason);
        connected = false;
        currentUser = null;
        notifyListeners(listener -> listener.onDisconnect(reason));
    }

    public void addEventListener(ChatEventListener listener) {
        eventListeners.add(listener);
    }

    public void removeEventListener(ChatEventListener listener) {
        eventListeners.remove(listener);
    }

    private void notifyListeners(ListenerNotification notification) {
        for (ChatEventListener listener : eventListeners) {
            try {
                notification.notify(listener);
            } catch (Exception e) {
                logger.warning("Error notifying listener: " + e.getMessage());
            }
        }
    }

    @FunctionalInterface
    private interface ListenerNotification {
        void notify(ChatEventListener listener);
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }
}
