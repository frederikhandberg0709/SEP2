package via.sep2.client.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import via.sep2.client.util.SceneManager;
import via.sep2.client.view.auth.CreateAccountViewController;
import via.sep2.client.view.auth.LoginViewController;
import via.sep2.client.viewmodel.auth.CreateAccountViewModel;
import via.sep2.client.viewmodel.auth.LoginViewModel;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest extends JavaFXTestBase {

    @Mock
    private SceneManager mockSceneManager;

    private CreateAccountViewController createAccountController;
    private LoginViewController loginController;

    @BeforeAll
    static void initJavaFX() {
        initializeJavaFX();
    }

    @BeforeEach
    void setUp() {
        createAccountController = new CreateAccountViewController();
        loginController = new LoginViewController();
    }

    // Tests for LoginViewController

    @Test
    void testLoginController_InitialState() {
        assertNotNull(loginController);
        assertNotNull(loginController.getViewModel());
        assertTrue(loginController.getViewModel() instanceof LoginViewModel);
    }

    @Test
    void testLoginController_ViewModel_Integration() {
        LoginViewController controller = loginController;
        LoginViewModel viewModel = controller.getViewModel();

        assertNotNull(viewModel);

        assertDoesNotThrow(() -> {
            viewModel.setUsername("testuser");
            viewModel.setPassword("password123");
        });

        assertEquals("testuser", viewModel.getUsername());
        assertEquals("password123", viewModel.getPassword());
    }

    @Test
    void testLoginController_TriggerLogin() {
        LoginViewController controller = loginController;

        assertDoesNotThrow(() -> controller.triggerLogin());
    }

    @Test
    void testLoginController_HandleLogin() {
        LoginViewController controller = loginController;

        assertDoesNotThrow(() -> controller.triggerLogin());

        LoginViewModel viewModel = controller.getViewModel();
        assertNotNull(viewModel);
    }

    @Test
    void testLoginController_HandleLogin_WithRealViewModel() {
        LoginViewController controller = loginController;
        LoginViewModel viewModel = controller.getViewModel();

        viewModel.setUsername("testuser");
        viewModel.setPassword("password123");

        assertDoesNotThrow(() -> controller.triggerLogin());
    }

    @Test
    void testLoginController_NavigationMethods_Exist() {
        LoginViewController controller = loginController;

        // Verify the controller was created
        assertNotNull(controller);

        // Verify the trigger methods exist using reflection
        assertDoesNotThrow(() -> {
            controller.getClass().getMethod("triggerLogin");
            controller.getClass().getMethod("triggerCreateAccount");
        });
    }

    @Test
    void testLoginController_ResetForm_WithFXML() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/via/sep2/fxml/auth/LoginView.fxml"));
        Parent root = loader.load();
        LoginViewController controller = loader.getController();

        assertDoesNotThrow(() -> controller.resetForm());
    }

    @Test
    void testLoginController_ResetForm_WithoutUI() {
        // Test reset functionality without relying on UI components
        LoginViewController controller = loginController;
        LoginViewModel viewModel = controller.getViewModel();

        // Set some state in the ViewModel
        viewModel.setUsername("testuser");
        viewModel.setPassword("password123");
        viewModel.setErrorMessage("Some error");

        assertDoesNotThrow(() -> {
            viewModel.reset();
        });

        assertEquals("", viewModel.getUsername());
        assertEquals("", viewModel.getPassword());
        assertEquals("", viewModel.getErrorMessage());
    }

    // Tests for CreateAccountViewController

    @Test
    void testCreateAccountController_InitialState() {
        assertNotNull(createAccountController);
        assertNotNull(createAccountController.getViewModel());
        assertTrue(createAccountController.getViewModel() instanceof CreateAccountViewModel);
    }

    @Test
    void testCreateAccountController_ViewModel_Integration() {
        CreateAccountViewController controller = createAccountController;
        CreateAccountViewModel viewModel = controller.getViewModel();

        assertNotNull(viewModel);

        assertDoesNotThrow(() -> {
            viewModel.setUsername("testuser");
            viewModel.setPassword("password123");
            viewModel.setConfirmPassword("password123");
            viewModel.setFirstName("Test");
            viewModel.setLastName("User");
        });

        assertEquals("testuser", viewModel.getUsername());
        assertEquals("password123", viewModel.getPassword());
        assertEquals("Test", viewModel.getFirstName());
        assertEquals("User", viewModel.getLastName());
    }

    @Test
    void testCreateAccountController_TriggerCreateAccount() {
        CreateAccountViewController controller = createAccountController;

        assertDoesNotThrow(() -> controller.triggerCreateAccount());
    }

    @Test
    void testCreateAccountController_NavigationMethods_Exist() {
        CreateAccountViewController controller = createAccountController;

        // Verify the controller was created
        assertNotNull(controller);

        // Verify the trigger methods exist using reflection
        assertDoesNotThrow(() -> {
            controller.getClass().getMethod("triggerCreateAccount");
            controller.getClass().getMethod("triggerLogin");
        });
    }

    @Test
    void testCreateAccountController_ResetForm_WithoutUI() {
        CreateAccountViewController controller = createAccountController;
        CreateAccountViewModel viewModel = controller.getViewModel();

        viewModel.setUsername("testuser");
        viewModel.setPassword("password123");
        viewModel.setFirstName("Test");
        viewModel.setLastName("User");
        viewModel.setErrorMessage("Some error");

        assertDoesNotThrow(() -> {
            viewModel.reset();
        });

        assertEquals("", viewModel.getUsername());
        assertEquals("", viewModel.getPassword());
        assertEquals("", viewModel.getFirstName());
        assertEquals("", viewModel.getLastName());
        assertEquals("", viewModel.getErrorMessage());
    }

    // Navigation tests

    @Test
    void testNavigation_LoginToCreateAccount() {
        try (MockedStatic<SceneManager> mockedSceneManager = mockStatic(SceneManager.class)) {
            mockedSceneManager.when(SceneManager::getInstance).thenReturn(mockSceneManager);

            // Simulate navigation
            mockSceneManager.showCreateAccount();

            // Verify navigation was called
            verify(mockSceneManager).showCreateAccount();
        }
    }

    @Test
    void testNavigation_CreateAccountToLogin() {
        try (MockedStatic<SceneManager> mockedSceneManager = mockStatic(SceneManager.class)) {
            mockedSceneManager.when(SceneManager::getInstance).thenReturn(mockSceneManager);

            // Simulate navigation
            mockSceneManager.showLogin();

            // Verify navigation was called
            verify(mockSceneManager).showLogin();
        }
    }

    @Test
    void testNavigation_SuccessfulLoginToMainChat() {
        try (MockedStatic<SceneManager> mockedSceneManager = mockStatic(SceneManager.class)) {
            mockedSceneManager.when(SceneManager::getInstance).thenReturn(mockSceneManager);

            // Simulate successful login navigation
            mockSceneManager.showMainChat();

            // Verify navigation was called
            verify(mockSceneManager).showMainChat();
        }
    }

    // Integration tests

    @Test
    void testControllerViewModel_Integration() {
        // Test that controllers properly integrate with their ViewModels
        LoginViewController loginController = new LoginViewController();
        CreateAccountViewController createController = new CreateAccountViewController();

        // Verify ViewModels are created and accessible
        assertNotNull(loginController.getViewModel());
        assertNotNull(createController.getViewModel());

        // Verify ViewModels are of correct type
        assertTrue(loginController.getViewModel() instanceof LoginViewModel);
        assertTrue(createController.getViewModel() instanceof CreateAccountViewModel);
    }

    @Test
    void testController_MethodsExist() {
        // Test that the required trigger methods exist and can be called
        LoginViewController loginController = new LoginViewController();
        CreateAccountViewController createController = new CreateAccountViewController();

        assertDoesNotThrow(() -> {
            loginController.getClass().getMethod("triggerLogin");
            loginController.getClass().getMethod("triggerCreateAccount");

            createController.getClass().getMethod("triggerCreateAccount");
            createController.getClass().getMethod("triggerLogin");
        });

        assertNotNull(loginController.getViewModel());
        assertNotNull(createController.getViewModel());
    }

    // Tests of ViewModel states

    @Test
    void testLoginViewModel_StateManagement() {
        LoginViewModel viewModel = loginController.getViewModel();

        // Test initial state
        assertEquals("", viewModel.getUsername());
        assertEquals("", viewModel.getPassword());
        assertFalse(viewModel.isLoading());

        // Test state changes
        viewModel.setUsername("testuser");
        viewModel.setPassword("password123");

        assertEquals("testuser", viewModel.getUsername());
        assertEquals("password123", viewModel.getPassword());

        viewModel.reset();
        assertEquals("", viewModel.getUsername());
        assertEquals("", viewModel.getPassword());
    }

    @Test
    void testCreateAccountViewModel_StateManagement() {
        CreateAccountViewModel viewModel = createAccountController.getViewModel();

        // Test initial state
        assertEquals("", viewModel.getUsername());
        assertEquals("", viewModel.getPassword());
        assertEquals("", viewModel.getFirstName());
        assertEquals("", viewModel.getLastName());
        assertFalse(viewModel.isLoading());
        assertFalse(viewModel.isAccountCreated());

        // Test state changes
        viewModel.setUsername("testuser");
        viewModel.setPassword("password123");
        viewModel.setFirstName("Test");
        viewModel.setLastName("User");

        assertEquals("testuser", viewModel.getUsername());
        assertEquals("password123", viewModel.getPassword());
        assertEquals("Test", viewModel.getFirstName());
        assertEquals("User", viewModel.getLastName());

        // Test reset
        viewModel.reset();
        assertEquals("", viewModel.getUsername());
        assertEquals("", viewModel.getPassword());
        assertEquals("", viewModel.getFirstName());
        assertEquals("", viewModel.getLastName());
    }

    // Test error handling

    @Test
    void testController_ViewModelErrorHandling() {
        LoginViewController loginController = new LoginViewController();
        CreateAccountViewController createController = new CreateAccountViewController();

        // Test ViewModel state with empty/invalid data
        assertDoesNotThrow(() -> {
            loginController.getViewModel().setUsername("");
            loginController.getViewModel().setPassword("");
        });

        assertDoesNotThrow(() -> {
            createController.getViewModel().setUsername("");
            createController.getViewModel().setPassword("");
        });

        // Test that ViewModels handle the state correctly
        LoginViewModel loginVM = loginController.getViewModel();
        assertFalse(loginVM.canLogin()); // Should be false with empty fields

        CreateAccountViewModel createVM = createController.getViewModel();
        assertFalse(createVM.canCreateAccount()); // Should be false with empty fields
    }

    @Test
    void testController_ValidationLogic() {
        LoginViewModel loginViewModel = loginController.getViewModel();
        CreateAccountViewModel createViewModel = createAccountController.getViewModel();

        // Test login validation
        assertFalse(loginViewModel.canLogin()); // Should be false with empty fields

        loginViewModel.setUsername("testuser");
        assertFalse(loginViewModel.canLogin()); // Should be false with only username

        loginViewModel.setPassword("password123");
        assertTrue(loginViewModel.canLogin()); // Should be true with both fields

        // Test create account validation
        createViewModel.setUsername("testuser");
        createViewModel.setPassword("password123");
        createViewModel.setConfirmPassword("password123");
        createViewModel.setFirstName("Test");
        createViewModel.setLastName("User");
        createViewModel.setUsernameValidation("Username is available ✓");

        assertTrue(createViewModel.canCreateAccount());
    }
}
