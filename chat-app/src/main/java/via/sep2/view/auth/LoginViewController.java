package via.sep2.view.auth;

import java.io.IOException;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import via.sep2.viewmodel.auth.LoginViewModel;

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

    public LoginViewController() {
        this.viewModel = new LoginViewModel();
    }

    @FXML
    private void initialize() {
        addStylesheet();
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
        try {
            navigateToCreateAccount();
        } catch (IOException e) {
            viewModel.setErrorMessage("Could not open create account page");
        }
    }

    public void resetForm() {
        viewModel.reset();
        usernameField.requestFocus();
    }

    public LoginViewModel getViewModel() {
        return viewModel;
    }

    private void addStylesheet() {
        usernameField.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                try {
                    String cssPath = getClass().getResource("/via/sep2/css/auth.css").toExternalForm();
                    newScene.getStylesheets().add(cssPath);
                } catch (Exception e) {
                    System.out.println("Could not load CSS file: " + e.getMessage());
                }
            }
        });
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

    private void navigateToCreateAccount() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/via/sep2/fxml/auth/CreateAccountView.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) createAccountButton.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Create Account - Chat App");
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
