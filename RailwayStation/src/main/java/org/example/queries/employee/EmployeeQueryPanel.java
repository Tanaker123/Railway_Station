package org.example.queries.employee;

import org.example.DatabaseManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

public class EmployeeQueryPanel extends JPanel {
    private JTextField minSalaryField;
    private JComboBox<String> genderCombo;
    private JComboBox<String> departmentCombo;
    private JCheckBox onlyHeadsCheck;
    private JTextField minAgeField;
    private JTextField maxAgeField;
    private JTextField minExperienceField;
    private JComboBox<String> childrenStatusCombo;
    private JButton searchButton;
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JLabel infoLabel;
    private List<String[]> departments;

    public EmployeeQueryPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("📋 Сотрудники"));

        initComponents();
        loadDepartments();
    }

    private void initComponents() {
        JPanel paramsPanel = new JPanel(new GridBagLayout());
        paramsPanel.setBorder(new TitledBorder("Параметры отбора"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        paramsPanel.add(new JLabel("Отдел:"), gbc);
        gbc.gridx = 1;
        departmentCombo = new JComboBox<>();
        departmentCombo.addItem("Все отделы");
        departmentCombo.setPreferredSize(new Dimension(200, 25));
        departmentCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(departmentCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 0;
        onlyHeadsCheck = new JCheckBox("Только начальники отделов");
        onlyHeadsCheck.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(onlyHeadsCheck, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Пол:"), gbc);
        gbc.gridx = 1;
        genderCombo = new JComboBox<>(new String[]{"Все", "Мужской", "Женский"});
        genderCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(genderCombo, gbc);

        gbc.gridx = 2; gbc.gridy = 1;
        paramsPanel.add(new JLabel("Возраст от:"), gbc);
        gbc.gridx = 3;
        minAgeField = new JTextField(5);
        minAgeField.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(minAgeField, gbc);
        gbc.gridx = 4;
        paramsPanel.add(new JLabel("до:"), gbc);
        gbc.gridx = 5;
        maxAgeField = new JTextField(5);
        maxAgeField.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(maxAgeField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        paramsPanel.add(new JLabel("Стаж (лет) от:"), gbc);
        gbc.gridx = 1;
        minExperienceField = new JTextField(5);
        minExperienceField.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(minExperienceField, gbc);

        gbc.gridx = 2; gbc.gridy = 2;
        paramsPanel.add(new JLabel("Наличие детей:"), gbc);
        gbc.gridx = 3;
        childrenStatusCombo = new JComboBox<>(new String[]{"Все", "Есть дети", "Нет детей"});
        childrenStatusCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(childrenStatusCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        paramsPanel.add(new JLabel("Зарплата от:"), gbc);
        gbc.gridx = 1;
        minSalaryField = new JTextField(10);
        minSalaryField.setFont(new Font("Arial", Font.PLAIN, 13));
        paramsPanel.add(minSalaryField, gbc);

        gbc.gridx = 2; gbc.gridy = 3;
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
        scrollPane.setBorder(new TitledBorder("Результаты"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadDepartments() {
        departments = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT department_id, department_name FROM departments ORDER BY department_name")) {
            while (rs.next()) {
                String id = rs.getString("department_id");
                String name = rs.getString("department_name");
                departments.add(new String[]{id, name});
                departmentCombo.addItem(name);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void executeQuery() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.fio AS \"ФИО\", ");
        sql.append("CASE WHEN e.gender = 'm' THEN 'Мужской' ELSE 'Женский' END AS \"Пол\", ");
        sql.append("EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.birthday_date)) AS \"Возраст\", ");
        sql.append("EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.hire_date)) AS \"Стаж\", ");
        sql.append("e.salary AS \"Зарплата\", ");
        sql.append("CASE WHEN e.has_children THEN e.children_count::text ELSE 'Нет' END AS \"Дети\", ");
        sql.append("p.profession_name AS \"Профессия\", ");
        sql.append("COALESCE(d.department_name, 'Не назначен') AS \"Отдел\", ");
        sql.append("CASE WHEN d.head_department_id = e.employee_id THEN 'Да' ELSE 'Нет' END AS \"Начальник отдела\" ");
        sql.append("FROM employee e ");
        sql.append("JOIN profession_types p ON e.profession_type = p.type_id ");
        sql.append("LEFT JOIN brigades b ON e.brigade_id = b.brigade_id ");
        sql.append("LEFT JOIN departments d ON b.department_id = d.department_id ");
        sql.append("WHERE e.dismissial_date IS NULL ");

        List<String> conditions = new ArrayList<>();

        int deptIdx = departmentCombo.getSelectedIndex();
        if (deptIdx > 0 && deptIdx <= departments.size()) {
            String deptId = departments.get(deptIdx - 1)[0];
            conditions.add("d.department_id = " + deptId);
        }

        if (onlyHeadsCheck.isSelected()) {
            conditions.add("EXISTS (SELECT 1 FROM departments d2 WHERE d2.head_department_id = e.employee_id)");
        }

        String gender = (String) genderCombo.getSelectedItem();
        if ("Мужской".equals(gender)) {
            conditions.add("e.gender = 'm'");
        } else if ("Женский".equals(gender)) {
            conditions.add("e.gender = 'f'");
        }

        if (!minAgeField.getText().trim().isEmpty()) {
            conditions.add("EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.birthday_date)) >= " + minAgeField.getText().trim());
        }
        if (!maxAgeField.getText().trim().isEmpty()) {
            conditions.add("EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.birthday_date)) <= " + maxAgeField.getText().trim());
        }

        if (!minExperienceField.getText().trim().isEmpty()) {
            conditions.add("EXTRACT(YEAR FROM AGE(CURRENT_DATE, e.hire_date)) >= " + minExperienceField.getText().trim());
        }

        String childrenStatus = (String) childrenStatusCombo.getSelectedItem();
        if ("Есть дети".equals(childrenStatus)) {
            conditions.add("e.has_children = true");
        } else if ("Нет детей".equals(childrenStatus)) {
            conditions.add("e.has_children = false");
        }

        if (!minSalaryField.getText().trim().isEmpty()) {
            conditions.add("e.salary >= " + minSalaryField.getText().trim());
        }

        if (!conditions.isEmpty()) {
            sql.append(" AND ").append(String.join(" AND ", conditions));
        }

        sql.append(" ORDER BY e.salary DESC");

        try (Connection conn = DatabaseManager.getConnection();
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

            int headsCount = 0;
            if (onlyHeadsCheck.isSelected()) {
                headsCount = data.size();
                infoLabel.setText("Найдено начальников отделов: " + headsCount);
            } else {
                infoLabel.setText("Всего сотрудников: " + data.size());
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }
}