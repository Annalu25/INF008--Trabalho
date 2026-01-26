package br.edu.ifba.inf008.plugins.model;

public class EletricVehicle extends VehicleType {

	private double chargingFee;

	public EletricVehicle() {
		super("ELÉTRICO");
		this.chargingFee = 8.0;
	}

	public EletricVehicle(double chargingFee) {
		super("ELÉTRICO");
		this.chargingFee = chargingFee;
	}

	@Override
	public double calculateAdditionalFees(int days) {
		return chargingFee * days;
	}
}

