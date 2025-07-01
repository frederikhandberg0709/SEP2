package via.sep2.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SceneManager {
    private static SceneManager instance;
    private Stage primaryStage;
    private Map<String, Scene> sceneCache;
    private Map<String, Object> controllerCache;

    public static final String LOGIN_SCENE = "login";
    public static final String CREATE_ACCOUNT_SCENE = "create_account";

    private SceneManager() {
        this.sceneCache = new HashMap<>();
        this.controllerCache = new HashMap<>();
    }

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void initialize(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void showLogin() {
        showScene(LOGIN_SCENE, "/via/sep2/fxml/auth/LoginView.fxml", "Login - Chat App",
                "/via/sep2/css/auth.css");
    }

    public void showCreateAccount() {
        showScene(CREATE_ACCOUNT_SCENE, "/via/sep2/fxml/auth/CreateAccountView.fxml", "Create Account - Chat App",
                "/via/sep2/css/auth.css");
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
    }

    public void removeFromCache(String sceneId) {
        sceneCache.remove(sceneId);
        controllerCache.remove(sceneId);
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
            primaryStage.close();
        }
    }
}
