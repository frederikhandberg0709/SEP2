package via.sep2.client.viewmodel.auth;

import java.util.concurrent.CompletableFuture;
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
import via.sep2.client.factory.ServiceFactory;
import via.sep2.client.service.AuthService;
import via.sep2.shared.dto.UserDTO;

public class CreateAccountViewModel {

    private static final Logger logger = Logger.getLogger(CreateAccountViewModel.class.getName());

    private final AuthService authService;
    private final ConnectionManager connectionManager;

    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty confirmPassword = new SimpleStringProperty("");
    private final StringProperty firstName = new SimpleStringProperty("");
    private final StringProperty lastName = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final StringProperty successMessage = new SimpleStringProperty("");
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty accountCreated = new SimpleBooleanProperty(false);

    private final StringProperty usernameValidation = new SimpleStringProperty("");
    private final StringProperty passwordValidation = new SimpleStringProperty("");
    private final StringProperty confirmPasswordValidation = new SimpleStringProperty("");

    private final ObjectProperty<UserDTO> createdUser = new SimpleObjectProperty<>();

    private final EventListener<ConnectionLostEvent> connectionLostListener;

    public CreateAccountViewModel() {
        ServiceFactory factory = ServiceFactory.getInstance();
        this.authService = factory.getService(AuthService.class);
        this.connectionManager = ConnectionManager.getInstance();

        this.connectionLostListener = this::handleConnectionLost;

        setupValidationListeners();
        setupEventListeners();

        logger.info("CreateAccountViewModel initialized");
    }

    public CreateAccountViewModel(AuthService authService) {
        this.authService = authService;
        this.connectionManager = ConnectionManager.getInstance();

        this.connectionLostListener = this::handleConnectionLost;

        setupValidationListeners();
        setupEventListeners();
    }

    private void setupEventListeners() {
        connectionManager.getEventBus().subscribe(ConnectionLostEvent.class, connectionLostListener);
    }

    private void handleConnectionLost(ConnectionLostEvent event) {
        Platform.runLater(() -> {
            setErrorMessage("Connection lost: " + event.getReason());
            setIsLoading(false);
        });
    }

    public void createAccount() {
        clearMessages();
        setAccountCreated(false);

        if (!validateAllInput()) {
            return;
        }

        setIsLoading(true);

        authService.createAccountAsync(
                getUsername().trim(),
                getPassword(),
                getFirstName().trim(),
                getLastName().trim())
                .thenAccept(user -> {
                    Platform.runLater(() -> {
                        setCreatedUser(user);
                        setAccountCreated(true);
                        setSuccessMessage("Account created successfully! You can now log in.");
                        clearPasswords();
                        setIsLoading(false);
                        logger.info("Account created successfully for user: " + user.getUsername());
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        setErrorMessage(extractErrorMessage(throwable));
                        setIsLoading(false);
                    });
                    return null;
                });
    }

    public void checkUsernameAvailability() {
        String usernameText = getUsername().trim();

        if (usernameText.isEmpty()) {
            setUsernameValidation("");
            return;
        }

        if (usernameText.length() < 3) {
            setUsernameValidation("Username must be at least 3 characters");
            return;
        }

        if (!usernameText.matches("^[a-zA-Z0-9_]+$")) {
            setUsernameValidation("Username can only contain letters, numbers, and underscores");
            return;
        }

        authService.checkUsernameExistsAsync(usernameText)
                .thenAccept(exists -> {
                    Platform.runLater(() -> {
                        if (exists) {
                            setUsernameValidation("Username is already taken");
                        } else {
                            setUsernameValidation("Username is available ✓");
                        }
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        setUsernameValidation("Could not check username availability");
                    });
                    return null;
                });
    }

    private String extractErrorMessage(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return throwable.getMessage() != null ? throwable.getMessage() : "An unexpected error occurred";
    }

    public void reset() {
        setUsername("");
        setPassword("");
        setConfirmPassword("");
        setFirstName("");
        setLastName("");
        clearMessages();
        clearValidationMessages();
        setIsLoading(false);
        setAccountCreated(false);
        setCreatedUser(null);
    }

    public void clearPasswords() {
        setPassword("");
        setConfirmPassword("");
    }

    public void clearMessages() {
        setErrorMessage("");
        setSuccessMessage("");
    }

    public void clearValidationMessages() {
        setUsernameValidation("");
        setPasswordValidation("");
        setConfirmPasswordValidation("");
    }

    public boolean canCreateAccount() {
        return !getUsername().trim().isEmpty() &&
                !getPassword().isEmpty() &&
                !getConfirmPassword().isEmpty() &&
                !getFirstName().trim().isEmpty() &&
                !getLastName().trim().isEmpty() &&
                getPassword().equals(getConfirmPassword()) &&
                isValidPassword(getPassword()) &&
                !isLoading() &&
                !getUsernameValidation().contains("taken") &&
                !getUsernameValidation().contains("Could not check");
    }

    private void setupValidationListeners() {
        passwordProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                setPasswordValidation("");
            } else if (!isValidPassword(newVal)) {
                setPasswordValidation("Password must be at least 8 characters long");
            } else {
                setPasswordValidation("Password is valid");
            }
            validatePasswordMatch();
        });

        confirmPasswordProperty().addListener((obs, oldVal, newVal) -> {
            validatePasswordMatch();
        });

        usernameProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                debounceUsernameCheck();
            }
        });
    }

    private CompletableFuture<Void> usernameCheckFuture;

    private void debounceUsernameCheck() {
        if (usernameCheckFuture != null && !usernameCheckFuture.isDone()) {
            usernameCheckFuture.cancel(true);
        }

        usernameCheckFuture = CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500);
                Platform.runLater(this::checkUsernameAvailability);
            } catch (InterruptedException e) {
            }
        });
    }

    private void validatePasswordMatch() {
        String pwd = getPassword();
        String confirmPwd = getConfirmPassword();

        if (confirmPwd.isEmpty()) {
            setConfirmPasswordValidation("");
        } else if (!pwd.equals(confirmPwd)) {
            setConfirmPasswordValidation("Passwords do not match");
        } else {
            setConfirmPasswordValidation("Passwords match");
        }
    }

    private boolean validateAllInput() {
        StringBuilder errors = new StringBuilder();

        String usernameText = getUsername().trim();
        if (usernameText.isEmpty()) {
            errors.append("Username is required. ");
        } else if (usernameText.length() < 3) {
            errors.append("Username must be at least 3 characters. ");
        } else if (!usernameText.matches("^[a-zA-Z0-9_]+$")) {
            errors.append("Username can only contain letters, numbers, and underscores. ");
        }

        if (getPassword().isEmpty()) {
            errors.append("Password is required. ");
        } else if (!isValidPassword(getPassword())) {
            errors.append("Password must be at least 8 characters long. ");
        }

        if (!getPassword().equals(getConfirmPassword())) {
            errors.append("Passwords do not match. ");
        }

        if (getFirstName().trim().isEmpty()) {
            errors.append("First name is required. ");
        } else if (!getFirstName().trim().matches("^[a-zA-Z\\s'-]+$")) {
            errors.append("First name contains invalid characters. ");
        }

        if (getLastName().trim().isEmpty()) {
            errors.append("Last name is required. ");
        } else if (!getLastName().trim().matches("^[a-zA-Z\\s'-]+$")) {
            errors.append("Last name contains invalid characters. ");
        }

        if (errors.length() > 0) {
            setErrorMessage(errors.toString().trim());
            return false;
        }

        return true;
    }

    private boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public void cleanup() {
        connectionManager.getEventBus().unsubscribe(ConnectionLostEvent.class, connectionLostListener);

        if (usernameCheckFuture != null && !usernameCheckFuture.isDone()) {
            usernameCheckFuture.cancel(true);
        }

        logger.info("CreateAccountViewModel cleaned up");
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

    public StringProperty confirmPasswordProperty() {
        return confirmPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword.get();
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword.set(confirmPassword);
    }

    public StringProperty firstNameProperty() {
        return firstName;
    }

    public String getFirstName() {
        return firstName.get();
    }

    public void setFirstName(String firstName) {
        this.firstName.set(firstName);
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    public String getLastName() {
        return lastName.get();
    }

    public void setLastName(String lastName) {
        this.lastName.set(lastName);
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

    public StringProperty successMessageProperty() {
        return successMessage;
    }

    public String getSuccessMessage() {
        return successMessage.get();
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage.set(successMessage);
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

    public BooleanProperty accountCreatedProperty() {
        return accountCreated;
    }

    public boolean isAccountCreated() {
        return accountCreated.get();
    }

    public void setAccountCreated(boolean accountCreated) {
        this.accountCreated.set(accountCreated);
    }

    public StringProperty usernameValidationProperty() {
        return usernameValidation;
    }

    public String getUsernameValidation() {
        return usernameValidation.get();
    }

    public void setUsernameValidation(String usernameValidation) {
        this.usernameValidation.set(usernameValidation);
    }

    public StringProperty passwordValidationProperty() {
        return passwordValidation;
    }

    public String getPasswordValidation() {
        return passwordValidation.get();
    }

    public void setPasswordValidation(String passwordValidation) {
        this.passwordValidation.set(passwordValidation);
    }

    public StringProperty confirmPasswordValidationProperty() {
        return confirmPasswordValidation;
    }

    public String getConfirmPasswordValidation() {
        return confirmPasswordValidation.get();
    }

    public void setConfirmPasswordValidation(String confirmPasswordValidation) {
        this.confirmPasswordValidation.set(confirmPasswordValidation);
    }

    public ObjectProperty<UserDTO> createdUserProperty() {
        return createdUser;
    }

    public UserDTO getCreatedUser() {
        return createdUser.get();
    }

    public void setCreatedUser(UserDTO createdUser) {
        this.createdUser.set(createdUser);
    }
}
