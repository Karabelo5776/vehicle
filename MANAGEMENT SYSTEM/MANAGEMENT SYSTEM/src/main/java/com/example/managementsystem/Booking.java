package com.example.managementsystem;

import java.time.LocalDate;

public class Booking {
    private int id;
    private String vehicle;
    private LocalDate startDate;
    private LocalDate endDate;
    private double amount;
    private String status;

    public Booking(int id, String vehicle, LocalDate startDate, LocalDate endDate, double amount, String status) {
        this.id = id;
        this.vehicle = vehicle;
        this.startDate = startDate;
        this.endDate = endDate;
        this.amount = amount;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getVehicle() {
        return vehicle;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }
}