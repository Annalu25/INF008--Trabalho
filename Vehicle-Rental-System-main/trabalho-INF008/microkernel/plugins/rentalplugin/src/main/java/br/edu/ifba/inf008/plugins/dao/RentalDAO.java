package br.edu.ifba.inf008.plugins.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import br.edu.ifba.inf008.plugins.model.Rental;

public class RentalDAO {

    private Connection connection;

    public RentalDAO(Connection connection) {
        this.connection = connection;
    }

    public void insert(Rental rental) {

        if (connection == null) {
            throw new IllegalStateException("No database connection available");
        }

        String sql =
            "INSERT INTO rentals " +
            "(vehicle_id, start_date, scheduled_end_date, base_rate, insurance_fee, total) " +
            "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, rental.getVehicle().getId());
            stmt.setDate(2, java.sql.Date.valueOf(rental.getStartDate()));
            stmt.setDate(3, java.sql.Date.valueOf(rental.getEndDate()));
            stmt.setDouble(4, rental.getBaseRate());
            stmt.setDouble(5, rental.getInsuranceFee());
            stmt.setDouble(6, rental.calculateTotal());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
