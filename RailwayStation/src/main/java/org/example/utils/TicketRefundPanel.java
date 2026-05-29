package org.example.utils;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.*;

public class TicketRefundPanel extends JPanel {
    private JTextField ticketIdField;
    private JButton refundButton;

    public TicketRefundPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Возврат билета"));

        initComponents();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel instructionLabel = new JLabel("Введите номер билета для возврата");
        instructionLabel.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(instructionLabel, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        mainPanel.add(new JLabel("Номер билета:"), gbc);

        ticketIdField = new JTextField(15);
        ticketIdField.setFont(new Font("Arial", Font.PLAIN, 14));
        gbc.gridx = 1;
        mainPanel.add(ticketIdField, gbc);

        refundButton = new JButton("Вернуть билет");
        refundButton.setFont(new Font("Arial", Font.BOLD, 14));
        refundButton.setBackground(new Color(204, 102, 0));
        refundButton.setForeground(Color.WHITE);
        refundButton.addActionListener(e -> refundTicket());
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        mainPanel.add(refundButton, gbc);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void refundTicket() {
        String ticketIdStr = ticketIdField.getText().trim();
        if (ticketIdStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите номер билета!");
            return;
        }

        int ticketId = Integer.parseInt(ticketIdStr);

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            String checkSql = "SELECT status, t.departure_datetime FROM tickets tk " +
                    "JOIN trips t ON tk.trip_id = t.trip_id WHERE tk.ticket_id = " + ticketId;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(checkSql);

            if (!rs.next()) {
                JOptionPane.showMessageDialog(this, "Билет с номером " + ticketId + " не найден!");
                return;
            }

            int status = rs.getInt("status");
            Timestamp departureTime = rs.getTimestamp("departure_datetime");

            if (status == 48) {
                JOptionPane.showMessageDialog(this, "Нельзя вернуть уже использованный билет!");
                return;
            }

            if (status == 47) {
                JOptionPane.showMessageDialog(this, "Этот билет уже был возвращен!");
                return;
            }

            if (departureTime != null && departureTime.before(new Timestamp(System.currentTimeMillis()))) {
                JOptionPane.showMessageDialog(this, "Нельзя вернуть билет на уже отправленный рейс!");
                return;
            }

            String insertRefund = "INSERT INTO ticket_refund (ticket_refund_id, refund_date, ticket_id) " +
                    "VALUES ((SELECT COALESCE(MAX(ticket_refund_id), 0) + 1 FROM ticket_refund), CURRENT_TIMESTAMP, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertRefund);
            pstmt.setInt(1, ticketId);
            pstmt.executeUpdate();

            conn.commit();

            JOptionPane.showMessageDialog(this, "Билет №" + ticketId + " успешно возвращен!\n" +
                    "Деньги будут возвращены на карту в течение 5-10 рабочих дней.");
            ticketIdField.setText("");

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при возврате: " + e.getMessage());
            e.printStackTrace();
        }
    }
}