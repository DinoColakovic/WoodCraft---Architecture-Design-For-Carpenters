package ba.woodcraft.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class UserController {

    @FXML private VBox root;
    @FXML private ImageView logoImage;
    @FXML private Label welcomeLabel;

    @FXML
    public void initialize() {
        if (root != null && logoImage != null) {
            logoImage.fitHeightProperty().bind(root.heightProperty().multiply(0.40));
        }
    }

    public void setWelcome(String username) {
        welcomeLabel.setText("Dobrodošli, " + username);
    }

    @FXML
    public void onLogout(ActionEvent event) {
        SceneNavigator.show("view/login.fxml");
    }

    @FXML
    public void onOpenCanvas(ActionEvent event) {
        SceneNavigator.show("view/canvas.fxml");
    }
}
