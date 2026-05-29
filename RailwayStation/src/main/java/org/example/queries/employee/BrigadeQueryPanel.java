package org.example.queries.employee;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;

public class BrigadeQueryPanel extends JPanel {
    private JComboBox<String> brigadeCombo;
    private JComboBox<String> departmentCombo;
    private JComboBox<String> locomotiveCombo;
    private JComboBox<String> genderCombo;
    private JTextField minAgeField;
    private JTextField maxAgeField;
    private JTextField minSalaryField;
    private JButton searchButton;
    private JButton refreshBrigadesButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;

    public BrigadeQueryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Запрос 2: Работники в бригаде"));

        initComponents();
        loadBrigades();
        loadDepartments();
        loadLocomotives();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Параметры отбора"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        paramsPanel.add(new JLabel("Бригада:"), gbc);
        gbc.gridx = 1;
        brigadeCombo = new JComboBox<>();
        brigadeCombo.setPreferredSize(new Dimension(200, 25));
        paramsPanel.add(brigadeCombo, gbc);

        gbc.gridx = 2;
        refreshBrigadesButton = new JButton("Обновить список бригад");
        refreshBrigadesButton.addActionListener(e -> loadBrigades());
        paramsPanel.add(refreshBrigadesButton, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Отдел:"), gbc);
        gbc.gridx = 1;
        departmentCombo = new JComboBox<>();
        departmentCombo.addItem("Все отделы");
        paramsPanel.add(departmentCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        paramsPanel.add(new JLabel("Обслуживают локомотив:"), gbc);
        gbc.gridx = 1;
        locomotiveCombo = new JComboBox<>();
        locomotiveCombo.addItem("Все");
        paramsPanel.add(locomotiveCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        paramsPanel.add(new JLabel("Пол:"), gbc);
        gbc.gridx = 1;
        genderCombo = new JComboBox<>(new String[]{"Все", "Мужской", "Женский"});
        paramsPanel.add(genderCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        paramsPanel.add(new JLabel("Возраст от:"), gbc);
        gbc.gridx = 1;
        minAgeField = new JTextField(5);
        paramsPanel.add(minAgeField, gbc);
        gbc.gridx = 2;
        paramsPanel.add(new JLabel("до:"), gbc);
        gbc.gridx = 3;
        maxAgeField = new JTextField(5);
        paramsPanel.add(maxAgeField, gbc);

        gbc.gridx = 0; gbc.gridy = 5;
        paramsPanel.add(new JLabel("Зарплата от:"), gbc);
        gbc.gridx = 1;
        minSalaryField = new JTextField(10);
        paramsPanel.add(minSalaryField, gbc);

        gbc.gridx = 0; gbc.gridy = 6;
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

    private void loadBrigades() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT b.brigade_id, b.brigade_number, bt.type_name, d.department_name " +
                             "FROM brigades b " +
                             "JOIN brigade_types bt ON b.brigade_type = bt.type_id " +
                             "JOIN departments d ON b.department_id = d.department_id " +
                             "ORDER BY b.brigade_number")) {

            brigadeCombo.removeAllItems();
            while (rs.next()) {
                String display = "Бригада №" + rs.getInt("brigade_number") +
                        " (" + rs.getString("type_name") + ") - " +
                        rs.getString("department_name");
                brigadeCombo.addItem(display + "|" + rs.getInt("brigade_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadDepartments() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT department_name FROM departments ORDER BY department_name")) {
            while (rs.next()) {
                departmentCombo.addItem(rs.getString("department_name"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadLocomotives() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT locomotive_number FROM locomotives ORDER BY locomotive_number")) {
            while (rs.next()) {
                locomotiveCombo.addItem(rs.getString("locomotive_number"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeQuery() {
        String selectedBrigade = (String) brigadeCombo.getSelectedItem();
        if (selectedBrigade == null || !selectedBrigade.contains("|")) {
            JOptionPane.showMessageDialog(this, "Выберите бригаду!");
            return;
        }

        String brigadeId = selectedBrigade.split("\\|")[1];

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.fio AS \"ФИО\", ");
        sql.append("CASE WHEN e.gender = 'm' THEN 'Мужской' ELSE 'Женский' END AS \"Пол\", ");
        sql.append("EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.birthday_date)) AS \"Возраст\", ");
        sql.append("e.salary AS \"Зарплата\", ");
        sql.append("p.profession_name AS \"Профессия\", ");
        sql.append("d.department_name AS \"Отдел\" ");
        sql.append("FROM employee e ");
        sql.append("JOIN profession_types p ON e.profession_type = p.type_id ");
        sql.append("JOIN brigades b ON e.brigade_id = b.brigade_id ");
        sql.append("JOIN departments d ON b.department_id = d.department_id ");
        sql.append("WHERE e.brigade_id = ").append(brigadeId);
        sql.append(" AND e.dismissial_date IS NULL");

        String department = (String) departmentCombo.getSelectedItem();
        if (department != null && !department.equals("Все отделы")) {
            sql.append(" AND d.department_name = '").append(department).append("'");
        }

        String locomotive = (String) locomotiveCombo.getSelectedItem();
        if (locomotive != null && !locomotive.equals("Все")) {
            sql.append(" AND EXISTS (SELECT 1 FROM locomotive_brigade_assigment lba ");
            sql.append(" JOIN locomotives l ON lba.locomotive_id = l.locomotive_id ");
            sql.append(" WHERE lba.brigade_id = b.brigade_id AND l.locomotive_number = '").append(locomotive).append("')");
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
            JOptionPane.showMessageDialog(this, "Найдено сотрудников в бригаде: " + data.size());

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}