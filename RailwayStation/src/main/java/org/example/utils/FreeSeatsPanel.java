package org.example.utils;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class FreeSeatsPanel extends JPanel {
    private JComboBox<String> tripCombo;
    private JButton searchButton;
    private JButton showTripsButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel infoLabel;

    public FreeSeatsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Свободные места"));

        initComponents();
        loadTrips();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        paramsPanel.setBorder(new TitledBorder("Выбор рейса"));

        paramsPanel.add(new JLabel("Выберите рейс:"));
        tripCombo = new JComboBox<>();
        tripCombo.setPreferredSize(new Dimension(500, 30));
        tripCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(tripCombo);

        showTripsButton = new JButton("Список рейсов");
        showTripsButton.setFont(new Font("Arial", Font.BOLD, 12));
        showTripsButton.addActionListener(e -> showTripsList());
        paramsPanel.add(showTripsButton);

        searchButton = new JButton("Показать свободные места");
        searchButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchButton.setBackground(new Color(0, 153, 76));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> loadFreeSeats());
        paramsPanel.add(searchButton);

        add(paramsPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 13));
        resultTable.setRowHeight(28);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Свободные места"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadTrips() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT t.trip_id, r.route_name, t.departure_datetime, " +
                             "s_start.station_name as from_station, s_end.station_name as to_station " +
                             "FROM trips t " +
                             "JOIN routes r ON t.route_id = r.route_id " +
                             "JOIN stations s_start ON r.start_station_id = s_start.station_id " +
                             "JOIN stations s_end ON r.end_station_id = s_end.station_id " +
                             "WHERE t.departure_datetime > CURRENT_TIMESTAMP " +
                             "ORDER BY t.departure_datetime")) {
            while (rs.next()) {
                String display = String.format("%s → %s | %s | Отправление: %s",
                        rs.getString("from_station"),
                        rs.getString("to_station"),
                        rs.getString("route_name"),
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16));
                tripCombo.addItem(display + "|" + rs.getInt("trip_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showTripsList() {
        StringBuilder sb = new StringBuilder();
        sb.append("СПИСОК ДОСТУПНЫХ РЕЙСОВ:\n\n");
        sb.append("────────────────────────────────────────────────────────────────\n");
        for (int i = 0; i < tripCombo.getItemCount(); i++) {
            String item = tripCombo.getItemAt(i);
            String display = item.split("\\|")[0];
            sb.append(String.format("│ %-62s │\n", display.substring(0, Math.min(62, display.length()))));
        }
        sb.append("────────────────────────────────────────────────────────────────");

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(700, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Список рейсов", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadFreeSeats() {
        String selected = (String) tripCombo.getSelectedItem();
        if (selected == null || !selected.contains("|")) {
            JOptionPane.showMessageDialog(this, "Выберите рейс!");
            return;
        }

        String[] parts = selected.split("\\|");
        int tripId;
        try {
            tripId = Integer.parseInt(parts[parts.length - 1]);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: неверный формат ID рейса");
            return;
        }

        String sql = "SELECT * FROM get_free_seats(" + tripId + ")";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Vector<String> columns = new Vector<>();
            columns.add("Номер вагона");
            columns.add("Тип вагона");
            columns.add("Номер места");
            columns.add("Класс места");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("coach_number"));
                row.add(rs.getString("coach_type"));
                row.add(rs.getString("seat_number"));
                row.add(rs.getString("seat_class"));
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            infoLabel.setText("Всего свободных мест: " + data.size());

            if (data.isEmpty()) {
                infoLabel.setText("На выбранный рейс нет свободных мест");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}