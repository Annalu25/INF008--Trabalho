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

        // Controller da interface igual o plugin 1
        IUIController uiController = ICore.getInstance().getUIController();

        MenuItem menuItem = new MenuItem("Relatório 1 - Gráfico de Pizza");

        menuItem.setOnAction(event -> {

            // Pega o DAO do relatorio 1
            Report1DAO dao = new Report1DAO();
            List<FuelReport> dados = dao.getReportData();

            // Faz o gráfico de pizza
            PieChart pieChart = new PieChart();
            pieChart.setTitle("Distribuição da Frota por Combustível");
            pieChart.setLegendVisible(true);
            pieChart.setLabelsVisible(true);
            pieChart.setStartAngle(90);

            // Adiciona os dados no gráfico
            for (FuelReport fr : dados) {

                PieChart.Data slice =
                        new PieChart.Data(fr.fuelType, fr.vehicleCount);

                pieChart.getData().add(slice);

                // Faz o tooltip
                Tooltip.install(
                    slice.getNode(),
                    new Tooltip(
                        "Combustível: " + fr.fuelType +
                        "\nTotal de veículos: " + fr.vehicleCount +
                        "\nDisponíveis: " + fr.availableCount +
                        "\nAlugados: " + fr.rentedCount +
                        "\nPercentual da frota: " + fr.fleetPercentage + "%"
                    )
                );

                // Coloca a cor que pede no relatorio 1
                slice.getNode().setStyle(
                    "-fx-pie-color: " + fr.chartColor + ";"
                );
            }

            // Título do topo
            Label titulo = new Label("Relatório de Frota");
            titulo.setStyle(
                "-fx-font-size: 16px;" +
                "-fx-font-weight: bold;"
            );

            // Layout final
            VBox layout = new VBox(10, titulo, pieChart);
            layout.setStyle("-fx-padding: 10;");

            // exibição do layout
            uiController.setContent(layout);
        });

        // Adiciona o menu item na interface
        uiController.addMenuItem("Relatórios", menuItem);

        return true;
    }
}