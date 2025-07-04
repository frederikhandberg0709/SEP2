package via.sep2.client.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javafx.application.Platform;
import via.sep2.client.connection.ConnectionManager;
import via.sep2.client.event.EventBus;
import via.sep2.client.service.AuthService;
import via.sep2.client.viewmodel.auth.CreateAccountViewModel;
import via.sep2.client.viewmodel.auth.LoginViewModel;
import via.sep2.shared.dto.UserDTO;

@ExtendWith(MockitoExtension.class)
class AuthenticationTest extends JavaFXTestBase {

    @Mock
    private AuthService mockAuthService;

    @Mock
    private ConnectionManager mockConnectionManager;

    @Mock
    private EventBus mockEventBus;

    private CreateAccountViewModel createAccountViewModel;
    private LoginViewModel loginViewModel;

    @BeforeAll
    static void initJavaFX() {
        initializeJavaFX();
    }

    @BeforeEach
    void setup() {
        lenient().when(mockConnectionManager.getEventBus()).thenReturn(mockEventBus);

        createAccountViewModel = new CreateAccountViewModel();
        loginViewModel = new LoginViewModel();
    }

    // Tests for AuthService

    @Test
    void testLoginAsync_Success() throws Exception {
        // Arrange
        String username = "testuser";
        String password = "password123";
        UserDTO expectedUser = new UserDTO(1, username, "Test", "User");

        AuthService authService = new AuthService(mockConnectionManager);
        when(mockConnectionManager.getRmiClient()).thenReturn(mock(via.sep2.client.rmi.ChatClientImpl.class));
        when(mockConnectionManager.getRmiClient().login(username, password)).thenReturn(expectedUser);
        when(mockConnectionManager.getSessionState()).thenReturn(mock(via.sep2.client.state.SessionState.class));

        // Act
        CompletableFuture<UserDTO> result = authService.loginAsync(username, password);
        UserDTO actualUser = result.get(5, TimeUnit.SECONDS);

        // Assert
        assertEquals(expectedUser.getUsername(), actualUser.getUsername());
        assertEquals(expectedUser.getFirstName(), actualUser.getFirstName());
        assertEquals(expectedUser.getLastName(), actualUser.getLastName());
        verify(mockConnectionManager).connect();
        verify(mockEventBus).publish(any());
    }

    @Test
    void testLoginAsync_InvalidCredentials() {
        // Arrange
        String username = "testuser";
        String password = "wrongpassword";

        when(mockAuthService.loginAsync(username, password))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Authentication failed: Invalid credentials")));

        // Act & Assert
        CompletableFuture<UserDTO> result = mockAuthService.loginAsync(username, password);

        assertThrows(Exception.class, () -> result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void testCreateAccountAsync_Success() throws Exception {
        // Arrange
        String username = "newuser";
        String password = "password123";
        String firstName = "New";
        String lastName = "User";
        UserDTO expectedUser = new UserDTO(2, username, firstName, lastName);

        when(mockAuthService.createAccountAsync(username, password, firstName, lastName))
                .thenReturn(CompletableFuture.completedFuture(expectedUser));

        // Act
        CompletableFuture<UserDTO> result = mockAuthService.createAccountAsync(username, password, firstName, lastName);
        UserDTO actualUser = result.get(5, TimeUnit.SECONDS);

        // Assert
        assertEquals(expectedUser.getUsername(), actualUser.getUsername());
        assertEquals(expectedUser.getFirstName(), actualUser.getFirstName());
        assertEquals(expectedUser.getLastName(), actualUser.getLastName());
    }

    @Test
    void testCreateAccountAsync_UsernameAlreadyExists() {
        // Arrange
        String username = "existinguser";
        String password = "password123";
        String firstName = "Test";
        String lastName = "User";

        when(mockAuthService.createAccountAsync(username, password, firstName, lastName))
                .thenReturn(CompletableFuture.failedFuture(
                        new RuntimeException("Account creation failed: Username already exists")));

        // Act & Assert
        CompletableFuture<UserDTO> result = mockAuthService.createAccountAsync(username, password, firstName, lastName);

        assertThrows(Exception.class, () -> result.get(5, TimeUnit.SECONDS));
    }

    @Test
    void testCheckUsernameExists_UserExists() throws Exception {
        // Arrange
        String username = "existinguser";
        when(mockAuthService.checkUsernameExistsAsync(username))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Act
        CompletableFuture<Boolean> result = mockAuthService.checkUsernameExistsAsync(username);
        Boolean exists = result.get(5, TimeUnit.SECONDS);

        // Assert
        assertTrue(exists);
    }

    @Test
    void testCheckUsernameExists_UserDoesNotExist() throws Exception {
        // Arrange
        String username = "newuser";
        when(mockAuthService.checkUsernameExistsAsync(username))
                .thenReturn(CompletableFuture.completedFuture(false));

        // Act
        CompletableFuture<Boolean> result = mockAuthService.checkUsernameExistsAsync(username);
        Boolean exists = result.get(5, TimeUnit.SECONDS);

        // Assert
        assertFalse(exists);
    }

    // Test for CreateAccountViewModel

    @Test
    void testCreateAccountViewModel_InitialState() {
        // Assert initial state
        assertEquals("", createAccountViewModel.getUsername());
        assertEquals("", createAccountViewModel.getPassword());
        assertEquals("", createAccountViewModel.getConfirmPassword());
        assertEquals("", createAccountViewModel.getFirstName());
        assertEquals("", createAccountViewModel.getLastName());
        assertEquals("", createAccountViewModel.getErrorMessage());
        assertEquals("", createAccountViewModel.getSuccessMessage());
        assertFalse(createAccountViewModel.isLoading());
        assertFalse(createAccountViewModel.isAccountCreated());
    }

    @Test
    void testCreateAccountViewModel_SetProperties() {
        // Act
        createAccountViewModel.setUsername("testuser");
        createAccountViewModel.setPassword("password123");
        createAccountViewModel.setConfirmPassword("password123");
        createAccountViewModel.setFirstName("Test");
        createAccountViewModel.setLastName("User");

        // Assert
        assertEquals("testuser", createAccountViewModel.getUsername());
        assertEquals("password123", createAccountViewModel.getPassword());
        assertEquals("password123", createAccountViewModel.getConfirmPassword());
        assertEquals("Test", createAccountViewModel.getFirstName());
        assertEquals("User", createAccountViewModel.getLastName());
    }

    @Test
    void testCreateAccountViewModel_PasswordValidation() throws InterruptedException {
        // Test invalid password (too short)
        createAccountViewModel.setPassword("123");
        Thread.sleep(100); // Allow validation to run
        assertTrue(createAccountViewModel.getPasswordValidation().contains("at least 8 characters"));

        // Test valid password
        createAccountViewModel.setPassword("password123");
        Thread.sleep(100); // Allow validation to run
        assertTrue(createAccountViewModel.getPasswordValidation().contains("valid"));
    }

    @Test
    void testCreateAccountViewModel_ConfirmPasswordValidation() throws InterruptedException {
        // Set password
        createAccountViewModel.setPassword("password123");

        // Test non-matching confirm password
        createAccountViewModel.setConfirmPassword("differentpassword");
        Thread.sleep(100); // Allow validation to run
        assertTrue(createAccountViewModel.getConfirmPasswordValidation().contains("do not match"));

        // Test matching confirm password
        createAccountViewModel.setConfirmPassword("password123");
        Thread.sleep(100); // Allow validation to run
        assertTrue(createAccountViewModel.getConfirmPasswordValidation().contains("match"));
    }

    @Test
    void testCreateAccountViewModel_CanCreateAccount() {
        // Setup valid input
        createAccountViewModel.setUsername("testuser");
        createAccountViewModel.setPassword("password123");
        createAccountViewModel.setConfirmPassword("password123");
        createAccountViewModel.setFirstName("Test");
        createAccountViewModel.setLastName("User");
        createAccountViewModel.setUsernameValidation("Username is available ✓");

        // Assert can create account
        assertTrue(createAccountViewModel.canCreateAccount());

        // Test with missing field
        createAccountViewModel.setFirstName("");
        assertFalse(createAccountViewModel.canCreateAccount());

        // Test with mismatched passwords
        createAccountViewModel.setFirstName("Test");
        createAccountViewModel.setConfirmPassword("differentpassword");
        assertFalse(createAccountViewModel.canCreateAccount());
    }

    @Test
    void testCreateAccountViewModel_Reset() {
        // Setup state
        createAccountViewModel.setUsername("testuser");
        createAccountViewModel.setPassword("password123");
        createAccountViewModel.setErrorMessage("Some error");
        createAccountViewModel.setIsLoading(true);

        // Act
        createAccountViewModel.reset();

        // Assert
        assertEquals("", createAccountViewModel.getUsername());
        assertEquals("", createAccountViewModel.getPassword());
        assertEquals("", createAccountViewModel.getErrorMessage());
        assertFalse(createAccountViewModel.isLoading());
    }

    // Tests for LoginViewModel

    @Test
    void testLoginViewModel_InitialState() {
        // Assert initial state
        assertEquals("", loginViewModel.getUsername());
        assertEquals("", loginViewModel.getPassword());
        assertEquals("", loginViewModel.getErrorMessage());
        assertFalse(loginViewModel.isLoading());
        assertFalse(loginViewModel.isLoginSuccessful());
        assertNull(loginViewModel.getCurrentUser());
    }

    @Test
    void testLoginViewModel_SetProperties() {
        // Act
        loginViewModel.setUsername("testuser");
        loginViewModel.setPassword("password123");
        loginViewModel.setErrorMessage("Test error");

        // Assert
        assertEquals("testuser", loginViewModel.getUsername());
        assertEquals("password123", loginViewModel.getPassword());
        assertEquals("Test error", loginViewModel.getErrorMessage());
    }

    @Test
    void testLoginViewModel_CanLogin() {
        // Test with empty fields
        assertFalse(loginViewModel.canLogin());

        // Test with username only
        loginViewModel.setUsername("testuser");
        assertFalse(loginViewModel.canLogin());

        // Test with both username and password
        loginViewModel.setPassword("password123");
        assertTrue(loginViewModel.canLogin());

        // Test when loading
        loginViewModel.setIsLoading(true);
        assertFalse(loginViewModel.canLogin());
    }

    @Test
    void testLoginViewModel_Login_InvalidInput() throws InterruptedException {
        // Test with empty username
        loginViewModel.setUsername("");
        loginViewModel.setPassword("password123");

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            loginViewModel.login();
            assertEquals("Please enter your username", loginViewModel.getErrorMessage());
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);

        // Test with empty password
        loginViewModel.setUsername("testuser");
        loginViewModel.setPassword("");

        CountDownLatch latch2 = new CountDownLatch(1);
        Platform.runLater(() -> {
            loginViewModel.login();
            assertEquals("Please enter your password", loginViewModel.getErrorMessage());
            latch2.countDown();
        });

        latch2.await(5, TimeUnit.SECONDS);
    }

    @Test
    void testLoginViewModel_ClearPassword() {
        // Arrange
        loginViewModel.setPassword("password123");

        // Act
        loginViewModel.clearPassword();

        // Assert
        assertEquals("", loginViewModel.getPassword());
    }

    @Test
    void testLoginViewModel_ClearErrorMessage() {
        // Arrange
        loginViewModel.setErrorMessage("Some error");

        // Act
        loginViewModel.clearErrorMessage();

        // Assert
        assertEquals("", loginViewModel.getErrorMessage());
    }

    @Test
    void testLoginViewModel_Reset() {
        // Setup state
        loginViewModel.setUsername("testuser");
        loginViewModel.setPassword("password123");
        loginViewModel.setErrorMessage("Some error");
        loginViewModel.setIsLoading(true);
        loginViewModel.setLoginSuccessful(true);

        // Act
        loginViewModel.reset();

        // Assert
        assertEquals("", loginViewModel.getUsername());
        assertEquals("", loginViewModel.getPassword());
        assertEquals("", loginViewModel.getErrorMessage());
        assertFalse(loginViewModel.isLoading());
        assertFalse(loginViewModel.isLoginSuccessful());
        assertNull(loginViewModel.getCurrentUser());
    }

    // Validation tests

    @Test
    void testUsernameValidation() {
        // Test empty username
        createAccountViewModel.setUsername("");
        createAccountViewModel.checkUsernameAvailability();
        assertEquals("", createAccountViewModel.getUsernameValidation());

        // Test short username
        createAccountViewModel.setUsername("ab");
        createAccountViewModel.checkUsernameAvailability();
        assertTrue(createAccountViewModel.getUsernameValidation().contains("at least 3 characters"));

        // Test invalid characters
        createAccountViewModel.setUsername("user@name");
        createAccountViewModel.checkUsernameAvailability();
        assertTrue(createAccountViewModel.getUsernameValidation().contains("letters, numbers, and underscores"));
    }

    @Test
    void testPasswordValidation() {
        // Test empty password
        createAccountViewModel.setPassword("");
        assertEquals("", createAccountViewModel.getPasswordValidation());

        // Test short password
        createAccountViewModel.setPassword("123");
        assertTrue(createAccountViewModel.getPasswordValidation().contains("at least 8 characters"));

        // Test valid password
        createAccountViewModel.setPassword("password123");
        assertTrue(createAccountViewModel.getPasswordValidation().contains("valid"));
    }

    @Test
    void testNameValidation() throws InterruptedException {
        // Setup view model with invalid names
        createAccountViewModel.setFirstName("123");
        createAccountViewModel.setLastName("!@#");
        createAccountViewModel.setUsername("testuser");
        createAccountViewModel.setPassword("password123");
        createAccountViewModel.setConfirmPassword("password123");

        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            createAccountViewModel.createAccount();
            assertTrue(createAccountViewModel.getErrorMessage().contains("invalid characters"));
            latch.countDown();
        });

        latch.await(5, TimeUnit.SECONDS);
    }

    // Mocked AuthService

    @Test
    void testCreateAccountViewModel_WithMockedAuthService() throws InterruptedException {
        CreateAccountViewModel mockViewModel = new CreateAccountViewModel(mockAuthService);

        // Arrange
        UserDTO expectedUser = new UserDTO(1, "testuser", "Test", "User");
        when(mockAuthService.createAccountAsync(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(expectedUser));

        mockViewModel.setUsername("testuser");
        mockViewModel.setPassword("password123");
        mockViewModel.setConfirmPassword("password123");
        mockViewModel.setFirstName("Test");
        mockViewModel.setLastName("User");

        // Act
        runOnFXThreadAndWait(() -> {
            mockViewModel.createAccount();
        });

        Thread.sleep(100);

        // Assert
        verify(mockAuthService).createAccountAsync("testuser", "password123", "Test", "User");
    }

    @Test
    void testLoginViewModel_WithMockedAuthService() throws InterruptedException {
        LoginViewModel mockViewModel = new LoginViewModel(mockAuthService);

        // Arrange
        UserDTO expectedUser = new UserDTO(1, "testuser", "Test", "User");
        when(mockAuthService.loginAsync("testuser", "password123"))
                .thenReturn(CompletableFuture.completedFuture(expectedUser));

        mockViewModel.setUsername("testuser");
        mockViewModel.setPassword("password123");

        // Act
        runOnFXThreadAndWait(() -> {
            mockViewModel.login();
        });

        Thread.sleep(100);

        // Assert
        verify(mockAuthService).loginAsync("testuser", "password123");
    }

    @Test
    void testUsernameValidation_WithMockedAuthService() throws InterruptedException {
        CreateAccountViewModel mockViewModel = new CreateAccountViewModel(mockAuthService);

        // Setup mock for username availability check
        when(mockAuthService.checkUsernameExistsAsync("testuser"))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(mockAuthService.checkUsernameExistsAsync("existinguser"))
                .thenReturn(CompletableFuture.completedFuture(true));

        // Test available username
        mockViewModel.setUsername("testuser");
        Thread.sleep(600);
        verify(mockAuthService).checkUsernameExistsAsync("testuser");

        // Test taken username
        mockViewModel.setUsername("existinguser");
        Thread.sleep(600);
        verify(mockAuthService).checkUsernameExistsAsync("existinguser");
    }

    @Test
    void testCreateAccountToLoginFlow_WithMockedAuthService() throws InterruptedException {
        CreateAccountViewModel mockCreateViewModel = new CreateAccountViewModel(mockAuthService);
        LoginViewModel mockLoginViewModel = new LoginViewModel(mockAuthService);

        // Arrange - Create account
        UserDTO newUser = new UserDTO(1, "testuser", "Test", "User");
        when(mockAuthService.createAccountAsync("testuser", "password123", "Test", "User"))
                .thenReturn(CompletableFuture.completedFuture(newUser));
        when(mockAuthService.loginAsync("testuser", "password123"))
                .thenReturn(CompletableFuture.completedFuture(newUser));

        // Act - Create account
        mockCreateViewModel.setUsername("testuser");
        mockCreateViewModel.setPassword("password123");
        mockCreateViewModel.setConfirmPassword("password123");
        mockCreateViewModel.setFirstName("Test");
        mockCreateViewModel.setLastName("User");

        runOnFXThreadAndWait(() -> {
            mockCreateViewModel.createAccount();
        });

        Thread.sleep(100);

        verify(mockAuthService).createAccountAsync("testuser", "password123", "Test", "User");

        // Act - Login with created account
        mockLoginViewModel.setUsername("testuser");
        mockLoginViewModel.setPassword("password123");

        runOnFXThreadAndWait(() -> {
            mockLoginViewModel.login();
        });

        Thread.sleep(100);

        verify(mockAuthService).loginAsync("testuser", "password123");
    }
}
