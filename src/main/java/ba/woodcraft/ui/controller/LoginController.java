package ba.woodcraft.ui.controller;

import ba.woodcraft.dao.UserDAO;
import ba.woodcraft.util.PasswordUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

public class LoginController {

    @FXML private VBox root;          // <VBox fx:id="root" ...>
    @FXML private ImageView logoImage; // <ImageView fx:id="logoImage" ...>

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        // povećava smanjuje logo u odnosu na veličinu prozora
        if (root != null && logoImage != null) {
            logoImage.fitHeightProperty().bind(root.heightProperty().multiply(0.40));
            // po želji 0.25, 0.35...
        }
    }

    @FXML
    public void onLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            prikazi("Greška", "Unesi username i password.");
            return;
        }

        UserDAO.DbUser user = userDAO.findByUsername(username);

        if (user == null) {
            prikazi("Greška", "Korisnik ne postoji.");
            return;
        }

        if (!PasswordUtil.provjeriLozinku(password, user.passwordHash())) {
            prikazi("Greška", "Pogrešna lozinka.");
            return;
        }

        String role = user.role();

        if ("ADMIN".equalsIgnoreCase(role)) {
            AdminController c = SceneNavigator.showWithController("view/admin.fxml");
            c.setWelcome(user.username());
        } else {
            UserController c = SceneNavigator.showWithController("view/user.fxml");
            c.setWelcome(user.username());
        }
    }

    @FXML
    public void onOpenRegister(ActionEvent event) {
        SceneNavigator.show("view/register.fxml");
    }

    private void prikazi(String naslov, String poruka) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(naslov);
        alert.setHeaderText(null);
        alert.setContentText(poruka);
        alert.showAndWait();
    }
}
