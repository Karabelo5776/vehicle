package com.example.managementsystem;

import javafx.beans.property.*;

public class User {
    private final IntegerProperty userId;
    private final StringProperty username;
    private final StringProperty fullName;
    private final StringProperty email;
    private final StringProperty role;
    private final StringProperty createdAt;

    public User(int userId, String username, String fullName, String email, String role, String createdAt) {
        this.userId = new SimpleIntegerProperty(userId);
        this.username = new SimpleStringProperty(username);
        this.fullName = new SimpleStringProperty(fullName);
        this.email = new SimpleStringProperty(email);
        this.role = new SimpleStringProperty(role);
        this.createdAt = new SimpleStringProperty(createdAt);
    }

    public int getUserId() {
        return userId.get();
    }
    public String getUsername() {
        return username.get();
    }
    public String getFullName() {
        return fullName.get();
    }
    public String getEmail() {
        return email.get();
    }
    public String getRole() {
        return role.get();
    }
    public String getCreatedAt() {
        return createdAt.get();
    }

    public IntegerProperty userIdProperty() {
        return userId;
    }
    public StringProperty usernameProperty() {
        return username;
    }
    public StringProperty fullNameProperty() {
        return fullName;
    }
    public StringProperty emailProperty() { return email; }
    public StringProperty roleProperty() { return role; }
    public StringProperty createdAtProperty() { return createdAt; }
}
