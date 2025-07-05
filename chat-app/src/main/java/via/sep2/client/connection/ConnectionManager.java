package via.sep2.client.connection;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Logger;
import via.sep2.client.event.EventBus;
import via.sep2.client.event.events.ConnectionLostEvent;
import via.sep2.client.event.events.DirectChatCreatedEvent;
import via.sep2.client.event.events.GroupChatCreatedEvent;
import via.sep2.client.event.events.MessageDeletedEvent;
import via.sep2.client.event.events.MessageEditedEvent;
import via.sep2.client.event.events.MessageReceivedEvent;
import via.sep2.client.event.events.UserDemotedEvent;
import via.sep2.client.event.events.UserJoinedGroupEvent;
import via.sep2.client.event.events.UserLeftGroupEvent;
import via.sep2.client.event.events.UserPromotedEvent;
import via.sep2.client.rmi.ChatClientImpl;
import via.sep2.client.rmi.ChatEventListener;
import via.sep2.client.state.ConnectedState;
import via.sep2.client.state.DisconnectedState;
import via.sep2.client.state.SessionState;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.DirectChatDTO;
import via.sep2.shared.dto.MessageDTO;
import via.sep2.shared.dto.UserDTO;

public class ConnectionManager {

    private static final Logger logger = Logger.getLogger(
        ConnectionManager.class.getName()
    );
    private static volatile ConnectionManager instance;
    private static final Object lock = new Object();

    private ChatClientImpl rmiClient;
    private SessionState sessionState;
    private EventBus eventBus;
    private UserDTO currentUser;

    private ConnectionManager() {
        this.sessionState = new DisconnectedState();
        this.sessionState.setContext(this);
        this.eventBus = new EventBus();
        logger.info("ConnectionManager initialized");
    }

    public static ConnectionManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ConnectionManager();
                }
            }
        }
        return instance;
    }

    public void connect() throws RemoteException, NotBoundException {
        if (rmiClient == null || !rmiClient.isConnected()) {
            logger.info("Connecting to server...");
            rmiClient = new ChatClientImpl();
            rmiClient.connect();
            setSessionState(new ConnectedState());
            setupEventForwarding();
            logger.info("Connected to server successfully");
        }
    }

    public void disconnect() {
        if (rmiClient != null) {
            rmiClient.disconnect();
            rmiClient = null;
        }
        currentUser = null;
        setSessionState(new DisconnectedState());
        logger.info("Disconnected from server");
    }

    private void setupEventForwarding() {
        rmiClient.addEventListener(
            new ChatEventListener() {
                @Override
                public void onMessageReceived(MessageDTO message) {
                    eventBus.publish(new MessageReceivedEvent(message));
                }

                @Override
                public void onMessageEdited(MessageDTO message) {
                    eventBus.publish(new MessageEditedEvent(message));
                }

                @Override
                public void onMessageDeleted(int messageId, int roomId) {
                    eventBus.publish(
                        new MessageDeletedEvent(messageId, roomId)
                    );
                }

                @Override
                public void onDirectChatCreated(DirectChatDTO directChat) {
                    eventBus.publish(new DirectChatCreatedEvent(directChat));
                }

                @Override
                public void onGroupChatCreated(ChatRoomDTO groupChat) {
                    eventBus.publish(new GroupChatCreatedEvent(groupChat));
                }

                @Override
                public void onUserJoinedGroup(
                    int roomId,
                    UserDTO user,
                    String invitedBy
                ) {
                    eventBus.publish(
                        new UserJoinedGroupEvent(roomId, user, invitedBy)
                    );
                }

                @Override
                public void onUserLeftGroup(
                    int roomId,
                    UserDTO user,
                    boolean wasRemoved,
                    String removedBy
                ) {
                    eventBus.publish(
                        new UserLeftGroupEvent(
                            roomId,
                            user,
                            wasRemoved,
                            removedBy
                        )
                    );
                }

                @Override
                public void onPromotedToAdmin(
                    int roomId,
                    UserDTO user,
                    String promotedBy
                ) {
                    eventBus.publish(
                        new UserPromotedEvent(roomId, user, promotedBy)
                    );
                }

                @Override
                public void onDemotedFromAdmin(
                    int roomId,
                    UserDTO user,
                    String demotedBy
                ) {
                    eventBus.publish(
                        new UserDemotedEvent(roomId, user, demotedBy)
                    );
                }

                @Override
                public void onDisconnect(String reason) {
                    currentUser = null;
                    setSessionState(new DisconnectedState());
                    eventBus.publish(new ConnectionLostEvent(reason));
                }
            }
        );
    }

    // Getters and setters
    public ChatClientImpl getRmiClient() {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server");
        }
        return rmiClient;
    }

    public SessionState getSessionState() {
        return sessionState;
    }

    public void setSessionState(SessionState state) {
        this.sessionState = state;
        state.setContext(this);
        logger.info("Session state changed to: " + state.getStateName());
    }

    public EventBus getEventBus() {
        return eventBus;
    }

    public boolean isConnected() {
        return rmiClient != null && rmiClient.isConnected();
    }

    public UserDTO getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(UserDTO user) {
        this.currentUser = user;
        if (user != null) {
            logger.info("Current user set to: " + user.getUsername());
        } else {
            logger.info("Current user cleared");
        }
    }

    public void shutdown() {
        disconnect();
        eventBus.shutdown();
        logger.info("ConnectionManager shutdown");
    }
}
