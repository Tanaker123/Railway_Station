package org.example.queries.routes;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class RouteCategoryPanel extends JPanel {
    private JComboBox<String> categoryCombo;
    private JComboBox<String> directionCombo;
    private JButton searchButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;

    public RouteCategoryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Запрос 10: Маршруты по категориям"));

        initComponents();
        loadCategories();
        loadDirections();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        paramsPanel.setBorder(new TitledBorder("Параметры"));

        paramsPanel.add(new JLabel("Категория маршрута:"));
        categoryCombo = new JComboBox<>();
        categoryCombo.setPreferredSize(new Dimension(150, 25));
        paramsPanel.add(categoryCombo);

        paramsPanel.add(new JLabel("Направление (откуда → куда):"));
        directionCombo = new JComboBox<>();
        directionCombo.setPreferredSize(new Dimension(250, 25));
        paramsPanel.add(directionCombo);

        searchButton = new JButton("Выполнить поиск");
        searchButton.addActionListener(e -> executeQuery());
        paramsPanel.add(searchButton);

        add(paramsPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 12));
        resultTable.setRowHeight(25);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 14));
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Маршруты"));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void loadCategories() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT category_id, category_name FROM route_categories ORDER BY category_name")) {
            while (rs.next()) {
                categoryCombo.addItem(rs.getString("category_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadDirections() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT DISTINCT s_start.station_name as from_station, s_end.station_name as to_station " +
                             "FROM routes r " +
                             "JOIN stations s_start ON r.start_station_id = s_start.station_id " +
                             "JOIN stations s_end ON r.end_station_id = s_end.station_id " +
                             "ORDER BY s_start.station_name")) {
            directionCombo.addItem("Все направления");
            while (rs.next()) {
                directionCombo.addItem(rs.getString("from_station") + " → " + rs.getString("to_station"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeQuery() {
        String selectedCategory = (String) categoryCombo.getSelectedItem();
        String selectedDirection = (String) directionCombo.getSelectedItem();

        if (selectedCategory == null) return;

        int categoryId = Integer.parseInt(selectedCategory.split("\\|")[1]);

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT r.route_name AS \"Маршрут\",\n");
        sql.append("       s_start.station_name AS \"Отправление\",\n");
        sql.append("       s_end.station_name AS \"Назначение\",\n");
        sql.append("       rc.category_name AS \"Категория\"\n");
        sql.append("FROM routes r\n");
        sql.append("JOIN stations s_start ON r.start_station_id = s_start.station_id\n");
        sql.append("JOIN stations s_end ON r.end_station_id = s_end.station_id\n");
        sql.append("JOIN route_categories rc ON r.category_id = rc.category_id\n");
        sql.append("WHERE r.category_id = ").append(categoryId).append("\n");

        if (selectedDirection != null && !selectedDirection.equals("Все направления")) {
            String[] parts = selectedDirection.split(" → ");
            sql.append("AND s_start.station_name = '").append(parts[0]).append("'\n");
            sql.append("AND s_end.station_name = '").append(parts[1]).append("'\n");
        }

        sql.append("ORDER BY r.route_name");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            Vector<String> columns = new Vector<>();
            columns.add("Маршрут");
            columns.add("Отправление");
            columns.add("Назначение");
            columns.add("Категория");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("Маршрут"));
                row.add(rs.getString("Отправление"));
                row.add(rs.getString("Назначение"));
                row.add(rs.getString("Категория"));
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            JOptionPane.showMessageDialog(this, "Найдено маршрутов: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}