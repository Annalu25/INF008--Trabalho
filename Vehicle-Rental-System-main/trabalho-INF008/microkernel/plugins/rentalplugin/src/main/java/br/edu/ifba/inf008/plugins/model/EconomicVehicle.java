package br.edu.ifba.inf008.plugins.model;

public class EconomicVehicle extends VehicleType {

    public EconomicVehicle() {
        super("ECONÔMICO");
    }

    @Override
    public double calculateAdditionalFees(int days) {
        return 0.0;
    }
}

