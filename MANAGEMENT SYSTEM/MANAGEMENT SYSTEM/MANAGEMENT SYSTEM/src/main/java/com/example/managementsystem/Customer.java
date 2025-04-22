package com.example.managementsystem;

import javafx.beans.property.*;

public class Customer {

    private final IntegerProperty id;
    private final StringProperty name;
    private final StringProperty email;
    private final StringProperty phone;
    private final StringProperty license;
    private final IntegerProperty rentalCount;

    public Customer(int id, String name, String email, String phone, String license, int rentalCount) {
        this.id = new SimpleIntegerProperty(id);
        this.name = new SimpleStringProperty(name);
        this.email = new SimpleStringProperty(email);
        this.phone = new SimpleStringProperty(phone);
        this.license = new SimpleStringProperty(license);
        this.rentalCount = new SimpleIntegerProperty(rentalCount);
    }

    // Property Getters for JavaFX bindings
    public IntegerProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty emailProperty() { return email; }
    public StringProperty phoneProperty() { return phone; }
    public StringProperty licenseProperty() { return license; }
    public IntegerProperty rentalCountProperty() { return rentalCount; }

    // Regular Getters
    public int getId() { return id.get(); }
    public String getName() { return name.get(); }
    public String getEmail() { return email.get(); }
    public String getPhone() { return phone.get(); }
    public String getLicense() { return license.get(); }
    public int getRentalCount() { return rentalCount.get(); }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setName(String name) { this.name.set(name); }
    public void setEmail(String email) { this.email.set(email); }
    public void setPhone(String phone) { this.phone.set(phone); }
    public void setLicense(String license) { this.license.set(license); }
    public void setRentalCount(int rentalCount) { this.rentalCount.set(rentalCount); }
}
