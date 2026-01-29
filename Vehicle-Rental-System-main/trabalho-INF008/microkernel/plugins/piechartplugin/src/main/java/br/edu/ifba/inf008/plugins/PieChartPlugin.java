package br.edu.ifba.inf008.plugins;

import br.edu.ifba.inf008.interfaces.IPlugin;
import br.edu.ifba.inf008.interfaces.ICore;
import br.edu.ifba.inf008.interfaces.IUIController;

import br.edu.ifba.inf008.plugins.dao.Report1DAO;
import br.edu.ifba.inf008.plugins.dao.Report1DAO.FuelReport;

import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;


import java.util.List;

public class PieChartPlugin implements IPlugin {

    @Override
    public boolean init() {

        IUIController uiController = ICore.getInstance().getUIController();
        System.out.println("Inicializando o PieChartPlugin...");

        MenuItem menuItem = uiController.createMenuItem(
            "Relatório 1",
            "Gráfico de Pizza"
        );

        menuItem.setOnAction(e -> {

            Report1DAO dao = new Report1DAO();
            List<FuelReport> dados = dao.getReportData();

            PieChart pieChart = new PieChart();
            pieChart.setTitle("Distribuição dos veículos por tipo de Combustível");
            pieChart.setPrefSize(500, 400);
            pieChart.setLegendVisible(true);
            pieChart.setLabelsVisible(true);
            pieChart.setStartAngle(90);

            for (FuelReport fr : dados) {

                PieChart.Data slice = new PieChart.Data(fr.fuelType, fr.vehicleCount);

                pieChart.getData().add(slice);

                slice.nodeProperty().addListener((obs, oldNode, newNode) -> {
                    if (newNode != null) {
                        newNode.setStyle("-fx-pie-color: " + fr.chartColor + " !important;");
                    }
                });
            }

            Label titulo = new Label("Gráfico de Pizza por tipo de Combustível");
            titulo.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;"
            );

            VBox layout = new VBox(10, titulo, pieChart);
            layout.setStyle("-fx-padding: 10;");

            uiController.createTab("Gráfico do tipo de combustivel", layout);
        });

        return true;
    }
}