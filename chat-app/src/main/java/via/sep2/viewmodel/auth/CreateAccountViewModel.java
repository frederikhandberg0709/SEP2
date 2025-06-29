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

public class CreateAccountViewModel {

    private final AuthModel authModel;

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

    public CreateAccountViewModel() {
        this.authModel = new AuthModelManager();
        setupValidationListeners();
    }

    public CreateAccountViewModel(AuthModel authModel) {
        this.authModel = authModel;
        setupValidationListeners();
    }

    public void createAccount() {
        clearMessages();
        setAccountCreated(false);

        if (!validateAllInput()) {
            return;
        }

        setIsLoading(true);

        Task<UserDTO> createAccountTask = new Task<UserDTO>() {
            @Override
            protected UserDTO call() throws Exception {
                return authModel.createAccount(
                        getUsername().trim(),
                        getPassword(),
                        getFirstName().trim(),
                        getLastName().trim());
            }

            @Override
            protected void succeeded() {
                UserDTO user = getValue();
                setCreatedUser(user);
                setAccountCreated(true);
                setSuccessMessage("Account created successfully! You can now log in.");
                clearPasswords();
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

        Thread createAccountThread = new Thread(createAccountTask);
        createAccountThread.setDaemon(true);
        createAccountThread.start();
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

        Task<Boolean> checkTask = new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                return authModel.usernameExists(usernameText);
            }

            @Override
            protected void succeeded() {
                Boolean exists = getValue();
                if (exists) {
                    setUsernameValidation("Username is already taken");
                } else {
                    setUsernameValidation("Username is available");
                }
            }

            @Override
            protected void failed() {
                setUsernameValidation("Could not check username availability");
            }
        };

        Thread checkThread = new Thread(checkTask);
        checkThread.setDaemon(true);
        checkThread.start();
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
                authModel.isValidPassword(getPassword()) &&
                !isLoading() &&
                !getUsernameValidation().contains("taken") &&
                !getUsernameValidation().contains("Could not check");
    }

    private void setupValidationListeners() {
        passwordProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty()) {
                setPasswordValidation("");
            } else if (!authModel.isValidPassword(newVal)) {
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
                checkUsernameAvailability();
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
        } else if (!authModel.isValidPassword(getPassword())) {
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
