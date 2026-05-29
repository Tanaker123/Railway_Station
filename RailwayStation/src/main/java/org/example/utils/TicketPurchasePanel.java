package org.example.utils;

import org.example.DatabaseConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TicketPurchasePanel extends JPanel {
    private JTextField fioField;
    private JTextField passportField;
    private JComboBox<String> genderCombo;
    private JTextField birthdayField;
    private JComboBox<String> tripCombo;
    private JComboBox<String> fromStationCombo;
    private JComboBox<String> toStationCombo;
    private JComboBox<String> seatCombo;
    private JLabel priceLabel;
    private JButton searchSeatsButton;
    private JButton purchaseButton;
    private int currentTripId = -1;

    public TicketPurchasePanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new TitledBorder("Покупка билета"));

        initComponents();
        loadTrips();
        loadStations();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel passengerTitle = new JLabel("Данные пассажира");
        passengerTitle.setFont(new Font("Arial", Font.BOLD, 14));
        passengerTitle.setForeground(new Color(0, 102, 204));
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(passengerTitle, gbc);
        gbc.gridwidth = 1;

        gbc.gridy = 1;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("ФИО пассажира:*"), gbc);
        gbc.gridx = 1;
        fioField = new JTextField(25);
        fioField.setFont(new Font("Arial", Font.PLAIN, 13));
        mainPanel.add(fioField, gbc);

        gbc.gridy = 2;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Паспортные данные:"), gbc);
        gbc.gridx = 1;
        passportField = new JTextField(25);
        mainPanel.add(passportField, gbc);

        gbc.gridy = 3;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Пол:*"), gbc);
        gbc.gridx = 1;
        genderCombo = new JComboBox<>(new String[]{"Мужской", "Женский"});
        mainPanel.add(genderCombo, gbc);

        gbc.gridy = 4;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Дата рождения (ГГГГ-ММ-ДД):*"), gbc);
        gbc.gridx = 1;
        birthdayField = new JTextField(25);
        birthdayField.setText(LocalDate.now().minusYears(25).format(DateTimeFormatter.ISO_LOCAL_DATE));
        mainPanel.add(birthdayField, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        mainPanel.add(new JSeparator(), gbc);
        gbc.gridwidth = 1;

        JLabel ticketTitle = new JLabel("Данные билета");
        ticketTitle.setFont(new Font("Arial", Font.BOLD, 14));
        ticketTitle.setForeground(new Color(0, 102, 204));
        gbc.gridy = 6;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        mainPanel.add(ticketTitle, gbc);
        gbc.gridwidth = 1;

        gbc.gridy = 7;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Выберите рейс:*"), gbc);
        gbc.gridx = 1;
        tripCombo = new JComboBox<>();
        tripCombo.setPreferredSize(new Dimension(400, 30));
        tripCombo.setFont(new Font("Arial", Font.PLAIN, 12));
        tripCombo.addActionListener(e -> updateTripInfo());
        mainPanel.add(tripCombo, gbc);

        gbc.gridy = 8;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Станция отправления:*"), gbc);
        gbc.gridx = 1;
        fromStationCombo = new JComboBox<>();
        fromStationCombo.setPreferredSize(new Dimension(250, 30));
        fromStationCombo.addActionListener(e -> updateAvailableSeats());
        mainPanel.add(fromStationCombo, gbc);

        gbc.gridy = 9;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Станция прибытия:*"), gbc);
        gbc.gridx = 1;
        toStationCombo = new JComboBox<>();
        toStationCombo.setPreferredSize(new Dimension(250, 30));
        toStationCombo.addActionListener(e -> updateAvailableSeats());
        mainPanel.add(toStationCombo, gbc);

        gbc.gridy = 10;
        gbc.gridx = 0;
        searchSeatsButton = new JButton("Проверить свободные места");
        searchSeatsButton.setFont(new Font("Arial", Font.BOLD, 13));
        searchSeatsButton.setBackground(new Color(0, 153, 76));
        searchSeatsButton.setForeground(Color.WHITE);
        searchSeatsButton.addActionListener(e -> loadFreeSeats());
        gbc.gridx = 1;
        mainPanel.add(searchSeatsButton, gbc);

        gbc.gridy = 11;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Выберите место:*"), gbc);
        gbc.gridx = 1;
        seatCombo = new JComboBox<>();
        seatCombo.setPreferredSize(new Dimension(300, 30));
        seatCombo.setFont(new Font("Arial", Font.PLAIN, 12));
        mainPanel.add(seatCombo, gbc);

        gbc.gridy = 12;
        gbc.gridx = 0;
        mainPanel.add(new JLabel("Стоимость билета:"), gbc);
        gbc.gridx = 1;
        priceLabel = new JLabel("0 ₽");
        priceLabel.setFont(new Font("Arial", Font.BOLD, 18));
        priceLabel.setForeground(new Color(0, 153, 76));
        mainPanel.add(priceLabel, gbc);

        gbc.gridy = 13;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        purchaseButton = new JButton("Оформить покупку");
        purchaseButton.setFont(new Font("Arial", Font.BOLD, 14));
        purchaseButton.setBackground(new Color(0, 102, 204));
        purchaseButton.setForeground(Color.WHITE);
        purchaseButton.setPreferredSize(new Dimension(200, 40));
        purchaseButton.addActionListener(e -> purchaseTicket());
        mainPanel.add(purchaseButton, gbc);

        JScrollPane scrollPane = new JScrollPane(mainPanel);
        add(scrollPane, BorderLayout.CENTER);

        JLabel requiredLabel = new JLabel("* - обязательные поля");
        requiredLabel.setFont(new Font("Arial", Font.ITALIC, 10));
        requiredLabel.setForeground(Color.GRAY);
        add(requiredLabel, BorderLayout.SOUTH);
    }

    private void loadTrips() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT t.trip_id, r.route_name, t.departure_datetime, " +
                             "s_start.station_name as from_station, s_end.station_name as to_station " +
                             "FROM trips t " +
                             "JOIN routes r ON t.route_id = r.route_id " +
                             "JOIN stations s_start ON r.start_station_id = s_start.station_id " +
                             "JOIN stations s_end ON r.end_station_id = s_end.station_id " +
                             "WHERE t.departure_datetime > CURRENT_TIMESTAMP " +
                             "ORDER BY t.departure_datetime")) {
            while (rs.next()) {
                String display = String.format("%s → %s | %s | %s",
                        rs.getString("from_station"),
                        rs.getString("to_station"),
                        rs.getString("route_name"),
                        rs.getTimestamp("departure_datetime").toString().substring(0, 16));
                tripCombo.addItem(display + "|" + rs.getInt("trip_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadStations() {
        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT station_id, station_name FROM stations ORDER BY station_name")) {
            while (rs.next()) {
                String station = rs.getString("station_name") + "|" + rs.getInt("station_id");
                fromStationCombo.addItem(station);
                toStationCombo.addItem(station);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateTripInfo() {
        String selected = (String) tripCombo.getSelectedItem();
        if (selected != null && selected.contains("|")) {
            currentTripId = Integer.parseInt(selected.split("\\|")[1]);
            updateAvailableSeats();
        }
    }

    private void updateAvailableSeats() {
        if (currentTripId != -1) {
            loadFreeSeats();
        }
    }

    private void loadFreeSeats() {
        if (currentTripId == -1) {
            JOptionPane.showMessageDialog(this, "Сначала выберите рейс!");
            return;
        }

        try (Connection conn = DatabaseConfig.getConnection();
             CallableStatement stmt = conn.prepareCall("{call get_free_seats(?)}")) {
            stmt.setInt(1, currentTripId);
            ResultSet rs = stmt.executeQuery();

            seatCombo.removeAllItems();
            while (rs.next()) {
                String seat = "Вагон " + rs.getString("coach_number") +
                        ", Место " + rs.getString("seat_number") +
                        " (" + rs.getString("coach_type") + ")" +
                        "|" + rs.getInt("seat_id");
                seatCombo.addItem(seat);
            }

            if (seatCombo.getItemCount() == 0) {
                seatCombo.addItem("Нет свободных мест");
                priceLabel.setText("Нет мест");
            } else {
                calculateApproximatePrice();
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при загрузке мест: " + e.getMessage());
        }
    }

    private void calculateApproximatePrice() {
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT tt.base_price FROM trips t " +
                             "JOIN trains tr ON t.train_id = tr.train_id " +
                             "JOIN train_types tt ON tr.train_type_id = tt.train_type_id " +
                             "WHERE t.trip_id = ?")) {
            pstmt.setInt(1, currentTripId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int basePrice = rs.getInt("base_price");
                priceLabel.setText("от " + basePrice + " ₽");
            }
        } catch (SQLException e) {
            priceLabel.setText("Цена рассчитается при покупке");
        }
    }

    private void purchaseTicket() {
        if (fioField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите ФИО пассажира!");
            return;
        }

        if (birthdayField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Введите дату рождения!");
            return;
        }

        String selectedTrip = (String) tripCombo.getSelectedItem();
        if (selectedTrip == null || !selectedTrip.contains("|")) {
            JOptionPane.showMessageDialog(this, "Выберите рейс!");
            return;
        }

        String selectedSeat = (String) seatCombo.getSelectedItem();
        if (selectedSeat == null || !selectedSeat.contains("|") || selectedSeat.equals("Нет свободных мест")) {
            JOptionPane.showMessageDialog(this, "Выберите место!");
            return;
        }

        String selectedFrom = (String) fromStationCombo.getSelectedItem();
        String selectedTo = (String) toStationCombo.getSelectedItem();

        if (selectedFrom == null || selectedTo == null || selectedFrom.equals(selectedTo)) {
            JOptionPane.showMessageDialog(this, "Выберите корректные станции отправления и прибытия!");
            return;
        }

        int tripId = Integer.parseInt(selectedTrip.split("\\|")[1]);
        int seatId = Integer.parseInt(selectedSeat.split("\\|")[1]);
        int fromStationId = Integer.parseInt(selectedFrom.split("\\|")[1]);
        int toStationId = Integer.parseInt(selectedTo.split("\\|")[1]);
        String gender = genderCombo.getSelectedItem().equals("Мужской") ? "m" : "f";

        try (Connection conn = DatabaseConfig.getConnection()) {
            conn.setAutoCommit(false);

            String checkSeatSql = "SELECT COUNT(*) FROM tickets WHERE trip_id = ? AND seat_id = ? AND status IN (46, 48)";
            PreparedStatement checkStmt = conn.prepareStatement(checkSeatSql);
            checkStmt.setInt(1, tripId);
            checkStmt.setInt(2, seatId);
            ResultSet checkRs = checkStmt.executeQuery();
            checkRs.next();
            if (checkRs.getInt(1) > 0) {
                JOptionPane.showMessageDialog(this, "Это место уже занято! Выберите другое.");
                conn.rollback();
                loadFreeSeats();
                return;
            }

            String insertPassenger = "INSERT INTO passengers (passenger_id, fio, birthday_date, passport_details, gender) " +
                    "VALUES ((SELECT COALESCE(MAX(passenger_id), 0) + 1 FROM passengers), ?, ?, ?, ?)";
            PreparedStatement pstmt = conn.prepareStatement(insertPassenger, Statement.RETURN_GENERATED_KEYS);
            pstmt.setString(1, fioField.getText().trim());
            pstmt.setDate(2, Date.valueOf(birthdayField.getText().trim()));
            pstmt.setString(3, passportField.getText().trim());
            pstmt.setString(4, gender);
            pstmt.executeUpdate();

            ResultSet rs = pstmt.getGeneratedKeys();
            rs.next();
            int passengerId = rs.getInt(1);

            String insertTicket = "INSERT INTO tickets (start_station_id, end_station_id, trip_id, status, passenger_id, seat_id) " +
                    "VALUES (?, ?, ?, 46, ?, ?)";
            pstmt = conn.prepareStatement(insertTicket, Statement.RETURN_GENERATED_KEYS);
            pstmt.setInt(1, fromStationId);
            pstmt.setInt(2, toStationId);
            pstmt.setInt(3, tripId);
            pstmt.setInt(4, passengerId);
            pstmt.setInt(5, seatId);
            pstmt.executeUpdate();

            rs = pstmt.getGeneratedKeys();
            rs.next();
            int ticketId = rs.getInt(1);

            String getPrice = "SELECT ticket_price FROM tickets WHERE ticket_id = " + ticketId;
            Statement stmt = conn.createStatement();
            ResultSet priceRs = stmt.executeQuery(getPrice);
            priceRs.next();
            int price = priceRs.getInt(1);

            conn.commit();

            JOptionPane.showMessageDialog(this,
                    "Билет успешно куплен!\n\n" +
                            "Номер билета: " + ticketId + "\n" +
                            "Стоимость: " + price + " ₽\n" +
                            "Маршрут: " + selectedTrip.split("\\|")[0] + "\n" +
                            "Место: " + selectedSeat.split("\\|")[0] + "\n\n" +
                            "Сохраните номер билета для возврата!",
                    "Покупка оформлена", JOptionPane.INFORMATION_MESSAGE);

            fioField.setText("");
            passportField.setText("");
            seatCombo.removeAllItems();
            loadTrips();

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка при покупке: " + e.getMessage());
            e.printStackTrace();
        }
    }
}