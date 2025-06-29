package via.sep2.viewmodel.auth;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.concurrent.Task;
import via.sep2.exception.AuthenticationException;
import via.sep2.model.AuthModel;
import via.sep2.model.AuthModelManager;
import via.sep2.shared.dto.UserDTO;

public class LoginViewModel {

    private final AuthModel authModel;

    private final StringProperty username = new SimpleStringProperty("");
    private final StringProperty password = new SimpleStringProperty("");
    private final StringProperty errorMessage = new SimpleStringProperty("");
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty loginSuccessful = new SimpleBooleanProperty(false);

    private final ObjectProperty<UserDTO> currentUser = new SimpleObjectProperty<>();

    public LoginViewModel() {
        this.authModel = new AuthModelManager();
        setupPropertyBindings();
    }

    public LoginViewModel(AuthModel authModel) {
        this.authModel = authModel;
        setupPropertyBindings();
    }

    public void login() {
        clearErrorMessage();
        setLoginSuccessful(false);

        if (!validateInput()) {
            return;
        }

        setIsLoading(true);

        Task<UserDTO> loginTask = new Task<UserDTO>() {
            @Override
            protected UserDTO call() throws Exception {
                return authModel.login(getUsername(), getPassword());
            }

            @Override
            protected void succeeded() {
                UserDTO user = getValue();
                setCurrentUser(user);
                setLoginSuccessful(true);
                clearPassword();
                setIsLoading(false);
            }

            @Override
            protected void failed() {
                Throwable exception = getException();
                if (exception instanceof AuthenticationException) {
                    setErrorMessage(exception.getMessage());
                } else {
                    setErrorMessage("An unexpected error occurred. Please try again.");
                }
                setIsLoading(false);
            }
        };

        Thread loginThread = new Thread(loginTask);
        loginThread.setDaemon(true);
        loginThread.start();
    }

    public void reset() {
        setUsername("");
        setPassword("");
        clearErrorMessage();
        setIsLoading(false);
        setLoginSuccessful(false);
        setCurrentUser(null);
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
                !isLoading();
    }

    private void setupPropertyBindings() {
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
}
