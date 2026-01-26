package br.edu.ifba.inf008.plugins.dao;

import br.edu.ifba.inf008.plugins.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    private final Connection conn;

    public ClientDAO(Connection conn) {
        this.conn = conn;
    }

    public List<Client> findAll() {
        List<Client> clients = new ArrayList<>();

        String sql = "SELECT id, email FROM clients ORDER BY email";

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                clients.add(
                    new Client(
                        rs.getInt("id"),
                        rs.getString("email")
                    )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return clients;
    }
}
