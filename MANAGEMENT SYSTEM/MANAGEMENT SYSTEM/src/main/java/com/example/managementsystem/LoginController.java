package com.example.managementsystem;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;

public class LoginController {

    @FXML private TextField usernameField; // used for both email or username
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("Admin", "Employee");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String userInput = usernameField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        if (userInput.isEmpty() || password.isEmpty() || role == null) {
            showAlert("Error", "Please fill in all fields");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT username, password FROM users WHERE (username = ? OR email = ?) AND role = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, userInput);
            stmt.setString(2, userInput);
            stmt.setString(3, role);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String username = rs.getString("username");
                String storedPassword = rs.getString("password");

                if (storedPassword.contains(":")) {
                    String[] passwordParts = storedPassword.split(":");
                    if (passwordParts.length != 2) {
                        showAlert("Error", "Invalid password format in database");
                        return;
                    }
                    String salt = passwordParts[0];
                    String hashedPassword = passwordParts[1];

                    if (PasswordUtils.verifyPassword(password, salt, hashedPassword)) {
                        loginSuccessful(event, role, username);
                    } else {
                        showAlert("Error", "Invalid credentials!");
                    }
                } else {

                    if (storedPassword.equals(password)) {
                        loginSuccessful(event, role, username);
                    } else {
                        showAlert("Error", "Invalid credentials!");
                    }
                }
            } else {
                showAlert("Error", "User not found!");
            }
        } catch (SQLException | IOException e) {
            showAlert("Database Error", "Error during login: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loginSuccessful(ActionEvent event, String role, String username) throws IOException {
        FXMLLoader loader = new FXMLLoader();
        Parent root;

        if (role.equals("Admin")) {
            loader.setLocation(getClass().getResource("admin_dashboard.fxml"));
            root = loader.load();
            AdminDashboardController controller = loader.getController();
            controller.setCurrentUser(username);

        } else if (role.equals("Employee")) {
            loader.setLocation(getClass().getResource("employee_dashboard.fxml"));
            root = loader.load();
            EmployeeDashboardController controller = loader.getController();
            controller.setCurrentUser(username);

        } else {
            showAlert("Error", "Invalid role selected!");
            return;
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.setScene(new Scene(root));
        stage.setTitle(role + " Dashboard");
        stage.show();
    }

    @FXML
    private void goToRegister(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("register.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Register");
            stage.show();
        } catch (IOException e) {
            showAlert("Error", "Failed to load register page.");
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
