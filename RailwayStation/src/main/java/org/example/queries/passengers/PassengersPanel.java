package org.example.queries.passengers;

import org.example.DatabaseManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class PassengersPanel extends JPanel {
    private JComboBox<String> tripCombo;
    private JTextField dateField;
    private JCheckBox internationalCheck;
    private JComboBox<String> luggageCombo;
    private JComboBox<String> genderCombo;
    private JTextField minAgeField;
    private JTextField maxAgeField;
    private JButton searchButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel infoLabel;

    public PassengersPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Пассажиры"));

        initComponents();
        loadTrips();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Параметры отбора"));
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
        paramsPanel.add(new JLabel("Дата отправления:"), gbc);
        gbc.gridx = 1;
        dateField = new JTextField(15);
        dateField.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(dateField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 2;
        internationalCheck = new JCheckBox("Только международные рейсы");
        paramsPanel.add(internationalCheck, gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 3;
        paramsPanel.add(new JLabel("Багаж:"), gbc);
        gbc.gridx = 1;
        luggageCombo = new JComboBox<>(new String[]{"Все", "С багажом", "Без багажа"});
        paramsPanel.add(luggageCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        paramsPanel.add(new JLabel("Пол:"), gbc);
        gbc.gridx = 1;
        genderCombo = new JComboBox<>(new String[]{"Все", "Мужской", "Женский"});
        paramsPanel.add(genderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        paramsPanel.add(new JLabel("Возраст от:"), gbc);
        gbc.gridx = 1;
        minAgeField = new JTextField(5);
        paramsPanel.add(minAgeField, gbc);
        gbc.gridx = 2;
        paramsPanel.add(new JLabel("до:"), gbc);
        gbc.gridx = 3;
        maxAgeField = new JTextField(5);
        paramsPanel.add(maxAgeField, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
        gbc.gridwidth = 4;
        searchButton = new JButton("Выполнить поиск");
        searchButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchButton.setBackground(new Color(0, 153, 76));
        searchButton.setForeground(Color.BLACK);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> executeQuery());
        paramsPanel.add(searchButton, gbc);

        add(paramsPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 13));
        resultTable.setRowHeight(28);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Пассажиры"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        add(infoLabel, BorderLayout.SOUTH);
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

    private void executeQuery() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT DISTINCT p.fio AS \"ФИО\",\n");
        sql.append("       CASE WHEN p.gender = 'm' THEN 'Мужской' ELSE 'Женский' END AS \"Пол\",\n");
        sql.append("       EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday_date)) AS \"Возраст\",\n");
        sql.append("       CASE WHEN l.luggage_id IS NOT NULL THEN 'Да' ELSE 'Нет' END AS \"Багаж\",\n");
        sql.append("       r.route_name AS \"Маршрут\",\n");
        sql.append("       t.departure_datetime AS \"Время отправления\"\n");
        sql.append("FROM tickets tk\n");
        sql.append("JOIN passengers p ON tk.passenger_id = p.passenger_id\n");
        sql.append("JOIN trips t ON tk.trip_id = t.trip_id\n");
        sql.append("JOIN routes r ON t.route_id = r.route_id\n");
        sql.append("JOIN route_categories rc ON r.category_id = rc.category_id\n");
        sql.append("LEFT JOIN luggage l ON p.passenger_id = l.passenger_id\n");
        sql.append("WHERE tk.status = 46\n");

        String selectedTrip = (String) tripCombo.getSelectedItem();
        if (selectedTrip != null && !selectedTrip.equals("Все рейсы") && selectedTrip.contains("|")) {
            int tripId = Integer.parseInt(selectedTrip.split("\\|")[1]);
            sql.append("AND t.trip_id = ").append(tripId).append("\n");
        }

        if (!dateField.getText().trim().isEmpty()) {
            sql.append("AND DATE(t.departure_datetime) = '").append(dateField.getText().trim()).append("'\n");
        }

        if (internationalCheck.isSelected()) {
            sql.append("AND rc.category_name = 'International'\n");
        }

        String luggage = (String) luggageCombo.getSelectedItem();
        if ("С багажом".equals(luggage)) {
            sql.append("AND l.luggage_id IS NOT NULL\n");
        } else if ("Без багажа".equals(luggage)) {
            sql.append("AND l.luggage_id IS NULL\n");
        }

        String gender = (String) genderCombo.getSelectedItem();
        if ("Мужской".equals(gender)) {
            sql.append("AND p.gender = 'm'\n");
        } else if ("Женский".equals(gender)) {
            sql.append("AND p.gender = 'f'\n");
        }

        if (!minAgeField.getText().trim().isEmpty()) {
            sql.append("AND EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday_date)) >= ").append(minAgeField.getText().trim()).append("\n");
        }
        if (!maxAgeField.getText().trim().isEmpty()) {
            sql.append("AND EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birthday_date)) <= ").append(maxAgeField.getText().trim()).append("\n");
        }

        sql.append("ORDER BY p.fio");

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            Vector<String> columns = new Vector<>();
            columns.add("ФИО");
            columns.add("Пол");
            columns.add("Возраст");
            columns.add("Багаж");
            columns.add("Маршрут");
            columns.add("Время отправления");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("ФИО"));
                row.add(rs.getString("Пол"));
                row.add(rs.getInt("Возраст"));
                row.add(rs.getString("Багаж"));
                row.add(rs.getString("Маршрут"));
                row.add(rs.getTimestamp("Время отправления") != null ?
                        rs.getTimestamp("Время отправления").toString().substring(0, 16) : "");
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            infoLabel.setText("Найдено пассажиров: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }
}