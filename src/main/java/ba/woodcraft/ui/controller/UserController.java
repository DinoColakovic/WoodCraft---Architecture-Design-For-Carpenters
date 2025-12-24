package ba.woodcraft.ui.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class UserController {

    @FXML private Label welcomeLabel;

    public void setWelcome(String username) {
        welcomeLabel.setText("Dobrodošli, " + username + " (USER)");
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
