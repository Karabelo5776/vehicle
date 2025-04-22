package com.example.managementsystem;

import javafx.beans.property.*;

public class Rental {
    private final IntegerProperty id;
    private final StringProperty customerName;
    private final StringProperty vehicleInfo;
    private final StringProperty startDate;
    private final StringProperty endDate;
    private final DoubleProperty amount;
    private final StringProperty status;

    public Rental(int id, String customerName, String vehicleInfo,
                  String startDate, String endDate, double amount, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.customerName = new SimpleStringProperty(customerName);
        this.vehicleInfo = new SimpleStringProperty(vehicleInfo);
        this.startDate = new SimpleStringProperty(startDate);
        this.endDate = new SimpleStringProperty(endDate);
        this.amount = new SimpleDoubleProperty(amount);
        this.status = new SimpleStringProperty(status);
    }

    // Property getters
    public IntegerProperty idProperty() { return id; }
    public StringProperty customerNameProperty() { return customerName; }
    public StringProperty vehicleInfoProperty() { return vehicleInfo; }
    public StringProperty startDateProperty() { return startDate; }
    public StringProperty endDateProperty() { return endDate; }
    public DoubleProperty amountProperty() { return amount; }
    public StringProperty statusProperty() { return status; }

    // Regular getters
    public int getId() { return id.get(); }
    public String getCustomerName() { return customerName.get(); }
    public String getVehicleInfo() { return vehicleInfo.get(); }
    public String getStartDate() { return startDate.get(); }
    public String getEndDate() { return endDate.get(); }
    public double getAmount() { return amount.get(); }
    public String getStatus() { return status.get(); }

    // Setters
    public void setStatus(String status) { this.status.set(status); }
}