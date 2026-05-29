package org.example.queries.tickets;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class TicketSalesPanel extends JPanel {
    private JTextField dateFromField;
    private JTextField dateToField;
    private JComboBox<String> filterTypeCombo;
    private JComboBox<String> routeCombo;
    private JButton searchButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel totalLabel;

    public TicketSalesPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Запрос 9: Анализ продажи билетов"));

        initComponents();
        loadRoutes();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Параметры анализа"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        paramsPanel.add(new JLabel("Период с:"), gbc);
        gbc.gridx = 1;
        dateFromField = new JTextField("2026-10-01", 12);
        paramsPanel.add(dateFromField, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        paramsPanel.add(new JLabel("по:"), gbc);
        gbc.gridx = 3;
        dateToField = new JTextField("2026-10-31", 12);
        paramsPanel.add(dateToField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Группировать по:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        filterTypeCombo = new JComboBox<>(new String[]{
                "Маршрутам",
                "Длительности маршрута",
                "Цене билета",
                "Всем критериям"
        });
        filterTypeCombo.addActionListener(e -> updateVisibleFields());
        paramsPanel.add(filterTypeCombo, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 2;
        routeCombo = new JComboBox<>();
        routeCombo.addItem("Все маршруты");
        gbc.gridx = 1;
        gbc.gridwidth = 3;
        JPanel routePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        routePanel.add(new JLabel("Маршрут:"));
        routePanel.add(routeCombo);
        paramsPanel.add(routePanel, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 4;
        searchButton = new JButton("Выполнить анализ продаж");
        searchButton.setFont(new Font("Arial", Font.BOLD, 14));
        searchButton.setBackground(new Color(0, 153, 76));
        searchButton.setForeground(Color.BLACK);
        searchButton.addActionListener(e -> executeQuery());
        paramsPanel.add(searchButton, gbc);

        add(paramsPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 12));
        resultTable.setRowHeight(25);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Результаты анализа"));
        add(scrollPane, BorderLayout.CENTER);

        totalLabel = new JLabel(" ", SwingConstants.CENTER);
        totalLabel.setFont(new Font("Arial", Font.BOLD, 14));
        totalLabel.setForeground(new Color(0, 102, 204));
        add(totalLabel, BorderLayout.SOUTH);
    }

    private void updateVisibleFields() {
        String selected = (String) filterTypeCombo.getSelectedItem();
        boolean showRoute = "Маршрутам".equals(selected) || "Всем критериям".equals(selected);
        routeCombo.setEnabled(showRoute);
    }

    private void loadRoutes() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT route_id, route_name FROM routes ORDER BY route_name")) {
            while (rs.next()) {
                routeCombo.addItem(rs.getString("route_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeQuery() {
        String dateFrom = dateFromField.getText().trim();
        String dateTo = dateToField.getText().trim();

        if (dateFrom.isEmpty() || dateTo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите период дат!");
            return;
        }

        String filterType = (String) filterTypeCombo.getSelectedItem();

        if ("Маршрутам".equals(filterType)) {
            showByRoute(dateFrom, dateTo);
        } else if ("Длительности маршрута".equals(filterType)) {
            showByDuration(dateFrom, dateTo);
        } else if ("Цене билета".equals(filterType)) {
            showByPrice(dateFrom, dateTo);
        } else {
            showAllCriteria(dateFrom, dateTo);
        }
    }

    private void showByRoute(String dateFrom, String dateTo) {
        StringBuilder sql = new StringBuilder();
        sql.append("WITH daily_sales AS (\n");
        sql.append("    SELECT r.route_id, r.route_name,\n");
        sql.append("           DATE(tk.date_purchase) AS sale_date,\n");
        sql.append("           COUNT(tk.ticket_id) AS tickets_sold\n");
        sql.append("    FROM tickets tk\n");
        sql.append("    JOIN trips t ON tk.trip_id = t.trip_id\n");
        sql.append("    JOIN routes r ON t.route_id = r.route_id\n");
        sql.append("    WHERE tk.date_purchase BETWEEN '").append(dateFrom).append("' AND '").append(dateTo).append("'\n");

        String selectedRoute = (String) routeCombo.getSelectedItem();
        if (selectedRoute != null && !selectedRoute.equals("Все маршруты") && selectedRoute.contains("|")) {
            int routeId = Integer.parseInt(selectedRoute.split("\\|")[1]);
            sql.append("    AND r.route_id = ").append(routeId).append("\n");
        }

        sql.append("    GROUP BY r.route_id, r.route_name, DATE(tk.date_purchase)\n");
        sql.append(")\n");
        sql.append("SELECT route_name AS \"Маршрут\",\n");
        sql.append("       ROUND(AVG(tickets_sold), 1) AS \"Среднее кол-во билетов в день\",\n");
        sql.append("       SUM(tickets_sold) AS \"Всего продано билетов\",\n");
        sql.append("       COUNT(*) AS \"Дней с продажами\"\n");
        sql.append("FROM daily_sales\n");
        sql.append("GROUP BY route_name\n");
        sql.append("ORDER BY \"Среднее кол-во билетов в день\" DESC");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            Vector<String> columns = new Vector<>();
            columns.add("Маршрут");
            columns.add("Среднее кол-во билетов в день");
            columns.add("Всего продано билетов");
            columns.add("Дней с продажами");

            Vector<Vector<Object>> data = new Vector<>();
            int totalTickets = 0;

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("Маршрут"));
                row.add(rs.getDouble("Среднее кол-во билетов в день"));
                row.add(rs.getInt("Всего продано билетов"));
                row.add(rs.getInt("Дней с продажами"));
                data.add(row);
                totalTickets += rs.getInt("Всего продано билетов");
            }

            tableModel.setDataVector(data, columns);
            totalLabel.setText("За период с " + dateFrom + " по " + dateTo + " продано всего билетов: " + totalTickets);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showByDuration(String dateFrom, String dateTo) {
        String sql = "WITH daily_sales AS (\n" +
                "    SELECT t.route_id,\n" +
                "           DATE(tk.date_purchase) AS sale_date,\n" +
                "           COUNT(tk.ticket_id) AS tickets_per_day\n" +
                "    FROM tickets tk\n" +
                "    JOIN trips t ON tk.trip_id = t.trip_id\n" +
                "    WHERE tk.date_purchase BETWEEN '" + dateFrom + "' AND '" + dateTo + "'\n" +
                "    GROUP BY t.route_id, DATE(tk.date_purchase)\n" +
                "),\n" +
                "route_durations AS (\n" +
                "    SELECT route_id,\n" +
                "           SUM(EXTRACT(EPOCH FROM next_station_time))/3600 AS duration_hours\n" +
                "    FROM route_stations\n" +
                "    GROUP BY route_id\n" +
                ")\n" +
                "SELECT r.route_name AS \"Маршрут\",\n" +
                "       ROUND(rd.duration_hours, 1) AS \"Длительность (часов)\",\n" +
                "       ROUND(AVG(ds.tickets_per_day), 1) AS \"Среднее кол-во билетов в день\",\n" +
                "       SUM(ds.tickets_per_day) AS \"Всего продано билетов\"\n" +
                "FROM routes r\n" +
                "JOIN route_durations rd ON r.route_id = rd.route_id\n" +
                "LEFT JOIN daily_sales ds ON r.route_id = ds.route_id\n" +
                "GROUP BY r.route_name, rd.duration_hours\n" +
                "ORDER BY rd.duration_hours";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Vector<String> columns = new Vector<>();
            columns.add("Маршрут");
            columns.add("Длительность (часов)");
            columns.add("Среднее кол-во билетов в день");
            columns.add("Всего продано билетов");

            Vector<Vector<Object>> data = new Vector<>();
            int totalTickets = 0;

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("Маршрут"));
                row.add(rs.getDouble("Длительность (часов)"));
                row.add(rs.getDouble("Среднее кол-во билетов в день"));
                row.add(rs.getInt("Всего продано билетов"));
                data.add(row);
                totalTickets += rs.getInt("Всего продано билетов");
            }

            tableModel.setDataVector(data, columns);
            totalLabel.setText("За период с " + dateFrom + " по " + dateTo + " продано всего билетов: " + totalTickets);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showByPrice(String dateFrom, String dateTo) {
        String sql = "SELECT tk.ticket_price AS \"Цена билета (₽)\",\n" +
                "       COUNT(*) AS \"Количество проданных билетов\",\n" +
                "       ROUND(AVG(COUNT(*)) OVER(), 1) AS \"Среднее количество в день\"\n" +
                "FROM tickets tk\n" +
                "WHERE tk.date_purchase BETWEEN '" + dateFrom + "' AND '" + dateTo + "'\n" +
                "GROUP BY tk.ticket_price\n" +
                "ORDER BY tk.ticket_price";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Vector<String> columns = new Vector<>();
            columns.add("Цена билета ");
            columns.add("Количество проданных билетов");
            columns.add("Среднее количество в день");

            Vector<Vector<Object>> data = new Vector<>();
            int totalTickets = 0;

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("Цена билета "));
                row.add(rs.getInt("Количество проданных билетов"));
                row.add(rs.getDouble("Среднее количество в день"));
                data.add(row);
                totalTickets += rs.getInt("Количество проданных билетов");
            }

            tableModel.setDataVector(data, columns);
            totalLabel.setText("За период с " + dateFrom + " по " + dateTo + " продано всего билетов: " + totalTickets);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showAllCriteria(String dateFrom, String dateTo) {
        String sql = "WITH daily_sales AS (\n" +
                "    SELECT t.route_id, tk.ticket_price,\n" +
                "           DATE(tk.date_purchase) AS sale_date,\n" +
                "           COUNT(tk.ticket_id) AS tickets_sold\n" +
                "    FROM tickets tk\n" +
                "    JOIN trips t ON tk.trip_id = t.trip_id\n" +
                "    WHERE tk.date_purchase BETWEEN '" + dateFrom + "' AND '" + dateTo + "'\n" +
                "    GROUP BY t.route_id, tk.ticket_price, DATE(tk.date_purchase)\n" +
                "),\n" +
                "route_durations AS (\n" +
                "    SELECT route_id,\n" +
                "           SUM(EXTRACT(EPOCH FROM next_station_time))/3600 AS duration_hours\n" +
                "    FROM route_stations\n" +
                "    GROUP BY route_id\n" +
                ")\n" +
                "SELECT r.route_name AS \"Маршрут\",\n" +
                "       ROUND(rd.duration_hours, 1) AS \"Длительность (ч)\",\n" +
                "       ds.ticket_price AS \"Цена билета (₽)\",\n" +
                "       ROUND(AVG(ds.tickets_sold), 1) AS \"Среднее в день\",\n" +
                "       SUM(ds.tickets_sold) AS \"Всего продано\"\n" +
                "FROM routes r\n" +
                "JOIN route_durations rd ON r.route_id = rd.route_id\n" +
                "LEFT JOIN daily_sales ds ON r.route_id = ds.route_id\n" +
                "GROUP BY r.route_name, rd.duration_hours, ds.ticket_price\n" +
                "ORDER BY r.route_name, ds.ticket_price";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Vector<String> columns = new Vector<>();
            columns.add("Маршрут");
            columns.add("Длительность (ч)");
            columns.add("Цена билета");
            columns.add("Среднее кол-во в день");
            columns.add("Всего продано");

            Vector<Vector<Object>> data = new Vector<>();
            int totalTickets = 0;

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("Маршрут"));
                row.add(rs.getDouble("Длительность (ч)"));
                row.add(rs.getInt("Цена билета"));
                row.add(rs.getDouble("Среднее в день"));
                row.add(rs.getInt("Всего продано"));
                data.add(row);
                totalTickets += rs.getInt("Всего продано");
            }

            tableModel.setDataVector(data, columns);
            totalLabel.setText("За период с " + dateFrom + " по " + dateTo + " продано всего билетов: " + totalTickets);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}