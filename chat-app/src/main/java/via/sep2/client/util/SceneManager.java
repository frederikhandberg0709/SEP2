package via.sep2.client.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import via.sep2.client.connection.ConnectionManager;
import via.sep2.client.event.EventListener;
import via.sep2.client.event.events.LoginSuccessEvent;
import via.sep2.client.event.events.LogoutEvent;
import via.sep2.client.factory.ServiceFactory;

public class SceneManager {

    private static final Logger logger = Logger.getLogger(
            SceneManager.class.getName());

    private static SceneManager instance;
    private Stage primaryStage;
    private Map<String, Scene> sceneCache;
    private Map<String, Object> controllerCache;

    private final EventListener<LoginSuccessEvent> loginSuccessListener;
    private final EventListener<LogoutEvent> logoutListener;

    public static final String LOGIN_SCENE = "login";
    public static final String CREATE_ACCOUNT_SCENE = "create_account";
    public static final String MAIN_CHAT_SCENE = "main_chat";
    public static final String CREATE_GROUP_DIALOG = "create_group_dialog";
    public static final String MANAGE_GROUP_DIALOG = "manage_group_dialog";
    public static final String CONFIRMATION_DIALOG = "confirmation_dialog";

    private SceneManager() {
        this.sceneCache = new HashMap<>();
        this.controllerCache = new HashMap<>();

        this.loginSuccessListener = this::handleLoginSuccess;
        this.logoutListener = this::handleLogout;

        logger.info("SceneManager initialized");
    }

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;

        ConnectionManager.getInstance()
                .getEventBus()
                .subscribe(LoginSuccessEvent.class, loginSuccessListener);

        ConnectionManager.getInstance()
                .getEventBus()
                .subscribe(LogoutEvent.class, logoutListener);

        primaryStage.setOnCloseRequest(event -> {
            cleanup();
        });

        logger.info("SceneManager initialized with primary stage");
    }

    private void handleLoginSuccess(LoginSuccessEvent event) {
        Platform.runLater(() -> {
            logger.info(
                    "Handling login success event for user: " +
                            (event.getUser() != null
                                    ? event.getUser().getUsername()
                                    : "unknown"));

            try {
                showMainChat();
                logger.info("Successfully navigated to main chat after login");
            } catch (Exception e) {
                logger.severe(
                        "Failed to navigate to main chat after login: " +
                                e.getMessage());
                showLogin();
            }
        });
    }

    private void handleLogout(LogoutEvent event) {
        Platform.runLater(() -> {
            logger.info(
                    "Handling logout event for user: " +
                            (event.getUser() != null
                                    ? event.getUser().getUsername()
                                    : "unknown"));

            clearUserSpecificScenes();

            showLogin();
        });
    }

    private void clearUserSpecificScenes() {
        removeFromCache(MAIN_CHAT_SCENE);

        logger.info("Cleared user-specific scenes from cache");
    }

    public void showLogin() {
        showScene(
                LOGIN_SCENE,
                "/via/sep2/fxml/auth/LoginView.fxml",
                "Login - Chat App",
                "/via/sep2/css/auth.css");

        primaryStage.setMinWidth(400);
        primaryStage.setMinHeight(500);
        primaryStage.setMaxWidth(600);
        primaryStage.setMaxHeight(700);

        primaryStage.setWidth(450);
        primaryStage.setHeight(550);
    }

    public void showCreateAccount() {
        showScene(
                CREATE_ACCOUNT_SCENE,
                "/via/sep2/fxml/auth/CreateAccountView.fxml",
                "Create Account - Chat App",
                "/via/sep2/css/auth.css");

        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(650);
        primaryStage.setMaxWidth(750);
        primaryStage.setMaxHeight(750);

        primaryStage.setWidth(500);
        primaryStage.setHeight(750);
    }

    public void showMainChat() {
        if (isUserAuthenticated()) {
            showScene(
                    MAIN_CHAT_SCENE,
                    "/via/sep2/fxml/chat/MainChatView.fxml",
                    "Chat - Logged in",
                    "/via/sep2/css/chat.css",
                    "/via/sep2/css/user-search.css");

            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            primaryStage.setMaxWidth(Double.MAX_VALUE);
            primaryStage.setMaxHeight(Double.MAX_VALUE);

            if (primaryStage.getWidth() < 800 || primaryStage.getHeight() < 600) {
                primaryStage.setWidth(1200);
                primaryStage.setHeight(800);
            }
        } else {
            logger.warning(
                    "Attempted to show main chat without authentication");
            showLogin();
        }
    }

    public Stage showCreateGroupDialog() {
        return showDialog(
                CREATE_GROUP_DIALOG,
                "/via/sep2/fxml/chat/CreateGroupDialog.fxml",
                "Create Group Chat",
                false,
                500,
                600,
                "/via/sep2/css/chat.css");
    }

    public Stage showManageGroupDialog(String groupName) {
        return showDialog(
                MANAGE_GROUP_DIALOG,
                "/via/sep2/fxml/chat/ManageGroupDialog.fxml",
                "Manage Group - " + groupName,
                true,
                500,
                400,
                "/via/sep2/css/chat.css");
    }

    public Stage showConfirmationDialog(String title) {
        return showDialog(
                CONFIRMATION_DIALOG,
                "/via/sep2/fxml/chat/ConfirmationDialog.fxml",
                title,
                false,
                400,
                200,
                "/via/sep2/css/chat.css");
    }

    private Stage showDialog(
            String sceneId,
            String fxmlPath,
            String title,
            boolean resizable,
            double width,
            double height,
            String... stylesheetPaths) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle(title);
            dialogStage.initModality(javafx.stage.Modality.WINDOW_MODAL);
            dialogStage.initOwner(primaryStage);
            dialogStage.setResizable(resizable);

            Scene scene = new Scene(root);
            loadStylesheet(scene, stylesheetPaths);

            dialogStage.setScene(scene);
            dialogStage.setWidth(width);
            dialogStage.setHeight(height);

            // Store the controller for retrieval
            controllerCache.put(sceneId, loader.getController());

            return dialogStage;

        } catch (IOException e) {
            logger.severe("Error loading dialog: " + sceneId + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private boolean isUserAuthenticated() {
        try {
            return ServiceFactory.getInstance()
                    .getService(via.sep2.client.service.AuthService.class)
                    .isAuthenticated();
        } catch (Exception e) {
            logger.warning(
                    "Could not check authentication status: " + e.getMessage());
            return false;
        }
    }

    public void cleanup() {
        logger.info("Cleaning up SceneManager");

        ConnectionManager.getInstance()
                .getEventBus()
                .unsubscribe(LoginSuccessEvent.class, loginSuccessListener);

        ConnectionManager.getInstance()
                .getEventBus()
                .unsubscribe(LogoutEvent.class, logoutListener);

        clearCache();
    }

    private Scene getScene(
            String sceneId,
            String fxmlPath,
            String... stylesheetPaths) throws IOException {
        Scene scene = sceneCache.get(sceneId);
        if (scene == null) {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath));
            Parent root = loader.load();
            scene = new Scene(root);

            sceneCache.put(sceneId, scene);
            controllerCache.put(sceneId, loader.getController());

            loadStylesheet(scene, stylesheetPaths);
        }
        return scene;
    }

    @SuppressWarnings("unchecked")
    public <T> T getController(String sceneId) {
        return (T) controllerCache.get(sceneId);
    }

    public void clearCache() {
        sceneCache.clear();
        controllerCache.clear();
        logger.info("Scene cache cleared");
    }

    public void removeFromCache(String sceneId) {
        sceneCache.remove(sceneId);
        controllerCache.remove(sceneId);
        logger.info("Removed from cache: " + sceneId);
    }

    private void loadStylesheet(Scene scene, String... stylesheetPaths) {
        for (String cssPath : stylesheetPaths) {
            try {
                String fullPath = getClass()
                        .getResource(cssPath)
                        .toExternalForm();
                if (!scene.getStylesheets().contains(fullPath)) {
                    scene.getStylesheets().add(fullPath);
                }
            } catch (Exception e) {
                System.out.println(
                        "Could not load CSS file: " +
                                cssPath +
                                " - " +
                                e.getMessage());
            }
        }
    }

    public void showScene(String sceneId, String fxmlPath, String title) {
        try {
            Scene scene = getScene(sceneId, fxmlPath);
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error loading scene: " + sceneId);
            e.printStackTrace();
        }
    }

    public void showScene(
            String sceneId,
            String fxmlPath,
            String title,
            String... stylesheetPaths) {
        try {
            Scene scene = getScene(sceneId, fxmlPath, stylesheetPaths);
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error loading scene: " + sceneId);
            e.printStackTrace();
        }
    }

    public void showScene(
            String sceneId,
            String fxmlPath,
            String title,
            boolean resizable,
            double width,
            double height,
            String... stylesheetPaths) {
        try {
            Scene scene = getScene(sceneId, fxmlPath, stylesheetPaths);
            primaryStage.setScene(scene);
            primaryStage.setTitle(title);
            primaryStage.setResizable(resizable);
            primaryStage.setWidth(width);
            primaryStage.setHeight(height);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error loading scene: " + sceneId);
            e.printStackTrace();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    public void closeApplication() {
        if (primaryStage != null) {
            cleanup();
            primaryStage.close();
        }
    }
}
