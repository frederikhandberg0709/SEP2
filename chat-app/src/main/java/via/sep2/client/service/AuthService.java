package via.sep2.client.service;

import java.rmi.RemoteException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import via.sep2.client.connection.ConnectionManager;
import via.sep2.client.event.events.LoginSuccessEvent;
import via.sep2.shared.dto.UserDTO;

public class AuthService {

    private static final Logger logger = Logger.getLogger(AuthService.class.getName());
    private final ConnectionManager connectionManager;

    public AuthService() {
        this.connectionManager = ConnectionManager.getInstance();
    }

    // For testing
    public AuthService(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public CompletableFuture<UserDTO> loginAsync(String username, String password) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Attempting login for user: " + username);

                connectionManager.connect();

                UserDTO user = connectionManager.getRmiClient().login(username, password);

                connectionManager.getSessionState().handleLogin(user);

                connectionManager.getEventBus().publish(new LoginSuccessEvent(user));

                logger.info("Login successful for user: " + username);
                return user;

            } catch (Exception e) {
                logger.warning("Login failed for user " + username + ": " + e.getMessage());
                throw new RuntimeException("Authentication failed: " + extractErrorMessage(e), e);
            }
        });
    }

    public CompletableFuture<UserDTO> createAccountAsync(String username, String password,
            String firstName, String lastName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Creating account for user: " + username);
                connectionManager.connect();

                UserDTO user = connectionManager.getRmiClient().createAccount(username, password, firstName, lastName);
                logger.info("Account created successfully for user: " + username);
                return user;

            } catch (Exception e) {
                logger.warning("Account creation failed for user " + username + ": " + e.getMessage());
                throw new RuntimeException("Account creation failed: " + extractErrorMessage(e), e);
            }
        });
    }

    public CompletableFuture<Boolean> checkUsernameExistsAsync(String username) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                connectionManager.connect();
                return connectionManager.getRmiClient().usernameExists(username);
            } catch (Exception e) {
                logger.warning("Error checking username: " + e.getMessage());
                return true;
            }
        });
    }

    public void logout() {
        try {
            if (connectionManager.isConnected() && connectionManager.getCurrentUser() != null) {
                connectionManager.getRmiClient().logout();
            }
        } catch (RemoteException e) {
            logger.warning("Error during logout: " + e.getMessage());
        } finally {
            connectionManager.getSessionState().handleLogout();
            logger.info("User logged out");
        }
    }

    public boolean isAuthenticated() {
        return connectionManager.getSessionState().canSendMessage();
    }

    public UserDTO getCurrentUser() {
        return connectionManager.getCurrentUser();
    }

    public boolean canPerformAction(String action) {
        switch (action.toLowerCase()) {
            case "send_message":
                return connectionManager.getSessionState().canSendMessage();
            case "join_group":
                return connectionManager.getSessionState().canJoinGroup();
            case "create_group":
                return connectionManager.getSessionState().canCreateGroup();
            case "view_public_groups":
                return connectionManager.getSessionState().canViewPublicGroups();
            default:
                return false;
        }
    }

    public String getSessionState() {
        return connectionManager.getSessionState().getStateName();
    }

    private String extractErrorMessage(Exception e) {
        if (e instanceof RemoteException) {
            RemoteException re = (RemoteException) e;
            if (re.getCause() != null && re.getCause().getMessage() != null) {
                return re.getCause().getMessage();
            }
        }
        return e.getMessage() != null ? e.getMessage() : "Unknown error occurred";
    }
}