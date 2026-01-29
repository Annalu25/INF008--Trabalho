package br.edu.ifba.inf008.plugins.model;

import br.edu.ifba.inf008.plugins.dao.DatabaseConnection;
import java.sql.Connection;
import java.sql.SQLException;

public class TestConnection {
    public static void main(String[] args) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("Conexão bem-sucedida!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
