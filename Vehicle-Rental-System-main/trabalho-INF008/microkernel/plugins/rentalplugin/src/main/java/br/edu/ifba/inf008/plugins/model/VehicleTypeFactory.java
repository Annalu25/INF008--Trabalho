package br.edu.ifba.inf008.plugins.model;

public class VehicleTypeFactory {

    public static VehicleType create(String type) {

        switch (type) {
            case "LUXO":
                return new LuxuryVehicle(150);

            case "SUV":
                return new SuvVehicle(50);

            case "VAN":
                return new VanVehicle(80);

            default:
                return new EconomicVehicle();
        }
    }
}
