package com.example.managementsystem;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.sql.*;
import java.util.Objects;

public class RegisterController {
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;

    @FXML
    public void initialize() {

        roleComboBox.getItems().addAll("Admin", "Employee");
    }

    @FXML
    private void handleRegister() {
        try {
            if (!validateInputs()) return;

            if (usernameExists(usernameField.getText())) {
                showAlert("Error", "Username already exists!");
                return;
            }

            String salt = PasswordUtils.generateSalt();
            String hashedPassword = PasswordUtils.hashPassword(passwordField.getText(), salt);

            try (Connection conn = DatabaseConnection.getConnection()) {
                String sql = "INSERT INTO users (full_name, email, username, password, role) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement stmt = conn.prepareStatement(sql);
                stmt.setString(1, fullNameField.getText());
                stmt.setString(2, emailField.getText());
                stmt.setString(3, usernameField.getText());
                stmt.setString(4, salt + ":" + hashedPassword);
                stmt.setString(5, roleComboBox.getValue().toUpperCase());

                int rowsAffected = stmt.executeUpdate();
                if (rowsAffected > 0) {
                    showAlert("Success", "Registration successful!");
                    goBackToHome();
                }
            }

        } catch (SQLException e) {
            showAlert("Database Error", "Error saving user: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            showAlert("Error", "Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateInputs() {
        if (fullNameField.getText().isEmpty() || emailField.getText().isEmpty() ||
                usernameField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            showAlert("Error", "All fields are required!");
            return false;
        }

        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showAlert("Error", "Passwords do not match!");
            return false;
        }

        if (roleComboBox.getValue() == null) {
            showAlert("Error", "Please select a role!");
            return false;
        }

        return true;
    }

    private boolean usernameExists(String username) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT username FROM users WHERE username = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    @FXML
    private void goBackToHome() throws Exception {
        Parent root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("home.fxml")));
        Stage window = (Stage) fullNameField.getScene().getWindow();
        window.setScene(new Scene(root));
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
