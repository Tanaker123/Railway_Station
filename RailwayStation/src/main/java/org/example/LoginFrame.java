package org.example;

import javax.swing.*;
import java.awt.*;
import java.sql.*;

public class LoginFrame extends JFrame {
    private JComboBox<String> roleCombo;
    private JPasswordField passwordField;
    private JTextField fioField;
    private JTextField employeeIdField;

    public LoginFrame() {
        setTitle("Железнодорожная станция - Вход");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 520);
        setLocationRelativeTo(null);
        setResizable(false);

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("Железнодорожная станция ");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 18));
        titleLabel.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 2;
        gbc.gridx = 0;
        JLabel roleLabel = new JLabel("Роль:");
        roleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(roleLabel, gbc);

        roleCombo = new JComboBox<>(new String[]{
                "Администратор",
                "Водитель",
                "Пассажир"
        });
        roleCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        roleCombo.setPreferredSize(new Dimension(250, 35));
        roleCombo.addActionListener(e -> updateFieldsVisibility());
        gbc.gridx = 1;
        add(roleCombo, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        JLabel fioLabel = new JLabel("ФИО:");
        fioLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(fioLabel, gbc);

        fioField = new JTextField(20);
        fioField.setFont(new Font("Arial", Font.PLAIN, 14));
        fioField.setPreferredSize(new Dimension(250, 35));
        gbc.gridx = 1;
        add(fioField, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        JLabel employeeIdLabel = new JLabel("Табельный номер:");
        employeeIdLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(employeeIdLabel, gbc);

        employeeIdField = new JTextField(20);
        employeeIdField.setFont(new Font("Arial", Font.PLAIN, 14));
        employeeIdField.setPreferredSize(new Dimension(250, 35));
        gbc.gridx = 1;
        add(employeeIdField, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        JLabel passwordLabel = new JLabel("Пароль:");
        passwordLabel.setFont(new Font("Arial", Font.BOLD, 14));
        add(passwordLabel, gbc);

        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Arial", Font.PLAIN, 16));
        passwordField.setPreferredSize(new Dimension(250, 35));
        gbc.gridx = 1;
        add(passwordField, gbc);

        JButton loginButton = new JButton("Войти");
        JButton exitButton = new JButton("Выход");
        loginButton.setFont(new Font("Arial", Font.BOLD, 14));
        exitButton.setFont(new Font("Arial", Font.BOLD, 14));
        loginButton.setPreferredSize(new Dimension(120, 40));
        exitButton.setPreferredSize(new Dimension(120, 40));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        add(buttonPanel, gbc);

        loginButton.addActionListener(e -> performLogin());
        exitButton.addActionListener(e -> System.exit(0));

        updateFieldsVisibility();

        getRootPane().setDefaultButton(loginButton);
    }

    private void updateFieldsVisibility() {
        String role = (String) roleCombo.getSelectedItem();
        if ("Администратор".equals(role)) {
            fioField.setEnabled(false);
            fioField.setEditable(false);
            employeeIdField.setEnabled(false);
            employeeIdField.setEditable(false);
        } else if ("Водитель".equals(role)) {
            fioField.setEnabled(true);
            fioField.setEditable(true);
            employeeIdField.setEnabled(true);
            employeeIdField.setEditable(true);
        } else if ("Пассажир".equals(role)) {
            fioField.setEnabled(true);
            fioField.setEditable(true);
            employeeIdField.setEnabled(true);
            employeeIdField.setEditable(true);
        }
    }

    private void performLogin() {
        String role = (String) roleCombo.getSelectedItem();
        String password = new String(passwordField.getPassword());
        String fio = fioField.getText().trim();
        String employeeId = employeeIdField.getText().trim();

        boolean validPassword = false;
        switch (role) {
            case "Администратор":
                validPassword = password.equals("admin");
                break;
            case "Водитель":
                validPassword = password.equals("driver");
                break;
            case "Пассажир":
                validPassword = password.equals("passenger");
                break;
        }

        if (!validPassword) {
            JOptionPane.showMessageDialog(this,
                    "Неверный пароль для роли: " + role,
                    "Ошибка входа", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int userId = -1;
        String userName = "";

        if ("Администратор".equals(role)) {
            userId = 71;
            userName = "Администратор";
            SessionManager.getInstance().login(role, userId, userName, "Администратор");
            new MainFrame(role);
            dispose();

        } else if ("Водитель".equals(role)) {
            if (fio.isEmpty() && employeeId.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Введите ФИО или табельный номер водителя!",
                        "Ошибка входа", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try (Connection conn = DatabaseConfig.getConnection()) {
                String sql = "SELECT e.employee_id, e.fio FROM employee e " +
                        "JOIN drivers d ON e.employee_id = d.driver_id " +
                        "WHERE e.dismissial_date IS NULL";

                if (!employeeId.isEmpty()) {
                    sql += " AND e.employee_id = " + employeeId;
                } else if (!fio.isEmpty()) {
                    sql += " AND e.fio ILIKE '%" + fio + "%'";
                }
                sql += " LIMIT 1";

                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);

                if (rs.next()) {
                    userId = rs.getInt("employee_id");
                    userName = rs.getString("fio");
                    SessionManager.getInstance().login(role, userId, userName, userName);
                    new MainFrame(role);
                    dispose();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Водитель не найден! Проверьте ФИО или табельный номер.",
                            "Ошибка входа", JOptionPane.ERROR_MESSAGE);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

        } else if ("Пассажир".equals(role)) {
            String idStr = employeeIdField.getText().trim();

            if (idStr.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Введите ID пассажира!\nДоступные ID: 231,232,233,234,235,236,237,411,412,413,414",
                        "Ошибка входа", JOptionPane.ERROR_MESSAGE);
                return;
            }

            try {
                int passengerId = Integer.parseInt(idStr);
                try (Connection conn = DatabaseConfig.getConnection()) {
                    String sql = "SELECT fio FROM passengers WHERE passenger_id = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setInt(1, passengerId);
                    ResultSet rs = pstmt.executeQuery();

                    if (rs.next()) {
                        userName = rs.getString("fio");
                        SessionManager.getInstance().login(role, passengerId, userName, userName);
                        new MainFrame(role);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Пассажир с ID " + passengerId + " не найден!");
                    }
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID должен быть числом! Вы ввели: " + idStr);
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Ошибка БД: " + e.getMessage());
            }
        }
    }
}