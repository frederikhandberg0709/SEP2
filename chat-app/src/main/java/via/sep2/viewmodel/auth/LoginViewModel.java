package via.sep2.viewmodel.auth;

import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import via.sep2.client.connection.ConnectionManager;
import via.sep2.client.event.EventListener;
import via.sep2.client.event.events.ConnectionLostEvent;
import via.sep2.client.event.events.LoginSuccessEvent;
import via.sep2.client.factory.ServiceFactory;
import via.sep2.client.service.AuthService;
import via.sep2.shared.dto.UserDTO;

public class LoginViewModel {

    private static final Logger logger = Logger.getLogger(LoginViewModel.class.getName());

    private final AuthService authService;
    private final ConnectionManager connectionManager;

    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty loginSuccessful = new SimpleBooleanProperty(false);
    private final ObjectProperty<UserDTO> currentUser = new SimpleObjectProperty<>();
    private final StringProperty connectionStatus = new SimpleStringProperty("Disconnected");

    private final EventListener<LoginSuccessEvent> loginSuccessListener;
    private final EventListener<ConnectionLostEvent> connectionLostListener;

    public LoginViewModel() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.authService = factory.getService(AuthService.class);
        this.connectionManager = ConnectionManager.getInstance();

        this.loginSuccessListener = this::handleLoginSuccess;
        this.connectionLostListener = this::handleConnectionLost;

        setupEventListeners();
        updateConnectionStatus();

        logger.info("LoginViewModel initialized");
    }

    public LoginViewModel(AuthService authService) {
        this.authService = authService;
        this.connectionManager = ConnectionManager.getInstance();

        this.loginSuccessListener = this::handleLoginSuccess;
        this.connectionLostListener = this::handleConnectionLost;

        setupEventListeners();
        updateConnectionStatus();
    }

    private void setupEventListeners() {
        connectionManager.getEventBus().subscribe(LoginSuccessEvent.class, loginSuccessListener);

        connectionManager.getEventBus().subscribe(ConnectionLostEvent.class, connectionLostListener);
    }

    private void handleLoginSuccess(LoginSuccessEvent event) {
        Platform.runLater(() -> {
            setCurrentUser(event.getUser());
            setLoginSuccessful(true);
            clearPassword();
            updateConnectionStatus();
            logger.info("Login success handled in ViewModel");
        });
    }

    private void handleConnectionLost(ConnectionLostEvent event) {
        Platform.runLater(() -> {
            setErrorMessage("Connection lost: " + event.getReason());
            setCurrentUser(null);
            setLoginSuccessful(false);
            updateConnectionStatus();
        });
    }

    public void login() {
        clearErrorMessage();
        setLoginSuccessful(false);

        if (!validateInput()) {
            return;
        }

        setIsLoading(true);

        authService.loginAsync(getUsername(), getPassword())
                .thenAccept(user -> {
                    Platform.runLater(() -> {
                        setIsLoading(false);
                        updateConnectionStatus();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        setIsLoading(false);
                        setErrorMessage(extractErrorMessage(throwable));
                    });
                    return null;
                });
    }

    public void logout() {
        authService.logout();
        Platform.runLater(() -> {
            setCurrentUser(null);
            setLoginSuccessful(false);
            updateConnectionStatus();
        });
    }

    private void updateConnectionStatus() {
        boolean connected = connectionManager.isConnected();

        if (!connected) {
            setConnectionStatus("Disconnected");
        } else if (authService.isAuthenticated()) {
            setConnectionStatus("Authenticated as " + authService.getCurrentUser().getUsername());
        } else {
            setConnectionStatus("Connected");
        }
    }

    private String extractErrorMessage(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return throwable.getMessage() != null ? throwable.getMessage() : "An unexpected error occurred";
    }

    private boolean validateInput() {
        if (getUsername().trim().isEmpty()) {
            setErrorMessage("Please enter your username");
            return false;
        }
        if (getPassword().isEmpty()) {
            setErrorMessage("Please enter your password");
            return false;
        }
        return true;
    }

    public void reset() {
        setUsername("");
        setPassword("");
        clearErrorMessage();
        setIsLoading(false);
        setLoginSuccessful(false);
        setCurrentUser(null);
        updateConnectionStatus();
    }

    public void clearPassword() {
        setPassword("");
    }

    public void clearErrorMessage() {
        setErrorMessage("");
    }

    public boolean canLogin() {
        return !getUsername().trim().isEmpty() &&
                !getPassword().isEmpty() &&
                !isLoading() &&
                !authService.isAuthenticated();
    }

    private void setupPropertyBindings() {
    }

    public void cleanup() {
        connectionManager.getEventBus().unsubscribe(LoginSuccessEvent.class, loginSuccessListener);
        connectionManager.getEventBus().unsubscribe(ConnectionLostEvent.class, connectionLostListener);
        logger.info("LoginViewModel cleaned up");
    }

    public AuthService getAuthService() {
        return authService;
    }

    public StringProperty usernameProperty() {
        return username;
    }

    public String getUsername() {
        return username.get();
    }

    public void setUsername(String username) {
        this.username.set(username);
    }

    public StringProperty passwordProperty() {
        return password;
    }

    public String getPassword() {
        return password.get();
    }

    public void setPassword(String password) {
        this.password.set(password);
    }

    public StringProperty errorMessageProperty() {
        return errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage.get();
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage.set(errorMessage);
    }

    public BooleanProperty isLoadingProperty() {
        return isLoading;
    }

    public boolean isLoading() {
        return isLoading.get();
    }

    public void setIsLoading(boolean isLoading) {
        this.isLoading.set(isLoading);
    }

    public BooleanProperty loginSuccessfulProperty() {
        return loginSuccessful;
    }

    public boolean isLoginSuccessful() {
        return loginSuccessful.get();
    }

    public void setLoginSuccessful(boolean loginSuccessful) {
        this.loginSuccessful.set(loginSuccessful);
    }

    public ObjectProperty<UserDTO> currentUserProperty() {
        return currentUser;
    }

    public UserDTO getCurrentUser() {
        return currentUser.get();
    }

    public void setCurrentUser(UserDTO currentUser) {
        this.currentUser.set(currentUser);
    }

    public StringProperty connectionStatusProperty() {
        return connectionStatus;
    }

    public String getConnectionStatus() {
        return connectionStatus.get();
    }

    public void setConnectionStatus(String connectionStatus) {
        this.connectionStatus.set(connectionStatus);
    }
}
