package br.edu.ifba.inf008.plugins.model;

public class VanVehicle extends VehicleType {

	private double cargoFee;

	public VanVehicle() {
		super("VAN");
		this.cargoFee = 15.0;
	}

	public VanVehicle(double cargoFee) {
		super("VAN");
		this.cargoFee = cargoFee;
	}

	@Override
	public double calculateAdditionalFees(int days) {
		return cargoFee * days;
	}
}

