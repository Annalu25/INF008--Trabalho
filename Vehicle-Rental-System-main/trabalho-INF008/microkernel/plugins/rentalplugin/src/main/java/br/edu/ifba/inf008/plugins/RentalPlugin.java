package br.edu.ifba.inf008.plugins;

import br.edu.ifba.inf008.interfaces.IPlugin;
import br.edu.ifba.inf008.interfaces.ICore;
import br.edu.ifba.inf008.interfaces.IUIController;
import br.edu.ifba.inf008.plugins.dao.ClientDAO;
import br.edu.ifba.inf008.plugins.dao.DatabaseConnection;
import br.edu.ifba.inf008.plugins.dao.RentalDAO;
import br.edu.ifba.inf008.plugins.dao.VehicleDAO;
import br.edu.ifba.inf008.plugins.model.Client;
import br.edu.ifba.inf008.plugins.model.Rental;
import br.edu.ifba.inf008.plugins.model.Vehicle;
import br.edu.ifba.inf008.plugins.model.VehicleType;
import br.edu.ifba.inf008.plugins.model.VehicleTypeFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RentalPlugin implements IPlugin {

    @Override
    public boolean init() {

        IUIController uiController = ICore.getInstance().getUIController();
        MenuItem menuItem = uiController.createMenuItem("Menu 1", "Rental Menu");

        menuItem.setOnAction(e -> {
            VBox box = new VBox(10); 
            Label titulo = new Label("Tela de Locação de Veículos");

            ComboBox<Client> cliente = new ComboBox<>();
            cliente.setPromptText("Selecione o cliente");

            try (Connection conn = DatabaseConnection.getConnection()) {
                ClientDAO clientDAO = new ClientDAO(conn);
                cliente.getItems().addAll(clientDAO.findAll());
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            // --- ComboBox de tipos de veículo (do banco com fallback) ---
            ComboBox<String> tipoVeiculo = new ComboBox<>();
            tipoVeiculo.setPromptText("Selecione o tipo de veículo");

            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT type FROM vehicles");
                    ResultSet rs = ps.executeQuery()) {

                List<String> types = new ArrayList<>();
                while (rs.next()) {
                    types.add(rs.getString(1));
                }

                if (types.isEmpty()) {
                    tipoVeiculo.getItems().addAll("ECONÔMICO", "COMPACT", "SUV", "LUXO", "VAN", "ELÉTRICO");
                } else {
                    tipoVeiculo.getItems().addAll(types);
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                tipoVeiculo.getItems().addAll("ECONÔMICO", "COMPACT", "SUV", "LUXO", "VAN", "ELÉTRICO");
            }

            // --- Carregar veículos do banco ---
            ObservableList<Vehicle> allVehicles = FXCollections.observableArrayList();

            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement("SELECT * FROM vehicles");
                    ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {
                    Vehicle vehicle = new Vehicle(

                            rs.getString("make"),
                            rs.getString("model"),
                            rs.getInt("year"),
                            rs.getString("fuel_type"),
                            rs.getString("transmission"),
                            rs.getInt("mileage"),
                            rs.getString("status"),
                            rs.getString("type"));
                    allVehicles.add(vehicle);
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            // --- TableView e FilteredList ---
            FilteredList<Vehicle> filteredVehicles = new FilteredList<>(allVehicles, v -> true);

            TableView<Vehicle> table = new TableView<>();
            table.setPrefHeight(220);
            table.setItems(filteredVehicles);

            TableColumn<Vehicle, String> colMakeModel = new TableColumn<>("Marca / Modelo");
            colMakeModel.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getMake() + " " + cell.getValue().getModel()));

            TableColumn<Vehicle, Integer> colYear = new TableColumn<>("Ano");
            colYear.setCellValueFactory(new PropertyValueFactory<>("year"));

            TableColumn<Vehicle, String> colFuel = new TableColumn<>("Combustível");
            colFuel.setCellValueFactory(new PropertyValueFactory<>("fuelType"));

            TableColumn<Vehicle, String> colTransmission = new TableColumn<>("Câmbio");
            colTransmission.setCellValueFactory(new PropertyValueFactory<>("transmission"));

            TableColumn<Vehicle, Integer> colMileage = new TableColumn<>("Km");
            colMileage.setCellValueFactory(new PropertyValueFactory<>("mileage"));
            
            TableColumn<Vehicle, String> colStatus = new TableColumn<>("Status");
            colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
            
            table.getColumns().addAll(colMakeModel, colYear, colFuel, colTransmission, colMileage);
            table.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

            // --- Filtro por tipo selecionado ---
            tipoVeiculo.setOnAction(ev -> {
                String tipoSelecionado = tipoVeiculo.getValue();
                System.out.println(tipoVeiculo.getValue());

                filteredVehicles.setPredicate(vehicle -> {
                    String vType = vehicle.getType() == null ? "" : vehicle.getType().trim();
                    String vStatus = vehicle.getStatus() == null ? "" : vehicle.getStatus().trim();

                    boolean tipoOk = (tipoSelecionado == null || tipoSelecionado.trim().isEmpty())
                            || tipoSelecionado.trim().equalsIgnoreCase(vType);
                    boolean statusOk = !"RENTED".equalsIgnoreCase(vStatus);

                    System.out.println(
                            "TYPE=[" + vehicle.getType() + "] | STATUS=[" + vehicle.getStatus() + "]");

                    return tipoOk && statusOk;
                });
            });

            // --- Campos adicionais ---
            DatePicker inicio = new DatePicker();
            inicio.setPromptText("Data de início");

            DatePicker fim = new DatePicker();
            fim.setPromptText("Data de término");

            TextField pickupLocation = new TextField();
            pickupLocation.setPromptText("Local de retirada");

            TextField baseRate = new TextField();
            baseRate.setPromptText("Valor da diária");

            TextField insuranceFee = new TextField();
            insuranceFee.setPromptText("Valor do seguro");

            Label totalLabel = new Label("Total: R$ 0,00");

            Button confirmar = new Button("Confirmar Locação");

            confirmar.setOnAction(ev2 -> {
                Vehicle selectedVehicle = table.getSelectionModel().getSelectedItem();

                if (selectedVehicle == null) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Erro");
                    alert.setHeaderText(null);
                    alert.setContentText("Selecione um veículo para locação!");
                    alert.showAndWait();
                    return;
                }

                try {
                    double diaria = Double.parseDouble(baseRate.getText());
                    double seguro = Double.parseDouble(insuranceFee.getText());
                    VehicleType type = VehicleTypeFactory.create(selectedVehicle.getType());

                    Rental rental = new Rental(
                            selectedVehicle,
                            type,
                            diaria,
                            seguro,
                            inicio.getValue(),
                            fim.getValue(),
                            pickupLocation.getText());

                    double total = rental.calculateTotal();
                    totalLabel.setText("Total: R$ " + total);

                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Confirmar locação");
                    confirmAlert.setHeaderText("Valor total: R$ " + total);
                    confirmAlert.setContentText("Deseja confirmar a locação?");
                    Optional<ButtonType> result = confirmAlert.showAndWait();

                    if (result.isPresent() && result.get() == ButtonType.OK) {
                        // Abrir conexão somente aqui
                        try (Connection conn = DatabaseConnection.getConnection()) {
                            RentalDAO rentalDAO = new RentalDAO(conn);
                            VehicleDAO vehicleDAO = new VehicleDAO(conn);

                            rentalDAO.insert(rental);
                            vehicleDAO.updateStatus(selectedVehicle.getId(), "RENTED");

                            Alert success = new Alert(Alert.AlertType.INFORMATION);
                            success.setTitle("Sucesso");
                            success.setHeaderText(null);
                            success.setContentText("Locação realizada com sucesso!");
                            success.showAndWait();
                        }
                    }

                } catch (NumberFormatException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erro");
                    alert.setHeaderText(null);
                    alert.setContentText("Digite valores válidos para diária e seguro!");
                    alert.showAndWait();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Erro");
                    alert.setHeaderText(null);
                    alert.setContentText("Ocorreu um erro ao processar a locação.");
                    alert.showAndWait();
                }
            });

            box.getChildren().addAll(titulo, cliente, tipoVeiculo, table, inicio, fim,
                    pickupLocation, baseRate, insuranceFee, totalLabel, confirmar);
            uiController.createTab("Locação de Veículos", box);
        });

        return true;
    }
}
