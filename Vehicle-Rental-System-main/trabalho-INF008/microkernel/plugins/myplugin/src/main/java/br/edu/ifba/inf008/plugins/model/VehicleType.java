package br.edu.ifba.inf008.plugins.model;

public abstract class VehicleType {

    protected String name;

    public VehicleType(String name) {
        this.name = name;
    }

    public abstract double calculateAdditionalFees(int days);

    public String getName() {
        return name;
    }
}
