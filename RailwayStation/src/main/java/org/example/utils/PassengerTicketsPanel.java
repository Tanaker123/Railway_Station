package org.example.utils;

import org.example.DatabaseConfig;
import org.example.SessionManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class PassengerTicketsPanel extends JPanel {
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JLabel infoLabel;

    public PassengerTicketsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Мои билеты"));

        initComponents();
        loadMyTickets();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshButton = new JButton("Обновить");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setBackground(new Color(0, 102, 204));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadMyTickets());
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
        scrollPane.setBorder(new TitledBorder("Мои билеты"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadMyTickets() {
        int passengerId = SessionManager.getInstance().getCurrentUserId();

        String sql = "SELECT tk.ticket_id, r.route_name, s_start.station_name as from_station,\n" +
                "       s_end.station_name as to_station, t.departure_datetime,\n" +
                "       tk.ticket_price, ts.description as status\n" +
                "FROM tickets tk\n" +
                "JOIN trips t ON tk.trip_id = t.trip_id\n" +
                "JOIN routes r ON t.route_id = r.route_id\n" +
                "JOIN stations s_start ON tk.start_station_id = s_start.station_id\n" +
                "JOIN stations s_end ON tk.end_station_id = s_end.station_id\n" +
                "JOIN ticket_statuses ts ON tk.status = ts.status_id\n" +
                "WHERE tk.passenger_id = " + passengerId + "\n" +
                "ORDER BY t.departure_datetime DESC";

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Vector<String> columns = new Vector<>();
            columns.add("№ билета");
            columns.add("Маршрут");
            columns.add("Отправление");
            columns.add("Назначение");
            columns.add("Время отправления");
            columns.add("Цена (₽)");
            columns.add("Статус");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("ticket_id"));
                row.add(rs.getString("route_name"));
                row.add(rs.getString("from_station"));
                row.add(rs.getString("to_station"));
                row.add(rs.getTimestamp("departure_datetime") != null ?
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16) : "");
                row.add(rs.getInt("ticket_price"));
                row.add(rs.getString("status"));
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            infoLabel.setText("Всего ваших билетов: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }
}