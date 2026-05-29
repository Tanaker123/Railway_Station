package org.example.queries.tickets;

import org.example.DatabaseManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.*;

public class RefundedTicketsPanel extends JPanel {
    private JComboBox<String> tripCombo;
    private JComboBox<String> routeCombo;
    private JButton searchButton;
    private JLabel resultLabel;

    public RefundedTicketsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Возвращенные билеты"));

        initComponents();
        loadTrips();
        loadRoutes();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Параметры"));
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

        gbc.gridx = 0; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Маршрут:"), gbc);
        gbc.gridx = 1;
        routeCombo = new JComboBox<>();
        routeCombo.addItem("Все маршруты");
        routeCombo.setPreferredSize(new Dimension(400, 30));
        routeCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(routeCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        searchButton = new JButton("Подсчитать возвращенные билеты");
        searchButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchButton.setBackground(new Color(0, 153, 76));
        searchButton.setForeground(Color.BLACK);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> executeQuery());
        paramsPanel.add(searchButton, gbc);

        add(paramsPanel, BorderLayout.NORTH);

        JPanel resultPanel = new JPanel(new GridBagLayout());
        resultPanel.setBorder(new TitledBorder("Результат"));
        resultLabel = new JLabel("Укажите параметры и нажмите 'Подсчитать'");
        resultLabel.setFont(new Font("Arial", Font.BOLD, 18));
        resultLabel.setForeground(new Color(0, 102, 204));
        resultPanel.add(resultLabel);
        add(resultPanel, BorderLayout.CENTER);
    }

    private void loadTrips() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT t.trip_id, r.route_name, " +
                             "s_start.station_name as from_station, s_end.station_name as to_station " +
                             "FROM trips t " +
                             "JOIN routes r ON t.route_id = r.route_id " +
                             "JOIN stations s_start ON r.start_station_id = s_start.station_id " +
                             "JOIN stations s_end ON r.end_station_id = s_end.station_id " +
                             "ORDER BY t.departure_datetime")) {
            tripCombo.addItem("Все рейсы");
            while (rs.next()) {
                String display = String.format("%s → %s | %s",
                        rs.getString("from_station"),
                        rs.getString("to_station"),
                        rs.getString("route_name"));
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
                     "SELECT route_name FROM routes ORDER BY route_name")) {
            while (rs.next()) {
                routeCombo.addItem(rs.getString("route_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeQuery() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT COUNT(*) AS refunded_count\n");
        sql.append("FROM tickets tk\n");
        sql.append("JOIN ticket_refund tr ON tk.ticket_id = tr.ticket_id\n");
        sql.append("JOIN trips t ON tk.trip_id = t.trip_id\n");
        sql.append("WHERE 1=1\n");

        String selectedTrip = (String) tripCombo.getSelectedItem();
        if (selectedTrip != null && !selectedTrip.equals("Все рейсы") && selectedTrip.contains("|")) {
            int tripId = Integer.parseInt(selectedTrip.split("\\|")[1]);
            sql.append("AND t.trip_id = ").append(tripId).append("\n");
        }

        String selectedRoute = (String) routeCombo.getSelectedItem();
        if (selectedRoute != null && !selectedRoute.equals("Все маршруты")) {
            sql.append("AND t.route_id = (SELECT route_id FROM routes WHERE route_name = '")
                    .append(selectedRoute).append("')\n");
        }

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            if (rs.next()) {
                int count = rs.getInt("refunded_count");
                String countText = String.format("%,d", count);
                resultLabel.setText("Количество возвращенных билетов: " + countText);
                resultLabel.setFont(new Font("Arial", Font.BOLD, 20));
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }
}