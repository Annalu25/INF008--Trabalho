package br.edu.ifba.inf008.plugins.model;

public class CompactVehicle extends VehicleType {

	private double dailyCompactFee;

	public CompactVehicle() {
		super("COMPACT");
		this.dailyCompactFee = 5.0;
	}

	public CompactVehicle(double dailyCompactFee) {
		super("COMPACT");
		this.dailyCompactFee = dailyCompactFee;
	}

	@Override
	public double calculateAdditionalFees(int days) {
		return dailyCompactFee * days;
	}
}
