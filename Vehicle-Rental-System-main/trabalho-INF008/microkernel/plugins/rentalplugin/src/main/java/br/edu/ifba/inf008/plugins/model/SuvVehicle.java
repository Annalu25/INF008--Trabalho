package br.edu.ifba.inf008.plugins.model;

public class SuvVehicle extends VehicleType {

    private double extraPassengerFee;

    public SuvVehicle(double extraPassengerFee) {
        super("SUV");
        this.extraPassengerFee = extraPassengerFee;
    }

    @Override
    public double calculateAdditionalFees(int days) {
        return extraPassengerFee * days;
    }
}
