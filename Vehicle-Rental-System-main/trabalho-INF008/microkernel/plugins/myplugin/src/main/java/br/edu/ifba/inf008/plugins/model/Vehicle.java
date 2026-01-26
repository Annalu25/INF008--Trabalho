package br.edu.ifba.inf008.plugins.model;

import java.util.concurrent.atomic.AtomicInteger;

public class Vehicle {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(1);

    private final int id;
    private String make;
    private String model;
    private int year;
    private String fuelType;
    private String transmission;
    private int mileage;
    private String status;
    private String type;

    public Vehicle(
            String make,
            String model,
            int year,
            String fuelType,
            String transmission,
            int mileage,
            String status,
            String type
    ) {
        this.id = ID_GENERATOR.getAndIncrement();
        this.make = make;
        this.model = model;
        this.year = year;
        this.fuelType = fuelType;
        this.transmission = transmission;
        this.mileage = mileage;
        this.status = status;
        this.type = type;
    }

    public int getId() { return id; }
    public String getMake() { return make; }
    public String getModel() { return model; }
    public int getYear() { return year; }
    public String getFuelType() { return fuelType; }
    public String getTransmission() { return transmission; }
    public int getMileage() { return mileage; }
    public String getStatus() { return status; }
    public String getType() { return type; }

}
