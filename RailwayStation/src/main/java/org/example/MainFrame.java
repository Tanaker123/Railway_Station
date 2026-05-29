package org.example;

import org.example.queries.employee.BrigadeQueryPanel;
import org.example.queries.employee.DriversMedicalPanel;
import org.example.queries.employee.EmployeeQueryPanel;
import org.example.queries.locomotives.LocomotiveRepairPanel;
import org.example.queries.locomotives.LocomotiveStatusPanel;
import org.example.queries.passengers.PassengersPanel;
import org.example.queries.routes.RouteCategoryPanel;
import org.example.queries.tickets.RefundedTicketsPanel;
import org.example.queries.tickets.TicketSalesPanel;
import org.example.queries.tickets.UnpaidTicketsPanel;
import org.example.queries.trips.CancelledTripsPanel;
import org.example.queries.trips.DelayedTripsPanel;
import org.example.queries.trips.TrainRoutePanel;
import org.example.queries.trips.TripStatusPanel;
import org.example.utils.*;
import org.example.utils.FreeSeatsPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class MainFrame extends JFrame {
    private String userRole;
    private JTabbedPane mainTabbedPane;
    private JPanel queriesPanel;
    private JTable dataTable;
    private DefaultTableModel tableModel;
    private JComboBox<String> tableCombo;
    private JTextField searchField;
    private JLabel recordCountLabel;
    private TableManager tableManager;
    private Map<String, String> columnTranslations;
    private String currentTable;
    private JList<String> queryList;
    private JPanel queryContentPanel;
    private DefaultListModel<String> queryListModel;
    private Map<String, JPanel> queryPanels;

    public MainFrame(String role) {
        this.userRole = role;
        setTitle("Железнодорожная станция - " + role + " - " + SessionManager.getInstance().getCurrentUserFio());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 850);
        setLocationRelativeTo(null);

        initColumnTranslations();
        initComponents();
        setupMenuBar();
        setVisible(true);
    }

    private void initColumnTranslations() {
        columnTranslations = new HashMap<>();
        columnTranslations.put("employee_id", "Табельный номер");
        columnTranslations.put("fio", "ФИО");
        columnTranslations.put("gender", "Пол");
        columnTranslations.put("phone_number", "Телефон");
        columnTranslations.put("birthday_date", "Дата рождения");
        columnTranslations.put("hire_date", "Дата приема");
        columnTranslations.put("dismissial_date", "Дата увольнения");
        columnTranslations.put("salary", "Зарплата");
        columnTranslations.put("profession_type", "Профессия");
        columnTranslations.put("profession_name", "Название профессии");
        columnTranslations.put("department_name", "Отдел");
        columnTranslations.put("has_children", "Наличие детей");
        columnTranslations.put("children_count", "Количество детей");

        columnTranslations.put("route_name", "Маршрут");
        columnTranslations.put("route_id", "Номер маршрута");
        columnTranslations.put("start_station_id", "Станция отправления");
        columnTranslations.put("end_station_id", "Станция прибытия");
        columnTranslations.put("category_id", "Категория");

        columnTranslations.put("station_name", "Название станции");
        columnTranslations.put("city", "Город");
        columnTranslations.put("country", "Страна");
        columnTranslations.put("station_id", "Код станции");

        columnTranslations.put("ticket_id", "Номер билета");
        columnTranslations.put("date_purchase", "Дата покупки");
        columnTranslations.put("start_station_id", "Станция отправления");
        columnTranslations.put("end_station_id", "Станция прибытия");
        columnTranslations.put("trip_id", "Номер рейса");
        columnTranslations.put("status", "Статус билета");
        columnTranslations.put("railway_station_id", "Станция продажи");
        columnTranslations.put("passenger_id", "Пассажир");
        columnTranslations.put("seat_id", "Номер места");
        columnTranslations.put("ticket_price", "Цена");

        columnTranslations.put("passenger_id", "Номер пассажира");
        columnTranslations.put("FIO", "ФИО пассажира");
        columnTranslations.put("birthday_date", "Дата рождения");
        columnTranslations.put("passport_details", "Паспортные данные");
        columnTranslations.put("gender", "Пол");

        columnTranslations.put("train_id", "Номер поезда");
        columnTranslations.put("locomotive_id", "Номер локомотива");
        columnTranslations.put("train_type_id", "Тип поезда");

        columnTranslations.put("locomotive_id", "Номер локомотива");
        columnTranslations.put("locomotive_number", "Серийный номер");

        columnTranslations.put("brigade_id", "Номер бригады");
        columnTranslations.put("brigade_number", "Номер бригады");
        columnTranslations.put("brigadier_id", "Бригадир");
        columnTranslations.put("brigade_type", "Тип бригады");
        columnTranslations.put("department_id", "Отдел");

        columnTranslations.put("driver_id", "Номер водителя");
        columnTranslations.put("license_number", "Номер лицензии");

        columnTranslations.put("repair_worker_id", "Номер ремонтника");
        columnTranslations.put("specialization", "Специализация");

        columnTranslations.put("dispatcher_id", "Номер диспетчера");
        columnTranslations.put("qualification_group", "Квалификация");

        columnTranslations.put("worker_train_preparation_id", "Номер сотрудника");
        columnTranslations.put("specialization", "Специализация");

        columnTranslations.put("help_desk_id", "Номер сотрудника");
        columnTranslations.put("knowledge_of_foreign_languages", "Знание языков");

        columnTranslations.put("cashier_id", "Номер кассира");

        columnTranslations.put("medical_id", "Номер осмотра");
        columnTranslations.put("date_medical_checkup", "Дата осмотра");
        columnTranslations.put("result_checkup", "Результат");
        columnTranslations.put("next_medical_checkup", "Следующий осмотр");
        columnTranslations.put("driver_id", "Водитель");

        columnTranslations.put("train_type_id", "Код типа");
        columnTranslations.put("type_name", "Название типа");
        columnTranslations.put("base_price", "Базовая цена");

        columnTranslations.put("status_id", "Код статуса");
        columnTranslations.put("status_name", "Статус");
        columnTranslations.put("description", "Описание");

        columnTranslations.put("delay_id", "Номер задержки");
        columnTranslations.put("delay_time", "Время задержки");
        columnTranslations.put("reason_id", "Причина");
        columnTranslations.put("reason_name", "Название причины");
        columnTranslations.put("route_station_id", "Станция");

        columnTranslations.put("route_station_id", "Код");
        columnTranslations.put("station_order", "Порядок");
        columnTranslations.put("train_stop", "Время стоянки");
        columnTranslations.put("next_station_time", "Время до след. станции");

        columnTranslations.put("coach_type_id", "Код типа");
        columnTranslations.put("total_seats", "Всего мест");
        columnTranslations.put("price_coefficient", "Коэффициент цены");

        columnTranslations.put("coach_id", "Номер вагона");
        columnTranslations.put("coach_number", "Номер вагона");

        columnTranslations.put("seat_id", "ID места");
        columnTranslations.put("seat_number", "Номер места");
        columnTranslations.put("seat_class", "Класс места");

        columnTranslations.put("trip_id", "Номер рейса");
        columnTranslations.put("departure_datetime", "Время отправления");
        columnTranslations.put("train_id", "Номер поезда");
        columnTranslations.put("route_id", "Номер маршрута");

        columnTranslations.put("maintenance_id", "Номер ТО");
        columnTranslations.put("maintenance_type_id", "Тип ТО");
        columnTranslations.put("planned_date", "Плановая дата");
        columnTranslations.put("schedule_id", "Номер графика");
        columnTranslations.put("start_date", "Дата начала");
        columnTranslations.put("end_date", "Дата окончания");
        columnTranslations.put("perfomed_by", "Кто выполнил");

        columnTranslations.put("preparation_id", "Номер подготовки");
        columnTranslations.put("inspection_date", "Дата осмотра");
        columnTranslations.put("technical_check", "Тех. проверка");
        columnTranslations.put("documents_check", "Проверка документов");
        columnTranslations.put("cleaning_done", "Уборка");
        columnTranslations.put("result_preparation", "Результат");
        columnTranslations.put("responsible_employee", "Ответственный");

        columnTranslations.put("assigment_id", "Номер назначения");
        columnTranslations.put("date_from", "Дата начала");
        columnTranslations.put("date_to", "Дата окончания");

        columnTranslations.put("ticket_refund_id", "Номер возврата");
        columnTranslations.put("refund_date", "Дата возврата");
    }

    private void initComponents() {
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setFont(new Font("Arial", Font.BOLD, 13));

        if ("Администратор".equals(userRole)) {
            mainTabbedPane.addTab("Управление данными", createDataManagementPanel());
        } else if ("Водитель".equals(userRole)) {
            mainTabbedPane.addTab("Мои данные", createDriverDataPanel());
        } else if ("Пассажир".equals(userRole)) {
            mainTabbedPane.addTab("Мои билеты", createPassengerDataPanel());
        }

        mainTabbedPane.addTab("Запросы", createQueriesPanel());
        add(mainTabbedPane);
    }

    private JPanel createQueriesPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new TitledBorder("Список запросов"));
        leftPanel.setPreferredSize(new Dimension(280, 0));

        queryListModel = new DefaultListModel<>();
        queryPanels = new HashMap<>();

        if ("Администратор".equals(userRole)) {
            addAdminQueriesToList();
        } else if ("Водитель".equals(userRole)) {
            addDriverQueriesToList();
        } else if ("Пассажир".equals(userRole)) {
            addPassengerQueriesToList();
        }

        queryList = new JList<>(queryListModel);
        queryList.setFont(new Font("Arial", Font.PLAIN, 13));
        queryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        queryList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selected = queryList.getSelectedValue();
                if (selected != null) {
                    showQueryPanel(selected);
                }
            }
        });

        JScrollPane listScroll = new JScrollPane(queryList);
        leftPanel.add(listScroll, BorderLayout.CENTER);

        queryContentPanel = new JPanel(new CardLayout());
        queryContentPanel.setBorder(new TitledBorder("Результат запроса"));

        for (Map.Entry<String, JPanel> entry : queryPanels.entrySet()) {
            queryContentPanel.add(entry.getValue(), entry.getKey());
        }

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, queryContentPanel);
        splitPane.setDividerLocation(300);
        splitPane.setResizeWeight(0.25);

        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private void showQueryPanel(String queryName) {
        CardLayout cl = (CardLayout) queryContentPanel.getLayout();
        cl.show(queryContentPanel, queryName);
    }

    private void addAdminQueriesToList() {
        String[] queries = {
                "1. Сотрудники",
                "2. Работники в бригаде",
                "3. Водители и медосмотр",
                "4. Статус локомотивов",
                "5. Локомотивы и ремонт",
                "6. Поезда на маршруте",
                "7. Отмененные рейсы",
                "8. Задержанные рейсы",
                "9. Анализ продаж",
                "10. Маршруты по категориям",
                "11. Пассажиры",
                "12. Невыкупленные билеты",
                "13. Возвращенные билеты",
                "Свободные места",
                "Статусы рейсов"
        };

        for (String q : queries) {
            queryListModel.addElement(q);
        }

        queryPanels.put("1. Сотрудники", new EmployeeQueryPanel());
        queryPanels.put("2. Работники в бригаде", new BrigadeQueryPanel());
        queryPanels.put("3. Водители и медосмотр", new DriversMedicalPanel());
        queryPanels.put("4. Статус локомотивов", new LocomotiveStatusPanel());
        queryPanels.put("5. Локомотивы и ремонт", new LocomotiveRepairPanel());
        queryPanels.put("6. Поезда на маршруте", new TrainRoutePanel());
        queryPanels.put("7. Отмененные рейсы", new CancelledTripsPanel());
        queryPanels.put("8. Задержанные рейсы", new DelayedTripsPanel());
        queryPanels.put("9. Анализ продаж", new TicketSalesPanel());
        queryPanels.put("10. Маршруты по категориям", new RouteCategoryPanel());
        queryPanels.put("11. Пассажиры", new PassengersPanel());
        queryPanels.put("12. Невыкупленные билеты", new UnpaidTicketsPanel());
        queryPanels.put("13. Возвращенные билеты", new RefundedTicketsPanel());
        queryPanels.put("Свободные места", new FreeSeatsPanel());
        queryPanels.put("Статусы рейсов", new TripStatusPanel());
    }

    private void addDriverQueriesToList() {
        String[] queries = {
                "Мои рейсы",
                "Мой медосмотр",
                "Статус локомотивов",
                "Задержанные рейсы",
                "Маршруты"
        };

        for (String q : queries) {
            queryListModel.addElement(q);
        }

        queryPanels.put("Мои рейсы", new DriverTripsPanel());
        queryPanels.put("Мой медосмотр", new DriverMedicalPanel());
        queryPanels.put("Статус локомотивов", new LocomotiveStatusPanel());
        queryPanels.put("Задержанные рейсы", new DelayedTripsPanel());
        queryPanels.put("Маршруты", new RouteCategoryPanel());
    }

    private void addPassengerQueriesToList() {
        String[] queries = {
                "Мои билеты",
                "Свободные места",
                "Расписание",
                "Купить билет",
                "Вернуть билет",
                "Статусы рейсов"
        };

        for (String q : queries) {
            queryListModel.addElement(q);
        }

        queryPanels.put("Мои билеты", new PassengerTicketsPanel());
        queryPanels.put("Свободные места", new FreeSeatsPanel());
        queryPanels.put("Расписание", new SchedulePanel());
        queryPanels.put("Купить билет", new TicketPurchasePanel());
        queryPanels.put("Вернуть билет", new TicketRefundPanel());
        queryPanels.put("Статусы рейсов", new TripStatusPanel());
    }

    private JPanel createDataManagementPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel topPanel = new JPanel(new BorderLayout(10, 5));
        topPanel.setBorder(new TitledBorder("Управление данными"));

        JPanel selectPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        selectPanel.add(new JLabel("Таблица:"));

        String[] tables = {"employee", "passengers", "trains", "routes", "stations",
                "tickets", "brigades", "locomotives", "drivers",
                "repair_workers", "dispatchers", "trip_delays", "medical_checkup"};
        tableCombo = new JComboBox<>(tables);
        tableCombo.setFont(new Font("Arial", Font.PLAIN, 13));
        tableCombo.addActionListener(e -> loadTableData());
        selectPanel.add(tableCombo);

        JButton refreshButton = new JButton("Обновить");
        refreshButton.setFont(new Font("Arial", Font.BOLD, 12));
        refreshButton.setBackground(new Color(0, 102, 204));
        refreshButton.setForeground(Color.BLACK);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(e -> loadTableData());
        selectPanel.add(refreshButton);

        selectPanel.add(new JLabel("Поиск:"));
        searchField = new JTextField(20);
        searchField.setFont(new Font("Arial", Font.PLAIN, 13));
        selectPanel.add(searchField);

        JButton searchButton = new JButton("Найти");
        searchButton.setFont(new Font("Arial", Font.BOLD, 12));
        searchButton.setFocusPainted(false);
        searchButton.addActionListener(e -> searchInTable());
        selectPanel.add(searchButton);

        topPanel.add(selectPanel, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));

        JButton addButton = new JButton("Добавить запись");
        addButton.setFont(new Font("Arial", Font.BOLD, 13));
        addButton.setBackground(new Color(0, 153, 76));
        addButton.setForeground(Color.BLACK);
        addButton.setFocusPainted(false);
        addButton.setPreferredSize(new Dimension(140, 35));
        addButton.addActionListener(e -> tableManager.showAddRecordDialog(this));
        buttonPanel.add(addButton);

        JButton editButton = new JButton("Редактировать");
        editButton.setFont(new Font("Arial", Font.BOLD, 13));
        editButton.setBackground(new Color(0, 102, 204));
        editButton.setForeground(Color.BLACK);
        editButton.setFocusPainted(false);
        editButton.setPreferredSize(new Dimension(140, 35));
        editButton.addActionListener(e -> tableManager.editSelectedRecord(this, dataTable.getSelectedRow()));
        buttonPanel.add(editButton);

        JButton deleteButton = new JButton("Удалить");
        deleteButton.setFont(new Font("Arial", Font.BOLD, 13));
        deleteButton.setBackground(new Color(204, 0, 0));
        deleteButton.setForeground(Color.BLACK);
        deleteButton.setFocusPainted(false);
        deleteButton.setPreferredSize(new Dimension(140, 35));
        deleteButton.addActionListener(e -> tableManager.deleteSelectedRecord(this, dataTable.getSelectedRow()));
        buttonPanel.add(deleteButton);

        topPanel.add(buttonPanel, BorderLayout.CENTER);

        recordCountLabel = new JLabel("Записей: 0");
        recordCountLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        topPanel.add(recordCountLabel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.NORTH);

        tableModel = new DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        dataTable = new JTable(tableModel);
        dataTable.setFont(new Font("Arial", Font.PLAIN, 13));
        dataTable.setRowHeight(28);
        dataTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));
        dataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        tableManager = new TableManager(tableModel, dataTable, recordCountLabel, columnTranslations, this::loadTableData);

        JScrollPane scrollPane = new JScrollPane(dataTable);
        scrollPane.setBorder(new TitledBorder("Данные"));
        panel.add(scrollPane, BorderLayout.CENTER);

        loadTableData();
        return panel;
    }

    private void loadTableData() {
        if (tableCombo == null) return;
        currentTable = (String) tableCombo.getSelectedItem();
        if (currentTable == null) return;
        tableManager.setCurrentTable(currentTable);

        if (currentTable.equals("trains")) {
            tableManager.loadTrainsTable();
        } else {
            tableManager.executeAndDisplayQuery("SELECT * FROM " + currentTable + " LIMIT 500");
        }
    }

    private void searchInTable() {
        String searchText = searchField.getText().trim();
        if (searchText.isEmpty()) {
            loadTableData();
            return;
        }
        tableManager.executeAndDisplayQuery("SELECT * FROM " + currentTable +
                " WHERE fio ILIKE '%" + searchText + "%' LIMIT 500");
    }

    private JPanel createDriverDataPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Информация о водителе", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);

        JTextArea infoArea = new JTextArea();
        infoArea.setEditable(false);
        infoArea.setFont(new Font("Arial", Font.PLAIN, 14));
        infoArea.setMargin(new Insets(15, 15, 15, 15));

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT e.fio, CASE WHEN e.gender = 'm' THEN 'Мужской' ELSE 'Женский' END as gender, " +
                             "e.phone_number, e.salary, e.hire_date, " +
                             "m.date_medical_checkup, m.next_medical_checkup, m.result_checkup " +
                             "FROM employee e " +
                             "LEFT JOIN drivers d ON e.employee_id = d.driver_id " +
                             "LEFT JOIN medical_checkup m ON d.driver_id = m.driver_id " +
                             "WHERE e.profession_type = 1 AND e.dismissial_date IS NULL LIMIT 1")) {
            if (rs.next()) {
                infoArea.setText(
                                "         ЛИЧНЫЕ ДАННЫЕ                 │\n" +
                                "─────────────────────────────────────────\n" +
                                " ФИО: " + padRight(rs.getString("fio"), 35) + "\n" +
                                " Пол: " + padRight(rs.getString("gender"), 36) + "\n" +
                                " Телефон: " + padRight(rs.getString("phone_number"), 32) + "\n" +
                                " Зарплата: " + padRight(rs.getInt("salary") + " ₽", 32) + "\n" +
                                " Дата приема: " + padRight(rs.getDate("hire_date") != null ? rs.getDate("hire_date").toString() : "Нет данных", 29) + "\n" +
                                "─────────────────────────────────────────\n" +
                                "        МЕДИЦИНСКИЙ ОСМОТР           \n" +
                                "─────────────────────────────────────────\n" +
                                " Дата осмотра: " + padRight(rs.getDate("date_medical_checkup") != null ? rs.getDate("date_medical_checkup").toString() : "Нет данных", 28) + "\n" +
                                " Результат: " + padRight(rs.getString("result_checkup") != null ? rs.getString("result_checkup") : "Нет данных", 31) + "\n" +
                                " Следующий осмотр: " + padRight(rs.getDate("next_medical_checkup") != null ? rs.getDate("next_medical_checkup").toString() : "Нет данных", 24) + "\n" +
                                "─────────────────────────────────────────"
                );
            }
        } catch (SQLException e) {
            infoArea.setText("Ошибка загрузки данных: " + e.getMessage());
        }

        panel.add(new JScrollPane(infoArea), BorderLayout.CENTER);
        return panel;
    }

    private String padRight(String s, int n) {
        if (s == null) s = "";
        return String.format("%-" + n + "s", s);
    }

    private JPanel createPassengerDataPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        JLabel titleLabel = new JLabel("Мои билеты", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        panel.add(titleLabel, BorderLayout.NORTH);

        DefaultTableModel ticketModel = new DefaultTableModel();
        JTable ticketTable = new JTable(ticketModel);
        ticketTable.setFont(new Font("Arial", Font.PLAIN, 13));
        ticketTable.setRowHeight(28);
        ticketTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 13));

        try (Connection conn = DatabaseConfig.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT tk.ticket_id, r.route_name, s_start.station_name as from_station,\n" +
                             "       s_end.station_name as to_station, t.departure_datetime,\n" +
                             "       tk.ticket_price, ts.description as status\n" +
                             "FROM tickets tk\n" +
                             "JOIN trips t ON tk.trip_id = t.trip_id\n" +
                             "JOIN routes r ON t.route_id = r.route_id\n" +
                             "JOIN stations s_start ON tk.start_station_id = s_start.station_id\n" +
                             "JOIN stations s_end ON tk.end_station_id = s_end.station_id\n" +
                             "JOIN ticket_statuses ts ON tk.status = ts.status_id\n" +
                             "ORDER BY t.departure_datetime DESC LIMIT 50")) {

            Vector<String> columns = new Vector<>();
            columns.add("№ билета");
            columns.add("Маршрут");
            columns.add("Отправление");
            columns.add("Назначение");
            columns.add("Время отправления");
            columns.add("Цена (₽)");
            columns.add("Статус");

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("ticket_id"));
                row.add(rs.getString("route_name"));
                row.add(rs.getString("from_station"));
                row.add(rs.getString("to_station"));
                row.add(rs.getTimestamp("departure_datetime") != null ? rs.getTimestamp("departure_datetime").toString().substring(0, 16) : "");
                row.add(rs.getInt("ticket_price"));
                row.add(rs.getString("status"));
                data.add(row);
            }

            ticketModel.setDataVector(data, columns);

            JLabel countLabel = new JLabel("Всего билетов: " + data.size());
            countLabel.setFont(new Font("Arial", Font.ITALIC, 11));
            countLabel.setForeground(Color.GRAY);
            panel.add(countLabel, BorderLayout.SOUTH);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Ошибка: " + e.getMessage());
        }

        panel.add(new JScrollPane(ticketTable), BorderLayout.CENTER);
        return panel;
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Файл");
        JMenuItem exitItem = new JMenuItem("Выход");
        exitItem.setFocusPainted(false);
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Помощь");
        JMenuItem aboutItem = new JMenuItem("О программе");
        aboutItem.setFocusPainted(false);
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        JMenu roleMenu = new JMenu("Сменить роль");
        JMenuItem logoutItem = new JMenuItem("Выйти из системы");
        logoutItem.setFocusPainted(false);
        logoutItem.addActionListener(e -> {
            dispose();
            new LoginFrame();
        });
        roleMenu.add(logoutItem);
        menuBar.add(roleMenu);

        setJMenuBar(menuBar);
    }

    private void showAboutDialog() {
        String message = "Железнодорожная станция\n\n" +
                "Роль: " + userRole + "\n\n" +
                "Функциональность:\n" +
                "1. Запросы с параметрами\n" +
                "2. Возможность просмотра, редактирования, добавления и удаления данных\n" +
                "3. Покупка и возврат билетов(пассажиры)\n" +
                "4. Статусы рейсов в реальном времени\n\n";
        JOptionPane.showMessageDialog(this, message, "О программе", JOptionPane.INFORMATION_MESSAGE);
    }
}