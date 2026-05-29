package org.example.queries.locomotives;

import org.example.DatabaseManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class LocomotiveStatusPanel extends JPanel {
    private JTextField dateTimeField;
    private JComboBox<String> stationCombo;
    private JButton searchButton;
    private JButton showStationsButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel infoLabel;

    public LocomotiveStatusPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Запрос 4: Статус локомотивов"));

        initComponents();
        loadStations();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Параметры функции"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        paramsPanel.add(new JLabel("Дата и время (ГГГГ-ММ-ДД ЧЧ:ММ:СС):"), gbc);
        gbc.gridx = 1;
        dateTimeField = new JTextField("2026-11-20 12:00:00", 25);
        dateTimeField.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(dateTimeField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Фильтр по станции:"), gbc);
        gbc.gridx = 1;
        stationCombo = new JComboBox<>();
        stationCombo.addItem("Все станции");
        stationCombo.setPreferredSize(new Dimension(300, 30));
        stationCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(stationCombo, gbc);

        gbc.gridx = 2;
        showStationsButton = new JButton("Список станций");
        showStationsButton.setFont(new Font("Arial", Font.BOLD, 12));
        showStationsButton.setFocusPainted(false);
        showStationsButton.addActionListener(e -> showStationsList());
        paramsPanel.add(showStationsButton, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.gridwidth = 3;
        searchButton = new JButton("Выполнить поиск");
        searchButton.setFont(new Font("Arial", Font.BOLD, 14));
        searchButton.setBackground(new Color(0, 153, 76));
        searchButton.setForeground(Color.BLACK);
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> executeFunction());
        paramsPanel.add(searchButton, gbc);

        add(paramsPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel();
        resultTable = new JTable(tableModel);
        resultTable.setFont(new Font("Arial", Font.PLAIN, 13));
        resultTable.setRowHeight(28);
        resultTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        JScrollPane scrollPane = new JScrollPane(resultTable);
        scrollPane.setBorder(new TitledBorder("Результат выполнения"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel("Возвращается статус каждого локомотива на указанный момент времени", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        infoLabel.setForeground(Color.GRAY);
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadStations() {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT station_id, station_name FROM stations ORDER BY station_name")) {
            while (rs.next()) {
                stationCombo.addItem(rs.getString("station_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void showStationsList() {
        StringBuilder sb = new StringBuilder();
        sb.append("Список станций для фильтрации:\n\n");
        sb.append("─────────────────────────────────────────\n");
        for (int i = 1; i < stationCombo.getItemCount(); i++) {
            String item = stationCombo.getItemAt(i);
            String[] parts = item.split("\\|");
            sb.append(String.format("│ %-2d. %-35s │\n", i, parts[0]));
        }
        sb.append("─────────────────────────────────────────\n\n");
        sb.append("Укажите ID станции для фильтрации результатов");

        JTextArea textArea = new JTextArea(sb.toString());
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(450, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Список станций", JOptionPane.INFORMATION_MESSAGE);
    }

    private void executeFunction() {
        String dateTime = dateTimeField.getText().trim();
        if (dateTime.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите дату и время!");
            return;
        }

        String stationParam = (String) stationCombo.getSelectedItem();
        String stationId = "NULL";
        String stationName = "всех станциях";
        if (stationParam != null && !stationParam.equals("Все станции")) {
            String[] parts = stationParam.split("\\|");
            stationId = parts[1];
            stationName = parts[0];
        }

        String sql = "SELECT * FROM get_locomotives_status_by_time('" + dateTime + "', " + stationId + ")";

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            Vector<String> columns = new Vector<>();
            for (int i = 1; i <= columnCount; i++) {
                String label = meta.getColumnLabel(i);
                switch (label) {
                    case "locomotive_id": label = "ID локомотива"; break;
                    case "locomotive_number": label = "Номер локомотива"; break;
                    case "current_status": label = "Текущий статус"; break;
                    case "current_station_id": label = "Код станции"; break;
                    case "current_station_name": label = "Станция"; break;
                    case "last_arrival_time": label = "Время прибытия"; break;
                    case "total_trips_count": label = "Всего маршрутов"; break;
                    case "total_filtered_count": label = "Всего на станции"; break;
                    default: label = label.replace("_", " ").toUpperCase();
                }
                columns.add(label);
            }

            Vector<Vector<Object>> data = new Vector<>();
            int totalOnStation = 0;

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object val = rs.getObject(i);
                    if (val instanceof Timestamp) {
                        row.add(((Timestamp) val).toString());
                    } else {
                        row.add(val != null ? val.toString() : "");
                    }
                }
                data.add(row);
                if (columnCount > 0) {
                    totalOnStation = rs.getInt(columnCount);
                }
            }

            tableModel.setDataVector(data, columns);

            if (!stationParam.equals("Все станции")) {
                infoLabel.setText("На станции " + stationName + " найдено локомотивов: " + totalOnStation);
            } else {
                infoLabel.setText("Всего локомотивов в системе: " + data.size());
            }

            if (data.isEmpty()) {
                infoLabel.setText("На указанное время нет данных о локомотивах");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка выполнения функции: " + e.getMessage());
            e.printStackTrace();
        }
    }
}