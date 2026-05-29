package org.example.queries.locomotives;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class LocomotiveRepairPanel extends JPanel {
    private JTextField plannedDateFromField;
    private JTextField plannedDateToField;
    private JTextField repairStartFromField;
    private JTextField repairStartToField;
    private JTextField minRepairsCountField;
    private JTextField minTripsField;
    private JButton searchButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;

    public LocomotiveRepairPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Запрос 5: Локомотивы и ремонт"));

        initComponents();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Параметры отбора"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        paramsPanel.add(new JLabel("Плановый осмотр от:"), gbc);
        gbc.gridx = 1;
        plannedDateFromField = new JTextField("2026-01-01", 12);
        paramsPanel.add(plannedDateFromField, gbc);
        gbc.gridx = 2;
        paramsPanel.add(new JLabel("до:"), gbc);
        gbc.gridx = 3;
        plannedDateToField = new JTextField("2026-12-31", 12);
        paramsPanel.add(plannedDateToField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Ремонт от:"), gbc);
        gbc.gridx = 1;
        repairStartFromField = new JTextField("", 12);
        paramsPanel.add(repairStartFromField, gbc);
        gbc.gridx = 2;
        paramsPanel.add(new JLabel("до:"), gbc);
        gbc.gridx = 3;
        repairStartToField = new JTextField("", 12);
        paramsPanel.add(repairStartToField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        paramsPanel.add(new JLabel("Ремонтировался не менее X раз:"), gbc);
        gbc.gridx = 1;
        minRepairsCountField = new JTextField("", 5);
        paramsPanel.add(minRepairsCountField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        paramsPanel.add(new JLabel("Совершил рейсов до ремонта не менее:"), gbc);
        gbc.gridx = 1;
        minTripsField = new JTextField("", 5);
        paramsPanel.add(minTripsField, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        gbc.gridwidth = 4;
        searchButton = new JButton("Выполнить поиск");
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
        scrollPane.setBorder(new TitledBorder("Результаты"));
        add(scrollPane, BorderLayout.CENTER);
    }

    private void executeQuery() {
        StringBuilder sql = new StringBuilder();
        sql.append("WITH locomotive_stats AS (\n");
        sql.append("    SELECT l.locomotive_id, l.locomotive_number,\n");
        sql.append("           COUNT(DISTINCT lm.maintenance_id) as repairs_count,\n");
        sql.append("           COUNT(DISTINCT t.trip_id) as total_trips,\n");
        sql.append("           MIN(ms.planned_date) as first_planned_date,\n");
        sql.append("           MIN(lm.start_date) as first_repair_date\n");
        sql.append("    FROM locomotives l\n");
        sql.append("    LEFT JOIN maintenance_schedule ms ON l.locomotive_id = ms.locomotive_id\n");
        sql.append("    LEFT JOIN locomotive_maintenance lm ON ms.schedule_id = lm.schedule_id\n");
        sql.append("    LEFT JOIN trains tr ON l.locomotive_id = tr.locomotive_id\n");
        sql.append("    LEFT JOIN trips t ON tr.train_id = t.train_id\n");
        sql.append("    GROUP BY l.locomotive_id, l.locomotive_number\n");
        sql.append(")\n");
        sql.append("SELECT l.locomotive_number AS \"Номер локомотива\",\n");
        sql.append("       ls.total_trips AS \"Всего рейсов\",\n");
        sql.append("       ls.repairs_count AS \"Количество ремонтов\",\n");
        sql.append("       ls.first_planned_date AS \"Дата первого планового осмотра\",\n");
        sql.append("       ls.first_repair_date AS \"Дата первого ремонта\"\n");
        sql.append("FROM locomotives l\n");
        sql.append("JOIN locomotive_stats ls ON l.locomotive_id = ls.locomotive_id\n");
        sql.append("WHERE 1=1\n");

        if (!plannedDateFromField.getText().trim().isEmpty() && !plannedDateToField.getText().trim().isEmpty()) {
            sql.append("AND (ls.first_planned_date BETWEEN '").append(plannedDateFromField.getText().trim())
                    .append("' AND '").append(plannedDateToField.getText().trim()).append("' OR ls.first_planned_date IS NULL)\n");
        }

        if (!repairStartFromField.getText().trim().isEmpty() && !repairStartToField.getText().trim().isEmpty()) {
            sql.append("AND (ls.first_repair_date BETWEEN '").append(repairStartFromField.getText().trim())
                    .append("' AND '").append(repairStartToField.getText().trim()).append("' OR ls.first_repair_date IS NULL)\n");
        }

        if (!minRepairsCountField.getText().trim().isEmpty()) {
            sql.append("AND ls.repairs_count >= ").append(minRepairsCountField.getText().trim()).append("\n");
        }

        if (!minTripsField.getText().trim().isEmpty()) {
            sql.append("AND ls.total_trips >= ").append(minTripsField.getText().trim()).append("\n");
        }

        sql.append("ORDER BY l.locomotive_number");

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            Vector<String> columns = new Vector<>();
            for (int i = 1; i <= columnCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    Object val = rs.getObject(i);
                    row.add(val != null ? val.toString() : "Нет данных");
                }
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            JOptionPane.showMessageDialog(this, "Найдено локомотивов: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}