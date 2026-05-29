package org.example.queries.employee;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DriversMedicalPanel extends JPanel {
    private JComboBox<Integer> yearCombo;
    private JComboBox<String> medicalStatusCombo;
    private JComboBox<String> genderCombo;
    private JTextField minAgeField;
    private JTextField maxAgeField;
    private JTextField minSalaryField;
    private JButton searchButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;

    public DriversMedicalPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Запрос 3: Водители и медосмотр"));

        initComponents();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Параметры отбора"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        paramsPanel.add(new JLabel("Год медосмотра:"), gbc);
        gbc.gridx = 1;
        yearCombo = new JComboBox<>();
        for (int y = 2023; y <= 2027; y++) {
            yearCombo.addItem(y);
        }
        yearCombo.setSelectedItem(2026);
        paramsPanel.add(yearCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        paramsPanel.add(new JLabel("Статус:"), gbc);
        gbc.gridx = 3;
        medicalStatusCombo = new JComboBox<>(new String[]{"Все", "Прошедшие", "Не прошедшие"});
        paramsPanel.add(medicalStatusCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Пол:"), gbc);
        gbc.gridx = 1;
        genderCombo = new JComboBox<>(new String[]{"Все", "Мужской", "Женский"});
        paramsPanel.add(genderCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Возраст от:"), gbc);
        gbc.gridx = 3;
        minAgeField = new JTextField(5);
        paramsPanel.add(minAgeField, gbc);
        gbc.gridx = 4;
        paramsPanel.add(new JLabel("до:"), gbc);
        gbc.gridx = 5;
        maxAgeField = new JTextField(5);
        paramsPanel.add(maxAgeField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        paramsPanel.add(new JLabel("Зарплата от:"), gbc);
        gbc.gridx = 1;
        minSalaryField = new JTextField(10);
        paramsPanel.add(minSalaryField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 6;
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
        int year = (Integer) yearCombo.getSelectedItem();
        String status = (String) medicalStatusCombo.getSelectedItem();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.fio AS \"ФИО водителя\", ");
        sql.append("CASE WHEN e.gender = 'm' THEN 'Мужской' ELSE 'Женский' END AS \"Пол\", ");
        sql.append("EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.birthday_date)) AS \"Возраст\", ");
        sql.append("e.salary AS \"Зарплата\", ");
        sql.append("COALESCE(m.date_medical_checkup::text, 'Нет данных') AS \"Дата последнего осмотра\", ");
        sql.append("CASE ");
        sql.append("  WHEN m.date_medical_checkup IS NULL THEN 'Не проходил' ");
        sql.append("  WHEN EXTRACT(YEAR FROM m.date_medical_checkup) = ").append(year).append(" THEN 'Прошел в ").append(year).append("' ");
        sql.append("  ELSE 'Не прошел в ").append(year).append("' ");
        sql.append("END AS \"Статус\" ");
        sql.append("FROM drivers d ");
        sql.append("JOIN employee e ON d.driver_id = e.employee_id ");
        sql.append("LEFT JOIN medical_checkup m ON d.driver_id = m.driver_id ");
        sql.append("WHERE e.dismissial_date IS NULL ");

        if ("Прошедшие".equals(status)) {
            sql.append(" AND EXTRACT(YEAR FROM m.date_medical_checkup) = ").append(year);
        } else if ("Не прошедшие".equals(status)) {
            sql.append(" AND (EXTRACT(YEAR FROM m.date_medical_checkup) != ").append(year);
            sql.append(" OR m.date_medical_checkup IS NULL)");
        }

        String gender = (String) genderCombo.getSelectedItem();
        if ("Мужской".equals(gender)) {
            sql.append(" AND e.gender = 'm'");
        } else if ("Женский".equals(gender)) {
            sql.append(" AND e.gender = 'f'");
        }

        if (!minAgeField.getText().trim().isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.birthday_date)) >= ").append(minAgeField.getText().trim());
        }
        if (!maxAgeField.getText().trim().isEmpty()) {
            sql.append(" AND EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.birthday_date)) <= ").append(maxAgeField.getText().trim());
        }

        if (!minSalaryField.getText().trim().isEmpty()) {
            sql.append(" AND e.salary >= ").append(minSalaryField.getText().trim());
        }

        sql.append(" ORDER BY e.fio");

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
                    row.add(rs.getString(i));
                }
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            JOptionPane.showMessageDialog(this, "Найдено водителей: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}