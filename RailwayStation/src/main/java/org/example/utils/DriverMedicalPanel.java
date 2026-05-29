package org.example.utils;

import org.example.DatabaseConfig;
import org.example.SessionManager;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class DriverMedicalPanel extends JPanel {
    private JTable resultTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JLabel infoLabel;

    public DriverMedicalPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Мой медосмотр"));

        initComponents();
        loadDriverMedical();
    }

    private void initComponents() {
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        refreshButton = new JButton("Обновить");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setBackground(new Color(0, 102, 204));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadDriverMedical());
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
        scrollPane.setBorder(new TitledBorder("Информация о медосмотре"));
        add(scrollPane, BorderLayout.CENTER);

        infoLabel = new JLabel(" ", SwingConstants.CENTER);
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        add(infoLabel, BorderLayout.SOUTH);
    }

    private void loadDriverMedical() {
        int driverId = SessionManager.getInstance().getCurrentUserId();

        String sql = "SELECT e.fio, m.date_medical_checkup, m.next_medical_checkup,\n" +
                "       m.result_checkup,\n" +
                "       CASE WHEN m.next_medical_checkup < CURRENT_DATE THEN 'Просрочен'\n" +
                "            WHEN m.next_medical_checkup < CURRENT_DATE + INTERVAL '30 days' THEN 'Истекает скоро'\n" +
                "            ELSE 'Действителен' END as status\n" +
                "FROM drivers d\n" +
                "JOIN employee e ON d.driver_id = e.employee_id\n" +
                "LEFT JOIN medical_checkup m ON d.driver_id = m.driver_id\n" +
                "WHERE d.driver_id = " + driverId;

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            Vector<String> columns = new Vector<>();
            columns.add("ФИО водителя");
            columns.add("Дата последнего осмотра");
            columns.add("Следующий осмотр");
            columns.add("Результат");
            columns.add("Статус");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("fio"));
                row.add(rs.getDate("date_medical_checkup") != null ? rs.getDate("date_medical_checkup") : "Нет данных");
                row.add(rs.getDate("next_medical_checkup") != null ? rs.getDate("next_medical_checkup") : "Нет данных");
                row.add(rs.getString("result_checkup") != null ? rs.getString("result_checkup") : "Нет данных");
                row.add(rs.getString("status"));
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            infoLabel.setText("Информация о вашем медосмотре");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }
    }
}