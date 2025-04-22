package com.example.managementsystem;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class AdminUsersController {
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> idColumn;
    @FXML private TableColumn<User, String> usernameColumn;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> createdColumn;
    @FXML private TextField searchField;

    private ObservableList<User> userList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Set up the columns in the table
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        usernameColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        createdColumn.setCellValueFactory(new PropertyValueFactory<>("createdAtFormatted"));

        // Load all users from database
        loadUsers();

        // Set the items in the table
        userTable.setItems(userList);
    }

    private void loadUsers() {
        userList.clear();

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {

            while (rs.next()) {
                User user = new User(
                        rs.getInt("user_id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at").toLocalDateTime()
                );
                userList.add(user);
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load users: " + e.getMessage(), Alert.AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddUser() {
        // Create a dialog for adding a new user
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Add New User");
        dialog.setHeaderText("Enter user details:");

        // Set the button types
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Create the form fields
        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Full Name");
        TextField emailField = new TextField();
        emailField.setPromptText("Email");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");
        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Admin", "Employee");
        roleComboBox.setValue("Employee");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(fullNameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Username:"), 0, 2);
        grid.add(usernameField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(roleComboBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a user when the OK button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return new User(
                        0, // ID will be auto-generated
                        fullNameField.getText(),
                        emailField.getText(),
                        usernameField.getText(),
                        passwordField.getText(),
                        roleComboBox.getValue(),
                        LocalDateTime.now()
                );
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();

        result.ifPresent(user -> {
            // Validate inputs
            if (user.getFullName().isEmpty() || user.getEmail().isEmpty() ||
                    user.getUsername().isEmpty() || user.getPassword().isEmpty()) {
                showAlert("Validation Error", "All fields are required", Alert.AlertType.ERROR);
                return;
            }

            // Insert the new user into the database
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "INSERT INTO users (full_name, email, username, password, role) VALUES (?, ?, ?, ?, ?)",
                         Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, user.getFullName());
                stmt.setString(2, user.getEmail());
                stmt.setString(3, user.getUsername());
                stmt.setString(4, user.getPassword()); // In real app, hash this password
                stmt.setString(5, user.getRole());

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            user.setId(generatedKeys.getInt(1));
                            userList.add(user);
                            showAlert("Success", "User added successfully", Alert.AlertType.INFORMATION);
                        }
                    }
                }
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    showAlert("Error", "Username or email already exists", Alert.AlertType.ERROR);
                } else {
                    showAlert("Database Error", "Failed to add user: " + e.getMessage(), Alert.AlertType.ERROR);
                }
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleEditUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert("No Selection", "Please select a user to edit", Alert.AlertType.WARNING);
            return;
        }

        // Create a dialog for editing the user
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Edit User");
        dialog.setHeaderText("Edit user details:");

        // Set the button types
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        // Create the form fields with current values
        TextField fullNameField = new TextField(selectedUser.getFullName());
        TextField emailField = new TextField(selectedUser.getEmail());
        TextField usernameField = new TextField(selectedUser.getUsername());
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Leave blank to keep current password");
        ComboBox<String> roleComboBox = new ComboBox<>();
        roleComboBox.getItems().addAll("Admin", "Employee");
        roleComboBox.setValue(selectedUser.getRole());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Full Name:"), 0, 0);
        grid.add(fullNameField, 1, 0);
        grid.add(new Label("Email:"), 0, 1);
        grid.add(emailField, 1, 1);
        grid.add(new Label("Username:"), 0, 2);
        grid.add(usernameField, 1, 2);
        grid.add(new Label("Password:"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(new Label("Role:"), 0, 4);
        grid.add(roleComboBox, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // Convert the result to a user when the OK button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                User editedUser = new User(
                        selectedUser.getId(),
                        fullNameField.getText(),
                        emailField.getText(),
                        usernameField.getText(),
                        passwordField.getText().isEmpty() ? selectedUser.getPassword() : passwordField.getText(),
                        roleComboBox.getValue(),
                        selectedUser.getCreatedAt()
                );
                return editedUser;
            }
            return null;
        });

        Optional<User> result = dialog.showAndWait();

        result.ifPresent(editedUser -> {
            // Validate inputs
            if (editedUser.getFullName().isEmpty() || editedUser.getEmail().isEmpty() ||
                    editedUser.getUsername().isEmpty()) {
                showAlert("Validation Error", "All fields except password are required", Alert.AlertType.ERROR);
                return;
            }

            // Update the user in the database
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "UPDATE users SET full_name = ?, email = ?, username = ?, " +
                                 (editedUser.getPassword().equals(selectedUser.getPassword()) ?
                                         "role = ? WHERE user_id = ?" :
                                         "password = ?, role = ? WHERE user_id = ?"))) {

                stmt.setString(1, editedUser.getFullName());
                stmt.setString(2, editedUser.getEmail());
                stmt.setString(3, editedUser.getUsername());

                if (editedUser.getPassword().equals(selectedUser.getPassword())) {
                    stmt.setString(4, editedUser.getRole());
                    stmt.setInt(5, editedUser.getId());
                } else {
                    stmt.setString(4, editedUser.getPassword()); // In real app, hash this password
                    stmt.setString(5, editedUser.getRole());
                    stmt.setInt(6, editedUser.getId());
                }

                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    // Update the user in the observable list
                    int index = userList.indexOf(selectedUser);
                    userList.set(index, editedUser);
                    showAlert("Success", "User updated successfully", Alert.AlertType.INFORMATION);
                }
            } catch (SQLException e) {
                if (e.getMessage().contains("Duplicate entry")) {
                    showAlert("Error", "Username or email already exists", Alert.AlertType.ERROR);
                } else {
                    showAlert("Database Error", "Failed to update user: " + e.getMessage(), Alert.AlertType.ERROR);
                }
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showAlert("No Selection", "Please select a user to delete", Alert.AlertType.WARNING);
            return;
        }

        // Confirm deletion
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete User");
        alert.setContentText("Are you sure you want to delete " + selectedUser.getUsername() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                         "DELETE FROM users WHERE user_id = ?")) {

                stmt.setInt(1, selectedUser.getId());
                int affectedRows = stmt.executeUpdate();

                if (affectedRows > 0) {
                    userList.remove(selectedUser);
                    showAlert("Success", "User deleted successfully", Alert.AlertType.INFORMATION);
                }
            } catch (SQLException e) {
                showAlert("Database Error", "Failed to delete user: " + e.getMessage(), Alert.AlertType.ERROR);
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleSearch() {
        String searchTerm = searchField.getText().trim().toLowerCase();

        if (searchTerm.isEmpty()) {
            userTable.setItems(userList);
            return;
        }

        ObservableList<User> filteredList = FXCollections.observableArrayList();
        for (User user : userList) {
            if (user.getUsername().toLowerCase().contains(searchTerm) ||
                    user.getFullName().toLowerCase().contains(searchTerm) ||
                    user.getEmail().toLowerCase().contains(searchTerm) ||
                    user.getRole().toLowerCase().contains(searchTerm)) {
                filteredList.add(user);
            }
        }

        userTable.setItems(filteredList);
    }

    private void showAlert(String title, String message, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // User model class
    public static class User {
        private int id;
        private String fullName;
        private String email;
        private String username;
        private String password;
        private String role;
        private LocalDateTime createdAt;

        public User(int id, String fullName, String email, String username,
                    String password, String role, LocalDateTime createdAt) {
            this.id = id;
            this.fullName = fullName;
            this.email = email;
            this.username = username;
            this.password = password;
            this.role = role;
            this.createdAt = createdAt;
        }

        // Getters and setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getRole() { return role; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getCreatedAtFormatted() {
            return createdAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        }
    }
}