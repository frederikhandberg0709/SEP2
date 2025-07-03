package via.sep2;

import java.util.logging.Logger;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import via.sep2.client.connection.ConnectionManager;
import via.sep2.client.factory.ServiceFactory;
import via.sep2.client.util.SceneManager;

public class App extends Application {

    private static final Logger logger = Logger.getLogger(App.class.getName());

    @Override
    public void start(Stage primaryStage) {
        try {
            SceneManager sceneManager = SceneManager.getInstance();
            sceneManager.initialize(primaryStage);

            primaryStage.setOnCloseRequest(event -> {
                shutdown();
                Platform.exit();
            });

            sceneManager.showLogin();

            logger.info("Application started successfully");
        } catch (Exception e) {
            logger.severe("Failed to start application: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void shutdown() {
        logger.info("Shutting down Chat Application");

        try {
            ConnectionManager.getInstance().shutdown();
            ServiceFactory.getInstance().clearServices();

            logger.info("Application shutdown completed");
        } catch (Exception e) {
            logger.severe("Error during shutdown: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch();
    }
}
