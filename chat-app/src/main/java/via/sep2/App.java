package via.sep2;

import javafx.application.Application;
import javafx.stage.Stage;
import via.sep2.util.SceneManager;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        SceneManager sceneManager = SceneManager.getInstance();
        sceneManager.initialize(primaryStage);

        sceneManager.showLogin();
    }

    public static void main(String[] args) {
        launch();
    }
}
