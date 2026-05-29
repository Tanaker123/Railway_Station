package org.example.queries.trips;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class TrainRoutePanel extends JPanel {
    private JComboBox<String> routeCombo;
    private JButton showRoutesButton;
    private JButton searchButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;

    public TrainRoutePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Запрос 6: Поезда на маршруте"));

        initComponents();
        loadRoutes();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        paramsPanel.setBorder(new TitledBorder("Параметры"));

        paramsPanel.add(new JLabel("Маршрут:"));
        routeCombo = new JComboBox<>();
        routeCombo.setPreferredSize(new Dimension(300, 25));
        paramsPanel.add(routeCombo);

        showRoutesButton = new JButton("Список маршрутов");
        showRoutesButton.addActionListener(e -> showRoutesList());
        paramsPanel.add(showRoutesButton);

        searchButton = new JButton("Показать поезда");
        searchButton.addActionListener(e -> executeQuery());
        paramsPanel.add(searchButton);

        add(paramsPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 12));
        resultTable.setRowHeight(25);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Поезда на маршруте"));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadRoutes() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT route_id, route_name, s_start.station_name as start_station, " +
                             "s_end.station_name as end_station " +
                             "FROM routes r " +
                             "JOIN stations s_start ON r.start_station_id = s_start.station_id " +
                             "JOIN stations s_end ON r.end_station_id = s_end.station_id " +
                             "ORDER BY route_name")) {
            while (rs.next()) {
                String display = rs.getString("route_name") + " (" +
                        rs.getString("start_station") + " → " +
                        rs.getString("end_station");
                routeCombo.addItem(display);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showRoutesList() {
        StringBuilder sb = new StringBuilder();
        sb.append("ВСЕ МАРШРУТЫ:\n\n");
        for (int i = 0; i < routeCombo.getItemCount(); i++) {
            String item = routeCombo.getItemAt(i);
            String[] parts = item.split("\\|");
            sb.append("• ").append(parts[0]).append("\n");
        }
        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Список маршрутов", JOptionPane.INFORMATION_MESSAGE);
    }

    private void executeQuery() {
        String selected = (String) routeCombo.getSelectedItem();
        if (selected == null || !selected.contains("|")) return;

        int routeId = Integer.parseInt(selected.split("\\|")[1]);

        String sql = "SELECT DISTINCT tr.train_id, l.locomotive_number, tt.type_name as train_type, " +
                "COUNT(DISTINCT tk.ticket_id) as tickets_sold " +
                "FROM trains tr " +
                "JOIN locomotives l ON tr.locomotive_id = l.locomotive_id " +
                "JOIN train_types tt ON tr.train_type_id = tt.train_type_id " +
                "JOIN trips t ON tr.train_id = t.train_id " +
                "LEFT JOIN tickets tk ON t.trip_id = tk.trip_id " +
                "WHERE t.route_id = " + routeId + " " +
                "GROUP BY tr.train_id, l.locomotive_number, tt.type_name " +
                "ORDER BY tr.train_id";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Vector<String> columns = new Vector<>();
            columns.add("ID поезда");
            columns.add("Номер локомотива");
            columns.add("Тип поезда");
            columns.add("Продано билетов");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("train_id"));
                row.add(rs.getString("locomotive_number"));
                row.add(rs.getString("train_type"));
                row.add(rs.getInt("tickets_sold"));
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            JOptionPane.showMessageDialog(this, "Найдено поездов: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}