package via.sep2.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import via.sep2.client.connection.ConnectionManager;
import via.sep2.client.event.EventListener;
import via.sep2.client.event.events.LogoutEvent;
import via.sep2.client.factory.ServiceFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class SceneManager {

    private static final Logger logger = Logger.getLogger(SceneManager.class.getName());

    private static SceneManager instance;
    private Stage primaryStage;
    private Map<String, Scene> sceneCache;
    private Map<String, Object> controllerCache;

    private final EventListener<LogoutEvent> logoutListener;

    public static final String LOGIN_SCENE = "login";
    public static final String CREATE_ACCOUNT_SCENE = "create_account";
    public static final String MAIN_CHAT_SCENE = "main_chat";

    private SceneManager() {
        this.sceneCache = new HashMap<>();
        this.controllerCache = new HashMap<>();

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

        ConnectionManager.getInstance().getEventBus()
                .subscribe(LogoutEvent.class, logoutListener);

        primaryStage.setOnCloseRequest(event -> {
            cleanup();
        });

        logger.info("SceneManager initialized with primary stage");
    }

    private void handleLogout(LogoutEvent event) {
        Platform.runLater(() -> {
            logger.info("Handling logout event for user: " +
                    (event.getUser() != null ? event.getUser().getUsername() : "unknown"));

            clearUserSpecificScenes();

            showLogin();
        });
    }

    private void clearUserSpecificScenes() {
        removeFromCache(MAIN_CHAT_SCENE);

        logger.info("Cleared user-specific scenes from cache");
    }

    public void showLogin() {
        showScene(LOGIN_SCENE, "/via/sep2/fxml/auth/LoginView.fxml", "Login - Chat App",
                "/via/sep2/css/auth.css");
    }

    public void showCreateAccount() {
        showScene(CREATE_ACCOUNT_SCENE, "/via/sep2/fxml/auth/CreateAccountView.fxml", "Create Account - Chat App",
                "/via/sep2/css/auth.css");
    }

    public void showMainChat() {
        if (isUserAuthenticated()) {
            showScene(MAIN_CHAT_SCENE, "/via/sep2/fxml/chat/MainChatView.fxml", "Chat - Logged in",
                    "/via/sep2/css/chat.css");
        } else {
            logger.warning("Attempted to show main chat without authentication");
            showLogin();
        }
    }

    private boolean isUserAuthenticated() {
        try {
            return ServiceFactory.getInstance()
                    .getService(via.sep2.client.service.AuthService.class)
                    .isAuthenticated();
        } catch (Exception e) {
            logger.warning("Could not check authentication status: " + e.getMessage());
            return false;
        }
    }

    public void cleanup() {
        logger.info("Cleaning up SceneManager");

        ConnectionManager.getInstance().getEventBus()
                .unsubscribe(LogoutEvent.class, logoutListener);

        clearCache();
    }

    private Scene getScene(String sceneId, String fxmlPath, String... stylesheetPaths) throws IOException {
        Scene scene = sceneCache.get(sceneId);
        if (scene == null) {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
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
                String fullPath = getClass().getResource(cssPath).toExternalForm();
                if (!scene.getStylesheets().contains(fullPath)) {
                    scene.getStylesheets().add(fullPath);
                }
            } catch (Exception e) {
                System.out.println("Could not load CSS file: " + cssPath + " - " + e.getMessage());
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

    public void showScene(String sceneId, String fxmlPath, String title, String... stylesheetPaths) {
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

    public void showScene(String sceneId, String fxmlPath, String title, boolean resizable, double width,
            double height, String... stylesheetPaths) {
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
