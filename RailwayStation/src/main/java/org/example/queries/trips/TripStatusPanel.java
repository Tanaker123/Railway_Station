package org.example.queries.trips;

import org.example.DatabaseManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class TripStatusPanel extends JPanel {
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JComboBox<String> statusFilterCombo;
    private JLabel infoLabel;

    private Map<String, String> statusMapping = new HashMap<>();

    public TripStatusPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Статусы рейсов"));

        initStatusMapping();
        initComponents();
        loadData();
    }

    private void initStatusMapping() {
        statusMapping.put("Все", "");
        statusMapping.put("Запланирован", "Scheduled");
        statusMapping.put("Подтвержден", "Confirmed");
        statusMapping.put("Подготовка", "Preparation");
        statusMapping.put("Посадка", "Boarding");
        statusMapping.put("Отправлен", "Departed");
        statusMapping.put("В пути", "In Transit");
        statusMapping.put("Задержан", "Delayed");
        statusMapping.put("Прибыл", "Arrived");
        statusMapping.put("Завершен", "Completed");
        statusMapping.put("Отменен", "Cancelled");
        statusMapping.put("Перенесен", "Postponed");
    }

    private void initComponents() {
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        filterPanel.setBorder(new TitledBorder("Фильтрация"));

        filterPanel.add(new JLabel("Статус рейса:"));
        String[] statuses = {"Все", "Запланирован", "Подтвержден", "Подготовка",
                "Посадка", "Отправлен", "В пути", "Задержан",
                "Прибыл", "Завершен", "Отменен", "Перенесен"};
        statusFilterCombo = new JComboBox<>(statuses);
        statusFilterCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        statusFilterCombo.addActionListener(e -> loadData());
        filterPanel.add(statusFilterCombo);

        refreshButton = new JButton("Обновить");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setBackground(new Color(0, 102, 204));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadData());
        filterPanel.add(refreshButton);

        add(filterPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 13));
        resultTable.setRowHeight(28);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));

        resultTable.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected && tableModel.getRowCount() > row) {
                    String status = (String) tableModel.getValueAt(row, 4);
                    if (status != null) {
                        if (status.equals("Задержан")) {
                            c.setBackground(new Color(255, 200, 200));
                        } else if (status.equals("Отменен")) {
                            c.setBackground(new Color(200, 200, 200));
                        } else if (status.equals("Отправлен") || status.equals("В пути")) {
                            c.setBackground(new Color(200, 255, 200));
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                    }
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Статусы рейсов"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadData() {
        String selectedStatus = (String) statusFilterCombo.getSelectedItem();
        String englishStatus = statusMapping.getOrDefault(selectedStatus, "");

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.trip_id, r.route_name, s_start.station_name as from_station,\n");
        sql.append("       s_end.station_name as to_station, ts.status_name, t.departure_datetime\n");
        sql.append("FROM trips t\n");
        sql.append("JOIN routes r ON t.route_id = r.route_id\n");
        sql.append("JOIN stations s_start ON r.start_station_id = s_start.station_id\n");
        sql.append("JOIN stations s_end ON r.end_station_id = s_end.station_id\n");
        sql.append("JOIN trip_statuses ts ON t.status_id = ts.status_id\n");

        if (!englishStatus.isEmpty()) {
            sql.append("WHERE ts.status_name = '").append(englishStatus).append("'\n");
        }

        sql.append("ORDER BY t.departure_datetime");

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            Vector<String> columns = new Vector<>();
            columns.add("Маршрут");
            columns.add("Отправление");
            columns.add("Назначение");
            columns.add("Время отправления");
            columns.add("Статус");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("route_name"));
                row.add(rs.getString("from_station"));
                row.add(rs.getString("to_station"));
                row.add(rs.getTimestamp("departure_datetime") != null ?
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16) : "");

                String engStatus = rs.getString("status_name");
                String rusStatus = getRussianStatus(engStatus);
                row.add(rusStatus);
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            infoLabel.setText("📊 Найдено рейсов: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }

    private String getRussianStatus(String engStatus) {
        for (Map.Entry<String, String> entry : statusMapping.entrySet()) {
            if (entry.getValue().equals(engStatus)) {
                return entry.getKey();
            }
        }
        return engStatus;
    }
}