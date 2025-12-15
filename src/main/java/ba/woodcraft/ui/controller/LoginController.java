package ba.woodcraft.ui.controller;

import ba.woodcraft.dao.UserDAO;
import ba.woodcraft.util.PasswordUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;

    private final UserDAO userDAO = new UserDAO();

    @FXML
    public void onLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            prikazi("Greška", "Unesi username i password.");
            return;
        }

        UserDAO.DbUser user = userDAO.findByUsername(username);
        System.out.println("DEBUG username=" + username + " -> " + user);

        if (user == null) {
            prikazi("Greška", "Korisnik ne postoji.");
            return;
        }

        if (!PasswordUtil.provjeriLozinku(password, user.passwordHash())) {
            prikazi("Greška", "Pogrešna lozinka.");
            return;
        }

        prikazi("Uspjeh", "Ulogovan kao: " + user.role());
    }

    private void prikazi(String naslov, String poruka) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(naslov);
        alert.setHeaderText(null);
        alert.setContentText(poruka);
        alert.showAndWait();
    }
}
