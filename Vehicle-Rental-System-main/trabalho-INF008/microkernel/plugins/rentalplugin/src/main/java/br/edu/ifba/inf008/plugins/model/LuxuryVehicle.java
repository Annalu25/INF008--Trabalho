package br.edu.ifba.inf008.plugins.model;

public class LuxuryVehicle extends VehicleType {

	private double luxuryServiceFee;

	public LuxuryVehicle() {
		super("LUXO");
		this.luxuryServiceFee = 50.0;
	}

	public LuxuryVehicle(double luxuryServiceFee) {
		super("LUXO");
		this.luxuryServiceFee = luxuryServiceFee;
	}

	@Override
	public double calculateAdditionalFees(int days) {
		return luxuryServiceFee * days;
	}
}

