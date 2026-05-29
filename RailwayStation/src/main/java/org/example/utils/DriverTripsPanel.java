package org.example.utils;

import org.example.DatabaseConfig;
import org.example.SessionManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DriverTripsPanel extends JPanel {
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JLabel infoLabel;

    public DriverTripsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("🚆 Мои рейсы"));

        initComponents();
        loadDriverTrips();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshButton = new JButton("Обновить");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setBackground(new Color(0, 102, 204));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadDriverTrips());
        topPanel.add(refreshButton);

        JLabel welcomeLabel = new JLabel("  Здравствуйте, " + SessionManager.getInstance().getCurrentUserFio());
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 13));
        welcomeLabel.setForeground(new Color(0, 102, 204));
        topPanel.add(welcomeLabel);

        add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 13));
        resultTable.setRowHeight(28);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Назначенные рейсы"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadDriverTrips() {
        int driverId = SessionManager.getInstance().getCurrentUserId();

        String sql = "SELECT t.trip_id, r.route_name, s_start.station_name as from_station,\n" +
                "       s_end.station_name as to_station, t.departure_datetime,\n" +
                "       ts.status_name, l.locomotive_number\n" +
                "FROM trips t\n" +
                "JOIN routes r ON t.route_id = r.route_id\n" +
                "JOIN stations s_start ON r.start_station_id = s_start.station_id\n" +
                "JOIN stations s_end ON r.end_station_id = s_end.station_id\n" +
                "JOIN trip_statuses ts ON t.status_id = ts.status_id\n" +
                "JOIN trains tr ON t.train_id = tr.train_id\n" +
                "JOIN locomotives l ON tr.locomotive_id = l.locomotive_id\n" +
                "JOIN locomotive_brigade_assigment lba ON l.locomotive_id = lba.locomotive_id\n" +
                "JOIN brigades b ON lba.brigade_id = b.brigade_id\n" +
                "WHERE b.brigadier_id = " + driverId + "\n" +
                "ORDER BY t.departure_datetime";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Vector<String> columns = new Vector<>();
            columns.add("Маршрут");
            columns.add("Отправление");
            columns.add("Назначение");
            columns.add("Время отправления");
            columns.add("Статус");
            columns.add("Локомотив");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("route_name"));
                row.add(rs.getString("from_station"));
                row.add(rs.getString("to_station"));
                row.add(rs.getTimestamp("departure_datetime") != null ?
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16) : "");
                row.add(rs.getString("status_name"));
                row.add(rs.getString("locomotive_number"));
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            infoLabel.setText("Найдено рейсов: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }
}