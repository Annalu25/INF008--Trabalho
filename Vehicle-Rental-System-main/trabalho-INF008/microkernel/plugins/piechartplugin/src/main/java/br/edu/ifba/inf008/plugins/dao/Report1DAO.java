package br.edu.ifba.inf008.plugins.dao;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class Report1DAO {

    // Essa classe está pegando cada linha que tem no relatório1.sql e transformando em um objeto Java
    public static class FuelReport {
        public String fuelType;
        public int vehicleCount;
        public int availableCount;
        public int rentedCount;
        public double fleetPercentage;
        public String chartColor;
    }

    // Executa o report1.sql e retorna os dados para o java entender
    public List<FuelReport> getReportData() {
        List<FuelReport> list = new ArrayList<>();


        try (
            Connection conn = DataBaseConnection.getConnection();
            InputStream is = getClass()
                    .getResourceAsStream("/sql/report1.sql");
        ) {

            if (is == null) {
                throw new RuntimeException("Arquivo report1.sql não encontrado");
            }

            // Lê o banco de dados do relatorio 1
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                FuelReport fr = new FuelReport();
                fr.fuelType = rs.getString("fuel_type");
                fr.vehicleCount = rs.getInt("vehicle_count");
                fr.availableCount = rs.getInt("available_count");
                fr.rentedCount = rs.getInt("rented_count");
                fr.fleetPercentage = rs.getDouble("fleet_percentage");
                fr.chartColor = rs.getString("chart_color");

                list.add(fr);
                // Dentro do while(rs.next()) do Report1DAO
                System.out.println("Encontrado: " + fr.fuelType + " - Total: " + fr.vehicleCount);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return list;

    }
}