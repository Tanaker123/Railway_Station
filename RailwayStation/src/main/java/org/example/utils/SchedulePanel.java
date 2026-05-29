package org.example.utils;

import org.example.DatabaseConfig;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class SchedulePanel extends JPanel {
    private JComboBox<String> tripCombo;
    private JButton searchButton;
    private JTable scheduleTable;
    private JTable routeDetailsTable;
    private DefaultTableModel scheduleModel;
    private DefaultTableModel routeDetailsModel;
    private JLabel infoLabel;

    public SchedulePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Расписание движения поездов"));

        initComponents();
        loadTrips();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        topPanel.setBorder(new TitledBorder("Выбор рейса"));

        topPanel.add(new JLabel("Выберите рейс:"));
        tripCombo = new JComboBox<>();
        tripCombo.setPreferredSize(new Dimension(500, 30));
        tripCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        tripCombo.addActionListener(e -> loadSchedule());
        topPanel.add(tripCombo);

        searchButton = new JButton("Показать расписание");
        searchButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchButton.setBackground(new Color(0, 102, 204));
        searchButton.setForeground(Color.WHITE);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> loadSchedule());
        topPanel.add(searchButton);

        add(topPanel, BorderLayout.NORTH);

        scheduleModel = new DefaultTableModel();
        scheduleTable = new JTable(scheduleModel);
        scheduleTable.setFont(new Font("Arial", Font.PLAIN, 13));
        scheduleTable.setRowHeight(28);
        scheduleTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        JScrollPane scrollPane1 = new JScrollPane(scheduleTable);
        scrollPane1.setBorder(new TitledBorder("Информация о рейсе"));
        scrollPane1.setPreferredSize(new Dimension(0, 120));

        routeDetailsModel = new DefaultTableModel();
        routeDetailsTable = new JTable(routeDetailsModel);
        routeDetailsTable.setFont(new Font("Arial", Font.PLAIN, 13));
        routeDetailsTable.setRowHeight(28);
        routeDetailsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        JScrollPane scrollPane2 = new JScrollPane(routeDetailsTable);
        scrollPane2.setBorder(new TitledBorder("Маршрут следования (промежуточные станции)"));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scrollPane1, scrollPane2);
        splitPane.setDividerLocation(150);
        add(splitPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadTrips() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT t.trip_id, r.route_name, t.departure_datetime, " +
                             "s_start.station_name as from_station, s_end.station_name as to_station, " +
                             "tr.train_id " +
                             "FROM trips t " +
                             "JOIN routes r ON t.route_id = r.route_id " +
                             "JOIN stations s_start ON r.start_station_id = s_start.station_id " +
                             "JOIN stations s_end ON r.end_station_id = s_end.station_id " +
                             "JOIN trains tr ON t.train_id = tr.train_id " +
                             "WHERE t.departure_datetime > CURRENT_TIMESTAMP " +
                             "ORDER BY t.departure_datetime")) {
            while (rs.next()) {
                int tripId = rs.getInt("trip_id");
                String display = String.format("Поезд №%d | %s → %s | %s | Отправление: %s",
                        rs.getInt("train_id"),
                        rs.getString("from_station"),
                        rs.getString("to_station"),
                        rs.getString("route_name"),
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16));
                tripCombo.addItem(display + "|" + tripId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadSchedule() {
        String selected = (String) tripCombo.getSelectedItem();
        if (selected == null || !selected.contains("|")) {
            return;
        }

        int tripId = Integer.parseInt(selected.split("\\|")[1]);

        try (Connection conn = DatabaseConfig.getConnection()) {
            String tripSql = "SELECT t.trip_id, r.route_name, t.departure_datetime, " +
                    "s_start.station_name as from_station, s_end.station_name as to_station, " +
                    "tr.train_id, l.locomotive_number, ts.status_name " +
                    "FROM trips t " +
                    "JOIN routes r ON t.route_id = r.route_id " +
                    "JOIN stations s_start ON r.start_station_id = s_start.station_id " +
                    "JOIN stations s_end ON r.end_station_id = s_end.station_id " +
                    "JOIN trains tr ON t.train_id = tr.train_id " +
                    "JOIN locomotives l ON tr.locomotive_id = l.locomotive_id " +
                    "JOIN trip_statuses ts ON t.status_id = ts.status_id " +
                    "WHERE t.trip_id = " + tripId;

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(tripSql);

            if (rs.next()) {
                Vector<String> scheduleColumns = new Vector<>();
                scheduleColumns.add("Номер поезда");
                scheduleColumns.add("Маршрут");
                scheduleColumns.add("Локомотив");
                scheduleColumns.add("Время отправления");
                scheduleColumns.add("Статус");

                Vector<Vector<Object>> scheduleData = new Vector<>();
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("train_id"));
                row.add(rs.getString("route_name"));
                row.add(rs.getString("locomotive_number"));
                row.add(rs.getTimestamp("departure_datetime") != null ?
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16) : "");
                row.add(rs.getString("status_name"));
                scheduleData.add(row);

                scheduleModel.setDataVector(scheduleData, scheduleColumns);
            }
            rs.close();

            String routeSql = "SELECT rs.station_order, s.station_name, " +
                    "rs.train_stop, rs.next_station_time " +
                    "FROM route_stations rs " +
                    "JOIN stations s ON rs.station_id = s.station_id " +
                    "WHERE rs.route_id = (SELECT route_id FROM trips WHERE trip_id = " + tripId + ") " +
                    "ORDER BY rs.station_order";

            rs = stmt.executeQuery(routeSql);

            Vector<String> routeColumns = new Vector<>();
            routeColumns.add("Порядок");
            routeColumns.add("Станция");
            routeColumns.add("Время стоянки");
            routeColumns.add("Время в пути до след. станции");

            Vector<Vector<Object>> routeData = new Vector<>();
            while (rs.next()) {
                Vector<Object> routeRow = new Vector<>();
                routeRow.add(rs.getInt("station_order"));
                routeRow.add(rs.getString("station_name"));
                routeRow.add(rs.getTime("train_stop") != null ? rs.getTime("train_stop").toString() : "0");
                routeRow.add(rs.getObject("next_station_time") != null ? rs.getObject("next_station_time").toString() : "");
                routeData.add(routeRow);
            }

            routeDetailsModel.setDataVector(routeData, routeColumns);
            infoLabel.setText("Всего станций в маршруте: " + routeData.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }
}