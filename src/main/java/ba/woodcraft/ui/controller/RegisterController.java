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

public class RegisterController {

    @FXML private VBox root;           // <VBox fx:id="root" ...>
    @FXML private ImageView logoImage; // <ImageView fx:id="logoImage" ...>

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmField;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void initialize() {
        if (root != null && logoImage != null) {
            logoImage.fitHeightProperty().bind(root.heightProperty().multiply(0.40));
        }
    }

    @FXML
    public void onRegister(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm = confirmField.getText();

        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            info("Greška", "Popuni sva polja.");
            return;
        }
        if (username.length() < 3) {
            info("Greška", "Username mora imati najmanje 3 karaktera.");
            return;
        }
        if (!password.equals(confirm)) {
            info("Greška", "Passwordi se ne podudaraju.");
            return;
        }
        if (userDAO.findByUsername(username) != null) {
            info("Greška", "Username već postoji.");
            return;
        }

        String hash = PasswordUtil.hashLozinke(password);
        boolean ok = userDAO.insertUser(username, hash, "USER"); // default USER

        if (ok) {
            info("Uspjeh", "Račun je kreiran. Možete se prijaviti.");
            SceneNavigator.show("view/login.fxml");
        } else {
            info("Greška", "Registracija nije uspjela.");
        }
    }

    @FXML
    public void onBackToLogin(ActionEvent event) {
        SceneNavigator.show("view/login.fxml");
    }

    private void info(String naslov, String poruka) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(naslov);
        a.setHeaderText(null);
        a.setContentText(poruka);
        a.showAndWait();
    }
}
