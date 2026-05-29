package org.example.queries.trips;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DelayedTripsPanel extends JPanel {
    private JComboBox<String> reasonCombo;
    private JButton searchButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel infoLabel;

    public DelayedTripsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Задержанные рейсы"));

        initComponents();
        loadReasons();
        loadData();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        paramsPanel.setBorder(new TitledBorder("Параметры фильтрации"));

        paramsPanel.add(new JLabel("Причина задержки:"));
        reasonCombo = new JComboBox<>();
        reasonCombo.addItem("Все причины");
        reasonCombo.setPreferredSize(new Dimension(300, 30));
        reasonCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        reasonCombo.addActionListener(e -> loadData());
        paramsPanel.add(reasonCombo);

        searchButton = new JButton("Показать задержанные рейсы");
        searchButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchButton.setBackground(new Color(255, 140, 0));
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
        scrollPane.setBorder(new TitledBorder("Список задержанных рейсов"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadReasons() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT reason_name FROM delay_reasons ORDER BY reason_name")) {
            while (rs.next()) {
                reasonCombo.addItem(rs.getString("reason_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadData() {
        String selectedReason = (String) reasonCombo.getSelectedItem();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT r.route_name, s_start.station_name as from_station,\n");
        sql.append("       s_end.station_name as to_station, t.departure_datetime,\n");
        sql.append("       td.delay_time, dr.reason_name\n");
        sql.append("FROM trips t\n");
        sql.append("JOIN routes r ON t.route_id = r.route_id\n");
        sql.append("JOIN stations s_start ON r.start_station_id = s_start.station_id\n");
        sql.append("JOIN stations s_end ON r.end_station_id = s_end.station_id\n");
        sql.append("JOIN trip_delays td ON t.trip_id = td.trip_id\n");
        sql.append("JOIN delay_reasons dr ON td.reason_id = dr.reason_id\n");

        if (selectedReason != null && !selectedReason.equals("Все причины")) {
            sql.append("WHERE dr.reason_name = '").append(selectedReason).append("'\n");
        }

        sql.append("ORDER BY td.delay_time DESC");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            Vector<String> columns = new Vector<>();
            columns.add("Маршрут");
            columns.add("Отправление");
            columns.add("Назначение");
            columns.add("Время отправления");
            columns.add("Время задержки");
            columns.add("Причина");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("route_name"));
                row.add(rs.getString("from_station"));
                row.add(rs.getString("to_station"));
                row.add(rs.getTimestamp("departure_datetime") != null ?
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16) : "");
                row.add(rs.getObject("delay_time") != null ? rs.getObject("delay_time").toString() : "0");
                row.add(rs.getString("reason_name"));
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            infoLabel.setText("Найдено задержанных рейсов: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }
}