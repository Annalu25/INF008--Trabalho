package br.edu.ifba.inf008.plugins.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Rental {

    private Vehicle vehicle;
    private VehicleType vehicleType;
    private double baseRate;
    private double insuranceFee;
    private LocalDate startDate;
    private LocalDate endDate;
    private String pickupLocation;

    public Rental(Vehicle vehicle, VehicleType vehicleType,
                  double baseRate, double insuranceFee,
                  LocalDate startDate, LocalDate endDate, String pickupLocation) {

        this.vehicle = vehicle;
        this.vehicleType = vehicleType;
        this.baseRate = baseRate;
        this.insuranceFee = insuranceFee;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pickupLocation = pickupLocation;
    }

    public Vehicle getVehicle() { return vehicle; }
    public VehicleType getVehicleType() { return vehicleType; }
    public double getBaseRate() { return baseRate; }
    public double getInsuranceFee() { return insuranceFee; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getPickupLocation() { return pickupLocation; }

    public double calculateTotal() {
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        double dailyValue = baseRate * days;
        double additionalFees = vehicleType.calculateAdditionalFees((int) days);

        return dailyValue + additionalFees + insuranceFee;
    }
}
