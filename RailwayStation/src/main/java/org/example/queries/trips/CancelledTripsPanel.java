package org.example.queries.trips;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class CancelledTripsPanel extends JPanel {
    private JComboBox<String> routeCombo;
    private JButton searchButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel infoLabel;

    public CancelledTripsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Отмененные рейсы"));

        initComponents();
        loadRoutes();
        loadData();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        paramsPanel.setBorder(new TitledBorder("Параметры фильтрации"));

        paramsPanel.add(new JLabel("Маршрут:"));
        routeCombo = new JComboBox<>();
        routeCombo.addItem("Все маршруты");
        routeCombo.setPreferredSize(new Dimension(300, 30));
        routeCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        routeCombo.addActionListener(e -> loadData());
        paramsPanel.add(routeCombo);

        searchButton = new JButton("Показать отмененные рейсы");
        searchButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchButton.setBackground(new Color(204, 0, 0));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> loadData());
        paramsPanel.add(searchButton);

        add(paramsPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 13));
        resultTable.setRowHeight(28);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Список отмененных рейсов"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadRoutes() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT route_id, route_name, s_start.station_name as from_station, " +
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

    private void loadData() {
        String selectedRoute = (String) routeCombo.getSelectedItem();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.route_name, s_start.station_name as from_station,\n");
        sql.append("       s_end.station_name as to_station, t.departure_datetime,\n");
        sql.append("       ts.description\n");
        sql.append("FROM trips t\n");
        sql.append("JOIN routes r ON t.route_id = r.route_id\n");
        sql.append("JOIN stations s_start ON r.start_station_id = s_start.station_id\n");
        sql.append("JOIN stations s_end ON r.end_station_id = s_end.station_id\n");
        sql.append("JOIN trip_statuses ts ON t.status_id = ts.status_id\n");
        sql.append("WHERE ts.status_name = 'Отменен'\n");

        if (selectedRoute != null && !selectedRoute.equals("Все маршруты") && !selectedRoute.contains("Все")) {
            String routeName = selectedRoute.split(" \\(")[0];
            sql.append("AND r.route_name = '").append(routeName).append("'\n");
        }

        sql.append("ORDER BY t.departure_datetime");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            Vector<String> columns = new Vector<>();
            columns.add("Маршрут");
            columns.add("Отправление");
            columns.add("Назначение");
            columns.add("Время отправления");
            columns.add("Причина отмены");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("route_name"));
                row.add(rs.getString("from_station"));
                row.add(rs.getString("to_station"));
                row.add(rs.getTimestamp("departure_datetime") != null ?
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16) : "");
                row.add(rs.getString("description") != null ? rs.getString("description") : "Отменен");
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            infoLabel.setText("Найдено отмененных рейсов: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }
}