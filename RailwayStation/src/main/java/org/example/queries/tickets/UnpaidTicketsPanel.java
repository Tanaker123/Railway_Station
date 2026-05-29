package org.example.queries.tickets;

import org.example.DatabaseManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class UnpaidTicketsPanel extends JPanel {
    private JComboBox<String> tripCombo;
    private JComboBox<String> routeCombo;
    private JButton searchButton;
    private JButton showTripsButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel infoLabel;

    public UnpaidTicketsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Невыкупленные билеты"));

        initComponents();
        loadTrips();
        loadRoutes();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Параметры фильтрации"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        paramsPanel.add(new JLabel("Рейс:"), gbc);
        gbc.gridx = 1;
        tripCombo = new JComboBox<>();
        tripCombo.setPreferredSize(new Dimension(400, 30));
        tripCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(tripCombo, gbc);

        gbc.gridx = 2;
        showTripsButton = new JButton("📋 Список рейсов");
        showTripsButton.addActionListener(e -> showTripsList());
        paramsPanel.add(showTripsButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Маршрут:"), gbc);
        gbc.gridx = 1;
        routeCombo = new JComboBox<>();
        routeCombo.addItem("Все маршруты");
        routeCombo.setPreferredSize(new Dimension(400, 30));
        routeCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(routeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 3;
        searchButton = new JButton("Показать невыкупленные билеты");
        searchButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchButton.setBackground(new Color(0, 153, 76));
        searchButton.setForeground(Color.BLACK);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> executeQuery());
        paramsPanel.add(searchButton, gbc);

        add(paramsPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 13));
        resultTable.setRowHeight(28);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Невыкупленные билеты"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadTrips() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT t.trip_id, r.route_name, t.departure_datetime, " +
                             "s_start.station_name as from_station, s_end.station_name as to_station " +
                             "FROM trips t " +
                             "JOIN routes r ON t.route_id = r.route_id " +
                             "JOIN stations s_start ON r.start_station_id = s_start.station_id " +
                             "JOIN stations s_end ON r.end_station_id = s_end.station_id " +
                             "ORDER BY t.departure_datetime")) {
            tripCombo.addItem("Все рейсы");
            while (rs.next()) {
                String display = String.format("%s → %s | %s | %s",
                        rs.getString("from_station"),
                        rs.getString("to_station"),
                        rs.getString("route_name"),
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16));
                tripCombo.addItem(display);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadRoutes() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT route_name, s_start.station_name as from_station, " +
                             "s_end.station_name as to_station FROM routes r " +
                             "JOIN stations s_start ON r.start_station_id = s_start.station_id " +
                             "JOIN stations s_end ON r.end_station_id = s_end.station_id " +
                             "ORDER BY route_name")) {
            while (rs.next()) {
                routeCombo.addItem(rs.getString("route_name") + " (" +
                        rs.getString("from_station") + " → " +
                        rs.getString("to_station") + ")");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showTripsList() {
        StringBuilder sb = new StringBuilder();
        sb.append("СПИСОК РЕЙСОВ:\n\n");
        for (int i = 1; i < tripCombo.getItemCount(); i++) {
            String item = tripCombo.getItemAt(i);
            String display = item.split("\\|")[0];
            sb.append("• ").append(display).append("\n");
        }
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(600, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Список рейсов", JOptionPane.INFORMATION_MESSAGE);
    }

    private void executeQuery() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.fio AS \"Пассажир\",\n");
        sql.append("       r.route_name AS \"Маршрут\",\n");
        sql.append("       s_start.station_name AS \"Отправление\",\n");
        sql.append("       s_end.station_name AS \"Назначение\",\n");
        sql.append("       t.departure_datetime AS \"Время отправления\",\n");
        sql.append("       tk.ticket_price AS \"Цена (₽)\"\n");
        sql.append("FROM tickets tk\n");
        sql.append("JOIN passengers p ON tk.passenger_id = p.passenger_id\n");
        sql.append("JOIN trips t ON tk.trip_id = t.trip_id\n");
        sql.append("JOIN routes r ON t.route_id = r.route_id\n");
        sql.append("JOIN stations s_start ON tk.start_station_id = s_start.station_id\n");
        sql.append("JOIN stations s_end ON tk.end_station_id = s_end.station_id\n");
        sql.append("WHERE tk.status = 49\n");

        String selectedTrip = (String) tripCombo.getSelectedItem();
        if (selectedTrip != null && !selectedTrip.equals("Все рейсы") && selectedTrip.contains("|")) {
            int tripId = Integer.parseInt(selectedTrip.split("\\|")[1]);
            sql.append("AND t.trip_id = ").append(tripId).append("\n");
        }

        String selectedRoute = (String) routeCombo.getSelectedItem();
        if (selectedRoute != null && !selectedRoute.equals("Все маршруты")) {
            String routeName = selectedRoute.split(" \\(")[0];
            sql.append("AND r.route_name = '").append(routeName).append("'\n");
        }

        sql.append("ORDER BY t.departure_datetime");

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            Vector<String> columns = new Vector<>();
            columns.add("Пассажир");
            columns.add("Маршрут");
            columns.add("Отправление");
            columns.add("Назначение");
            columns.add("Время отправления");
            columns.add("Цена");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("Пассажир"));
                row.add(rs.getString("Маршрут"));
                row.add(rs.getString("Отправление"));
                row.add(rs.getString("Назначение"));
                row.add(rs.getTimestamp("Время отправления") != null ?
                        rs.getTimestamp("Время отправления").toString().substring(0, 16) : "");
                row.add(rs.getInt("Цена"));
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            infoLabel.setText("Найдено невыкупленных билетов: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }
}