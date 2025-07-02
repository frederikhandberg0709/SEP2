package via.sep2.client.view.auth;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import via.sep2.client.util.SceneManager;
import via.sep2.client.viewmodel.auth.LoginViewModel;

public class LoginViewController {

    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Button loginButton;
    @FXML
    private Button createAccountButton;
    @FXML
    private Label errorLabel;

    private LoginViewModel viewModel;
    private SceneManager sceneManager;

    public LoginViewController() {
        this.viewModel = new LoginViewModel();
        this.sceneManager = sceneManager.getInstance();
    }

    @FXML
    private void initialize() {
        setupDataBinding();
        setupEventHandlers();
        setupValidation();
    }

    @FXML
    private void handleLogin() {
        viewModel.login();
    }

    @FXML
    private void handleCreateAccount() {
        sceneManager.showCreateAccount();
    }

    public void resetForm() {
        viewModel.reset();
        usernameField.requestFocus();
    }

    public LoginViewModel getViewModel() {
        return viewModel;
    }

    private void setupDataBinding() {
        usernameField.textProperty().bindBidirectional(viewModel.usernameProperty());
        passwordField.textProperty().bindBidirectional(viewModel.passwordProperty());

        errorLabel.textProperty().bind(viewModel.errorMessageProperty());

        loginButton.disableProperty().bind(
                viewModel.isLoadingProperty()
                        .or(viewModel.usernameProperty().isEmpty())
                        .or(viewModel.passwordProperty().isEmpty()));

        errorLabel.visibleProperty().bind(viewModel.errorMessageProperty().isNotEmpty());
        errorLabel.managedProperty().bind(errorLabel.visibleProperty());
    }

    private void setupEventHandlers() {
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> {
            if (viewModel.canLogin()) {
                handleLogin();
            }
        });

        usernameField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                viewModel.clearErrorMessage();
            }
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(oldVal)) {
                viewModel.clearErrorMessage();
            }
        });
    }

    private void setupValidation() {
        viewModel.loginSuccessfulProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                handleSuccessfulLogin();
            }
        });
    }

    private void handleSuccessfulLogin() {
        try {
            navigateToMainScreen();
        } catch (IOException e) {
            viewModel.setErrorMessage("Could not open main application");
        }
    }

    private void navigateToMainScreen() throws IOException {
        // TODO: replace with navigation to chat window
        System.out.println("Login successful! User: " + viewModel.getCurrentUser().getUsername());

        // FXMLLoader loader = new
        // FXMLLoader(getClass().getResource("/via/sep2/fxml/MainView.fxml"));
        // Parent root = loader.load();
        // Stage stage = (Stage) loginButton.getScene().getWindow();
        // Scene scene = new Scene(root);
        // stage.setScene(scene);
        // stage.setTitle("Chat App");
    }
}
