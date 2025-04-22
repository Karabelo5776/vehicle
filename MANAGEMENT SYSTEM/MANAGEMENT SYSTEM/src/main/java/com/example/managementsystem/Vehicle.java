package com.example.managementsystem;

import javafx.beans.property.*;

public class Vehicle {
    private final IntegerProperty id;
    private final StringProperty brand;
    private final StringProperty model;
    private final StringProperty category;
    private final IntegerProperty year;
    private final DoubleProperty pricePerDay;
    private final StringProperty status;

    public Vehicle(int id, String brand, String model, String category,
                   int year, double pricePerDay, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.brand = new SimpleStringProperty(brand);
        this.model = new SimpleStringProperty(model);
        this.category = new SimpleStringProperty(category);
        this.year = new SimpleIntegerProperty(year);
        this.pricePerDay = new SimpleDoubleProperty(pricePerDay);
        this.status = new SimpleStringProperty(status);
    }

    // Property Getters
    public IntegerProperty idProperty() { return id; }
    public StringProperty brandProperty() { return brand; }
    public StringProperty modelProperty() { return model; }
    public StringProperty categoryProperty() { return category; }
    public IntegerProperty yearProperty() { return year; }
    public DoubleProperty pricePerDayProperty() { return pricePerDay; }
    public StringProperty statusProperty() { return status; }

    // Regular Getters
    public int getId() { return id.get(); }
    public String getBrand() { return brand.get(); }
    public String getModel() { return model.get(); }
    public String getCategory() { return category.get(); }
    public int getYear() { return year.get(); }
    public double getPricePerDay() { return pricePerDay.get(); }
    public String getStatus() { return status.get(); }

    // Setters
    public void setId(int id) { this.id.set(id); }
    public void setBrand(String brand) { this.brand.set(brand); }
    public void setModel(String model) { this.model.set(model); }
    public void setCategory(String category) { this.category.set(category); }
    public void setYear(int year) { this.year.set(year); }
    public void setPricePerDay(double pricePerDay) { this.pricePerDay.set(pricePerDay); }
    public void setStatus(String status) { this.status.set(status); }
}
