package via.sep2.client.view.auth;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import via.sep2.client.util.SceneManager;
import via.sep2.client.viewmodel.auth.CreateAccountViewModel;

public class CreateAccountViewController {

    @FXML
    private TextField firstNameField;
    @FXML
    private TextField lastNameField;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmPasswordField;
    @FXML
    private Button createAccountButton;
    @FXML
    private Button loginButton;
    @FXML
    private Label errorLabel;
    @FXML
    private Label successLabel;
    @FXML
    private Label usernameValidationLabel;
    @FXML
    private Label passwordValidationLabel;
    @FXML
    private Label confirmPasswordValidationLabel;

    private CreateAccountViewModel viewModel;
    private SceneManager sceneManager;

    public CreateAccountViewController() {
        this.viewModel = new CreateAccountViewModel();
        this.sceneManager = sceneManager.getInstance();
    }

    @FXML
    private void initialize() {
        setupDataBinding();
        setupEventHandlers();
        setupValidation();
    }

    @FXML
    private void handleCreateAccount() {
        viewModel.createAccount();
    }

    @FXML
    private void handleLogin() {
        sceneManager.showLogin();
    }

    // For testing
    public void triggerCreateAccount() {
        handleCreateAccount();
    }

    public void triggerLogin() {
        handleLogin();
    }

    public void resetForm() {
        viewModel.reset();
        firstNameField.requestFocus();
    }

    public CreateAccountViewModel getViewModel() {
        return viewModel;
    }

    private void setupDataBinding() {
        firstNameField.textProperty().bindBidirectional(viewModel.firstNameProperty());
        lastNameField.textProperty().bindBidirectional(viewModel.lastNameProperty());
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());
        confirmPasswordField.textProperty().bindBidirectional(viewModel.confirmPasswordProperty());

        errorLabel.textProperty().bind(viewModel.errorMessageProperty());
        successLabel.textProperty().bind(viewModel.successMessageProperty());

        createAccountButton.disableProperty().bind(
                viewModel.isLoadingProperty()
                        .or(viewModel.usernameProperty().isEmpty())
                        .or(viewModel.passwordProperty().isEmpty())
                        .or(viewModel.confirmPasswordProperty().isEmpty())
                        .or(viewModel.firstNameProperty().isEmpty())
                        .or(viewModel.lastNameProperty().isEmpty()));

        usernameValidationLabel.textProperty().bind(viewModel.usernameValidationProperty());
        passwordValidationLabel.textProperty().bind(viewModel.passwordValidationProperty());
        confirmPasswordValidationLabel.textProperty().bind(viewModel.confirmPasswordValidationProperty());

        errorLabel.visibleProperty().bind(viewModel.errorMessageProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());

        successLabel.visibleProperty().bind(viewModel.successMessageProperty().isNotEmpty());
        successLabel.managedProperty().bind(successLabel.visibleProperty());

        usernameValidationLabel.visibleProperty().bind(viewModel.usernameValidationProperty().isNotEmpty());
        usernameValidationLabel.managedProperty().bind(usernameValidationLabel.visibleProperty());

        passwordValidationLabel.visibleProperty().bind(viewModel.passwordValidationProperty().isNotEmpty());
        passwordValidationLabel.managedProperty().bind(passwordValidationLabel.visibleProperty());

        confirmPasswordValidationLabel.visibleProperty()
                .bind(viewModel.confirmPasswordValidationProperty().isNotEmpty());
        confirmPasswordValidationLabel.managedProperty().bind(confirmPasswordValidationLabel.visibleProperty());
    }

    private void setupEventHandlers() {
        firstNameField.setOnAction(e -> lastNameField.requestFocus());
        lastNameField.setOnAction(e -> usernameField.requestFocus());
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> confirmPasswordField.requestFocus());
        confirmPasswordField.setOnAction(e -> {
            if (viewModel.canCreateAccount()) {
                handleCreateAccount();
            }
        });

        firstNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                viewModel.clearMessages();
            }
        });

        lastNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                viewModel.clearMessages();
            }
        });

        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                viewModel.clearMessages();
            }
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                viewModel.clearMessages();
            }
        });

        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                viewModel.clearMessages();
            }
        });
    }

    private void setupValidation() {
        viewModel.accountCreatedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                handleSuccessfulAccountCreation();
            }
        });

        setupValidationStyling();
    }

    private void setupValidationStyling() {
        viewModel.usernameValidationProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.contains("available")) {
                usernameValidationLabel.getStyleClass().removeAll("error-text", "warning-text");
                usernameValidationLabel.getStyleClass().add("success-text");
            } else if (newVal.contains("taken") || newVal.contains("invalid")) {
                usernameValidationLabel.getStyleClass().removeAll("success-text", "warning-text");
                usernameValidationLabel.getStyleClass().add("error-text");
            } else {
                usernameValidationLabel.getStyleClass().removeAll("success-text", "error-text");
                usernameValidationLabel.getStyleClass().add("warning-text");
            }
        });

        viewModel.passwordValidationProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.contains("valid")) {
                passwordValidationLabel.getStyleClass().removeAll("error-text");
                passwordValidationLabel.getStyleClass().add("success-text");
            } else {
                passwordValidationLabel.getStyleClass().removeAll("success-text");
                passwordValidationLabel.getStyleClass().add("error-text");
            }
        });

        viewModel.confirmPasswordValidationProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.contains("match")) {
                confirmPasswordValidationLabel.getStyleClass().removeAll("error-text");
                confirmPasswordValidationLabel.getStyleClass().add("success-text");
            } else {
                confirmPasswordValidationLabel.getStyleClass().removeAll("success-text");
                confirmPasswordValidationLabel.getStyleClass().add("error-text");
            }
        });
    }

    private void handleSuccessfulAccountCreation() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(3), e -> {
            sceneManager.showLogin();
        }));
        timeline.play();
    }
}
