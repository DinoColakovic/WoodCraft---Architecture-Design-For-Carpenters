package ba.woodcraft;

import ba.woodcraft.ui.controller.SceneNavigator;
import javafx.application.Application;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("WoodCraft");

        SceneNavigator.init(primaryStage);
        SceneNavigator.show("view/login.fxml");
    }

    public static void main(String[] args) {
        launch();

    }
}
