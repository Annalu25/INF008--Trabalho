# Documentação detalhada do RentalPlugin

Este documento descreve em detalhes o funcionamento do `RentalPlugin` (plugin de locação de veículos) localizado em microkernel/plugins/myplugin. O objetivo é explicar cada pasta, arquivo e cada linha de código relevante, além de como configurar, executar e testar o plugin.

> Observação: caminhos e referências de arquivo dentro deste documento usam links para os arquivos no workspace.

**Índice**
- Visão geral
- Arquitetura e integração com o microkernel
- Banco de dados (conexão e schema esperado)
- Explicação arquivo a arquivo (linha a linha)
  - [RentalPlugin.java](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/RentalPlugin.java)
  - DAO: `ClientDAO.java`, `DatabaseConnection.java`, `RentalDAO.java`, `VehicleDAO.java`
  - Model: `Client.java`, `Vehicle.java`, `Rental.java`, `VehicleType` e implementações
- Como rodar e testar
- Observações e melhorias


**Visão geral**

O `RentalPlugin` implementa a interface `IPlugin` do microkernel e adiciona ao UIController um novo item de menu que abre uma aba para realizar locações de veículos. Ele faz leituras do banco de dados para carregar clientes e veículos, permite filtrar veículos por tipo, calcula o total da locação usando `VehicleType` e grava a locação no banco atualizando o status do veículo.


**Arquitetura e integração**

- O plugin implementa `br.edu.ifba.inf008.interfaces.IPlugin` e, quando inicializado (`init()`), obtém o `IUIController` via `ICore.getInstance().getUIController()` (integração com o núcleo microkernel).
- A UI é construída com JavaFX (controles como `MenuItem`, `ComboBox`, `TableView`, `DatePicker`, `Alert`, etc.).
- O acesso a dados usa classes DAO (`ClientDAO`, `RentalDAO`, `VehicleDAO`) e uma classe utilitária de conexão `DatabaseConnection` que fornece um `Connection` JDBC.
- Modelos: `Client`, `Vehicle`, `Rental`, `VehicleType` e subclasses (`EconomicVehicle`, `LuxuryVehicle`, etc.) encapsulam dados e regras (por exemplo, cálculo de taxas adicionais no `VehicleType`).


**Banco de dados**

- Configuração de conexão (em `DatabaseConnection.java`):
  - URL: `jdbc:mariadb://localhost:3306/locadora`
  - Usuário: `root`
  - Senha: `mariadb09`
- Tabelas esperadas (mínimo): `clients`, `vehicles`, `rentals` com colunas usadas nos DAOs e em consultas do plugin.
  - `clients` (colunas lidas: `id`, `email`)
  - `vehicles` (colunas lidas: `id`, `make`, `model`, `year`, `fuel_type`, `transmission`, `mileage`, `status`, `type`)
  - `rentals` (colunas usadas na inserção: `vehicle_id`, `start_date`, `scheduled_end_date`, `base_rate`, `insurance_fee`, `total`)


**Explicação arquivo a arquivo (linha a linha)**

---

**RentalPlugin.java**

Arquivo: [microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/RentalPlugin.java](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/RentalPlugin.java)

Conteúdo (trecho com comentários linha a linha):

```java
package br.edu.ifba.inf008.plugins; // Declara o pacote do plugin

import br.edu.ifba.inf008.interfaces.IPlugin; // Interface do microkernel para plugins
import br.edu.ifba.inf008.interfaces.ICore; // Acesso ao core do microkernel
import br.edu.ifba.inf008.interfaces.IUIController; // Controller de UI fornecido pelo core
import br.edu.ifba.inf008.plugins.dao.ClientDAO; // DAO para clientes
import br.edu.ifba.inf008.plugins.dao.DatabaseConnection; // Utilitário de conexão JDBC
import br.edu.ifba.inf008.plugins.dao.RentalDAO; // DAO para inserir locações
import br.edu.ifba.inf008.plugins.dao.VehicleDAO; // DAO para atualizar status do veículo
import br.edu.ifba.inf008.plugins.model.Client; // Modelo Client
import br.edu.ifba.inf008.plugins.model.Rental; // Modelo Rental
import br.edu.ifba.inf008.plugins.model.Vehicle; // Modelo Vehicle
import br.edu.ifba.inf008.plugins.model.VehicleType; // Abstração de tipo de veículo
import br.edu.ifba.inf008.plugins.model.VehicleTypeFactory; // Fábrica para criar VehicleType
import javafx.beans.property.SimpleStringProperty; // JavaFX para coluna personalizada
import javafx.collections.FXCollections; // Utilitário JavaFX para coleções observáveis
import javafx.collections.ObservableList; // Lista observável
import javafx.collections.transformation.FilteredList; // Lista filtrada para tabela
import javafx.scene.control.*; // Controles JavaFX (Label, Button, TableView, etc.)
import javafx.scene.control.cell.PropertyValueFactory; // Cria células por propriedade
import javafx.scene.layout.VBox; // Layout vertical

import java.sql.Connection; // JDBC Connection
import java.sql.PreparedStatement; // JDBC PreparedStatement
import java.sql.ResultSet; // JDBC ResultSet
import java.sql.SQLException; // Exceção SQL
import java.util.ArrayList; // Lista utilitária
import java.util.List; // Interface List
import java.util.Optional; // Para diálogos de confirmação

public class RentalPlugin implements IPlugin { // Implementa IPlugin do microkernel

    @Override
    public boolean init() { // Método chamado para inicializar o plugin

        IUIController uiController = ICore.getInstance().getUIController(); // Obtém o controller de UI do core
        MenuItem menuItem = uiController.createMenuItem("Menu 1", "Rental Menu"); // Cria um item de menu

        // Registrar ação do menu
        menuItem.setOnAction(e -> { // Define o que acontece ao clicar no menu
            VBox box = new VBox(10); // Layout vertical com espaçamento 10
            Label titulo = new Label("Tela de Locação de Veículos"); // Título da tela

            ComboBox<Client> cliente = new ComboBox<>(); // ComboBox para seleção de cliente
            cliente.setPromptText("Selecione o cliente");

            try (Connection conn = DatabaseConnection.getConnection()) { // Obtém conexão
                ClientDAO clientDAO = new ClientDAO(conn); // Instancia ClientDAO
                cliente.getItems().addAll(clientDAO.findAll()); // Carrega clientes no ComboBox
            } catch (Exception ex) {
                ex.printStackTrace(); // Em caso de erro, imprime stacktrace
            }

            // --- ComboBox de tipos de veículo (do banco com fallback) ---
            ComboBox<String> tipoVeiculo = new ComboBox<>();
            tipoVeiculo.setPromptText("Selecione o tipo de veículo");

            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT type FROM vehicles");
                    ResultSet rs = ps.executeQuery()) {

                List<String> types = new ArrayList<>();
                while (rs.next()) {
                    types.add(rs.getString(1)); // Lê cada tipo distinto da tabela vehicles
                }

                if (types.isEmpty()) { // Se nenhum tipo no banco, usa fallback
                    tipoVeiculo.getItems().addAll("ECONÔMICO", "COMPACT", "SUV", "LUXO", "VAN", "ELÉTRICO");
                } else {
                    tipoVeiculo.getItems().addAll(types); // Caso contrário, adiciona tipos do banco
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
                tipoVeiculo.getItems().addAll("ECONÔMICO", "COMPACT", "SUV", "LUXO", "VAN", "ELÉTRICO");
            }

            // --- Carregar veículos do banco ---
            ObservableList<Vehicle> allVehicles = FXCollections.observableArrayList(); // Lista observável

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
                    allVehicles.add(vehicle); // Adiciona cada veículo lido à lista observável
                }

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            // --- TableView e FilteredList ---
            FilteredList<Vehicle> filteredVehicles = new FilteredList<>(allVehicles, v -> true); // Lista filtrável

            TableView<Vehicle> table = new TableView<>();
            table.setPrefHeight(220);
            table.setItems(filteredVehicles); // Atrelando a lista filtrada à tabela

            TableColumn<Vehicle, String> colMakeModel = new TableColumn<>("Marca / Modelo");
            colMakeModel.setCellValueFactory(cell -> new SimpleStringProperty(
                    cell.getValue().getMake() + " " + cell.getValue().getModel())); // Coluna concatenada

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

                    return tipoOk && statusOk; // Retorna true somente para veículos do tipo selecionado e não alugados
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
                    return; // Encerra se nenhum veículo selecionado
                }

                try {
                    double diaria = Double.parseDouble(baseRate.getText()); // Converte diária
                    double seguro = Double.parseDouble(insuranceFee.getText()); // Converte seguro
                    VehicleType type = VehicleTypeFactory.create(selectedVehicle.getType()); // Cria VehicleType

                    Rental rental = new Rental(
                            selectedVehicle,
                            type,
                            diaria,
                            seguro,
                            inicio.getValue(),
                            fim.getValue(),
                            pickupLocation.getText()); // Cria objeto Rental

                    double total = rental.calculateTotal(); // Calcula total
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

                            rentalDAO.insert(rental); // Insere locação
                            vehicleDAO.updateStatus(selectedVehicle.getId(), "RENTED"); // Atualiza status

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
            uiController.createTab("Locação de Veículos", box); // Abre aba no UIController do core
        });

        return true; // init retornando true indica sucesso
    }
}
```

Notas importantes sobre esse arquivo:
- Uso de `DatabaseConnection.getConnection()` em vários blocos `try-with-resources` garante que `Connection`, `PreparedStatement` e `ResultSet` sejam fechados automaticamente.
- `Vehicle` é instanciado com dados lidos do banco; observe que o `id` do `Vehicle` é gerado localmente por `Vehicle.ID_GENERATOR` — se você deseja mapear o `id` do banco, será necessário adaptar `Vehicle` para aceitar o `id` retornado pelo banco.
- O filtro garante que veículos com `status = 'RENTED'` não apareçam como disponíveis.

---

**DAO: `ClientDAO.java`**

Arquivo: [microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/dao/ClientDAO.java](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/dao/ClientDAO.java)

Código com explicação:

```java
package br.edu.ifba.inf008.plugins.dao;

import br.edu.ifba.inf008.plugins.model.Client;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClientDAO {

    private final Connection conn; // Conexão fornecida pelo chamador

    public ClientDAO(Connection conn) {
        this.conn = conn; // Injeção da conexão (não abre/fecha aqui)
    }

    public List<Client> findAll() {
        List<Client> clients = new ArrayList<>();

        String sql = "SELECT id, email FROM clients ORDER BY email"; // Consulta mínima

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
            e.printStackTrace(); // Tratamento simples de exceção
        }

        return clients; // Retorna lista (pode ser vazia)
    }
}
```

Notas:
- `ClientDAO` não fecha a `Connection`: cabe ao chamador (o plugin) gerenciá-la.
- A consulta seleciona apenas `id` e `email` — versão mínima necessária para o `ComboBox`.

---

**DAO: `DatabaseConnection.java`**

Arquivo: [microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/dao/DatabaseConnection.java](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/dao/DatabaseConnection.java)

Conteúdo:

```java
package br.edu.ifba.inf008.plugins.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL = "jdbc:mariadb://localhost:3306/locadora";
    private static final String USER = "root";
    private static final String PASSWORD = "mariadb09";

    private static Connection connection; // Singleton simples

    // Retorna a conexão única (singleton)
    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        }
        return connection;
    }
}
```

Notas e recomendações:
- Conexão em singleton simplista — se a aplicação for multithreaded, considere uso de pool (HikariCP, DBCP).
- Credenciais hard-coded — mova para arquivo de configuração (ex.: `application.properties`) ou variáveis de ambiente.

---

**DAO: `RentalDAO.java`**

Arquivo: [microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/dao/RentalDAO.java](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/dao/RentalDAO.java)

Código:

```java
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
```

Notas críticas:
- `stmt.setInt(1, rental.getVehicle().getId());` usa o `id` do objeto `Vehicle`. No `Vehicle` atual, o `id` é gerado localmente via `AtomicInteger` (não é o ID do banco). Para gravar corretamente a referência `vehicle_id` para um registro persistido, o `Vehicle` precisa refletir o `id` do banco (ou a tabela `vehicles` deve ter IDs compatíveis com o gerador local). Caso contrário, o `vehicle_id` inserido poderá não corresponder a nenhum veículo no BD.
- As datas usam `java.sql.Date.valueOf(LocalDate)` — requer que `startDate` e `endDate` não sejam nulos.

---

**DAO: `VehicleDAO.java`**

Arquivo: [microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/dao/VehicleDAO.java](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/dao/VehicleDAO.java)

Conteúdo:

```java
package br.edu.ifba.inf008.plugins.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class VehicleDAO {

    private Connection connection;

    public VehicleDAO(Connection connection) {
        this.connection = connection;
    }

    public void updateStatus(int vehicleId, String status) {
        String sql = "UPDATE vehicles SET status = ? WHERE id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, vehicleId);
            stmt.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

Notas:
- Atualiza o status do veículo no BD. Mesma observação sobre `vehicleId`: precisa ser consistente com o `id` armazenado no BD.

---

**Modelos**

Explicações e observações sobre cada classe do modelo (caminho: `microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/model`)

- `Client.java` ([link](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/model/Client.java))
  - Contém `id` e `email`.
  - `toString()` retorna `email`, portanto aparece assim no `ComboBox`.

- `Vehicle.java` ([link](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/model/Vehicle.java))
  - Campos: `id` (gerado com `AtomicInteger`), `make`, `model`, `year`, `fuelType`, `transmission`, `mileage`, `status`, `type`.
  - Observação importante: `id` é gerado localmente — se quiser o `id` do banco, adapte o construtor para receber `id` (e ao ler do DB, usar `rs.getInt("id")`).

- `Rental.java` ([link](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/model/Rental.java))
  - Campos: `vehicle`, `vehicleType`, `baseRate`, `insuranceFee`, `startDate`, `endDate`, `pickupLocation`.
  - `calculateTotal()` calcula dias entre datas (inclusivo), soma `baseRate * days`, adiciona fees calculadas pelo `VehicleType` e `insuranceFee`.
  - Atenção: `ChronoUnit.DAYS.between(startDate, endDate) + 1` assume `endDate >= startDate`.

- `VehicleType` (abstrato) e implementações (`EconomicVehicle`, `CompactVehicle`, `EletricVehicle`, `LuxuryVehicle`, `SuvVehicle`, `VanVehicle`) ([link para a fábrica](microkernel/plugins/myplugin/src/main/java/br/edu/ifba/inf008/plugins/model/VehicleTypeFactory.java))
  - Cada implementação define `calculateAdditionalFees(int days)` retornando taxa adicional por dia.
  - `VehicleTypeFactory.create(type)` escolhe a implementação baseada na string `type` (ex.: "LUXO", "SUV", "VAN").

- `TestConnection.java` — utilitário com `main` para testar a conexão com o banco.

---

**Como rodar e testar**

1. Preparar o banco de dados (MariaDB/MySQL) com um schema `locadora` e as tabelas `clients`, `vehicles`, `rentals`. Um exemplo mínimo de `init.sql` (ajuste colunas/constraints conforme necessidade):

```sql
CREATE DATABASE IF NOT EXISTS locadora;
USE locadora;

CREATE TABLE IF NOT EXISTS clients (
  id INT AUTO_INCREMENT PRIMARY KEY,
  email VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS vehicles (
  id INT AUTO_INCREMENT PRIMARY KEY,
  make VARCHAR(100),
  model VARCHAR(100),
  year INT,
  fuel_type VARCHAR(50),
  transmission VARCHAR(50),
  mileage INT,
  status VARCHAR(50),
  type VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS rentals (
  id INT AUTO_INCREMENT PRIMARY KEY,
  vehicle_id INT,
  start_date DATE,
  scheduled_end_date DATE,
  base_rate DOUBLE,
  insurance_fee DOUBLE,
  total DOUBLE,
  FOREIGN KEY (vehicle_id) REFERENCES vehicles(id)
);
```

2. Ajustar credenciais de `DatabaseConnection.java` se necessário ou configurar um usuário com as credenciais existentes.

3. Executar o projeto (módulo `microkernel`): há um `pom.xml` na raiz de `microkernel`; usar Maven para compilar e rodar a aplicação.

Comandos (no Windows Powershell, a partir da pasta `microkernel`):

```powershell
mvn -DskipTests clean package
# Em seguida, executar a aplicação principal (se houver um main configurado no pom) ou rodar via IDE.
mvn -pl app exec:java        
```

4. Testes manuais:
- Inicie a aplicação do microkernel que cria a UI JavaFX.
- No menu criado pelo plugin (`Menu 1 -> Rental Menu`), abra a aba "Locação de Veículos".
- Verifique se o `ComboBox` de clientes está populado (se não, verifique dados na tabela `clients`).
- Verifique se o `ComboBox` de tipos mostra tipos do banco ou fallback.
- Selecione um veículo disponível (status diferente de `RENTED`), preencha datas, valores e confirme a locação.
- Confirme que a tabela `rentals` recebeu um novo registro e que `vehicles.status` mudou para `RENTED`.

5. Testes programáticos rápidos:
- Rode `TestConnection` para checar conexão com o DB:

```powershell
# A partir da pasta do módulo do plugin (ou importando a classe na IDE) compile e execute:
# Exemplo: usando mvn exec:java ou executar via IDE
```

---

**Observações e melhorias sugeridas**

- Sincronizar o `id` do `Vehicle` com o `id` do banco: ao ler veículos, use `rs.getInt("id")` e altere `Vehicle` para aceitar `id` no construtor (ou adicionar setter), evitando inconsistências ao inserir `rental`.
- Substituir conexão singleton por pool de conexões.
- Melhorar tratamento de exceções e mensagens de erro ao usuário.
- Validar datas (ex.: `endDate` não pode ser anterior a `startDate`).
- Internacionalização: valores monetários formatados corretamente com `NumberFormat`.
- Sanitização/validação de inputs (ex.: campos numéricos, campos obrigatórios).


---

Se deseja, eu posso:
- Gerar um script SQL `init.sql` completo com dados de exemplo.
- Atualizar a classe `Vehicle` para aceitar `id` vindo do BD e adaptar os pontos de leitura para usar `rs.getInt("id")`.
- Mover as configurações de DB para variáveis de ambiente.

Quer que eu aplique alguma dessas melhorias agora?
