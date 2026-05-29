package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.List;

public class TableManager {
    private String currentTable;
    private DefaultTableModel tableModel;
    private JTable dataTable;
    private JLabel recordCountLabel;
    private Map<String, String> columnTranslations;
    private Runnable refreshCallback;

    public TableManager(DefaultTableModel tableModel, JTable dataTable, JLabel recordCountLabel,
                        Map<String, String> columnTranslations, Runnable refreshCallback) {
        this.tableModel = tableModel;
        this.dataTable = dataTable;
        this.recordCountLabel = recordCountLabel;
        this.columnTranslations = columnTranslations;
        this.refreshCallback = refreshCallback;
    }

    public void setCurrentTable(String table) {
        this.currentTable = table;
    }

    public boolean isIdColumn(String colName) {
        String lowerName = colName.toLowerCase();
        return lowerName.endsWith("_id") || lowerName.equals("id") ||
                lowerName.equals("ticket_id") || lowerName.equals("employee_id") ||
                lowerName.equals("passenger_id") || lowerName.equals("trip_id") ||
                lowerName.equals("route_id") || lowerName.equals("station_id") ||
                lowerName.equals("brigade_id") || lowerName.equals("department_id") ||
                lowerName.equals("locomotive_id") || lowerName.equals("train_id") ||
                lowerName.equals("schedule_id") || lowerName.equals("maintenance_id") ||
                lowerName.equals("medical_id") || lowerName.equals("delay_id") ||
                lowerName.equals("preparation_id") || lowerName.equals("assigment_id") ||
                lowerName.equals("type_id") || lowerName.equals("category_id");
    }

    public void executeAndDisplayQuery(String sql) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            Vector<String> columns = new Vector<>();

            if (currentTable != null && currentTable.equals("trains")) {
                columns.add("Номер поезда");
                columns.add("Локомотив");
                columns.add("Тип поезда");

                Vector<Vector<Object>> data = new Vector<>();
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("train_id"));
                    row.add(rs.getString("locomotive_number"));
                    row.add(rs.getString("type_name"));
                    data.add(row);
                }
                tableModel.setDataVector(data, columns);
                if (recordCountLabel != null) {
                    recordCountLabel.setText("Записей: " + data.size());
                }
                return;
            }

            if (currentTable != null && currentTable.equals("trip_delays")) {
                String newSql = "SELECT td.delay_id, r.route_name, td.delay_time, dr.reason_name, " +
                        "td.trip_id, td.route_station_id " +
                        "FROM trip_delays td " +
                        "LEFT JOIN trips t ON td.trip_id = t.trip_id " +
                        "LEFT JOIN routes r ON t.route_id = r.route_id " +
                        "LEFT JOIN delay_reasons dr ON td.reason_id = dr.reason_id";
                try (Statement stmt2 = conn.createStatement();
                     ResultSet rs2 = stmt2.executeQuery(newSql)) {
                    ResultSetMetaData meta2 = rs2.getMetaData();
                    int colCount2 = meta2.getColumnCount();

                    for (int i = 1; i <= colCount2; i++) {
                        String colName = meta2.getColumnLabel(i);
                        if (colName.equals("delay_id")) {
                            columns.add("ID задержки");
                        } else if (colName.equals("route_name")) {
                            columns.add("Маршрут");
                        } else if (colName.equals("delay_time")) {
                            columns.add("Время задержки");
                        } else if (colName.equals("reason_name")) {
                            columns.add("Причина");
                        } else if (colName.equals("trip_id")) {
                            columns.add("ID рейса");
                        } else if (colName.equals("route_station_id")) {
                            columns.add("ID станции маршрута");
                        }
                    }

                    Vector<Vector<Object>> data = new Vector<>();
                    while (rs2.next()) {
                        Vector<Object> row = new Vector<>();
                        row.add(rs2.getInt("delay_id"));
                        row.add(rs2.getString("route_name") != null ? rs2.getString("route_name") : "Неизвестно");
                        row.add(rs2.getObject("delay_time") != null ? rs2.getObject("delay_time").toString() : "0");
                        row.add(rs2.getString("reason_name") != null ? rs2.getString("reason_name") : "Неизвестно");
                        row.add(rs2.getInt("trip_id"));
                        row.add(rs2.getInt("route_station_id"));
                        data.add(row);
                    }
                    tableModel.setDataVector(data, columns);
                    if (recordCountLabel != null) {
                        recordCountLabel.setText("Записей: " + data.size());
                    }
                    return;
                }
            }

            for (int i = 1; i <= columnCount; i++) {
                String colName = meta.getColumnLabel(i);
                String translated = columnTranslations.getOrDefault(colName, colName);
                if (!isIdColumn(colName)) {
                    columns.add(translated);
                }
            }

            Vector<Vector<Object>> data = new Vector<>();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = meta.getColumnLabel(i);
                    if (!isIdColumn(colName)) {
                        Object value = rs.getObject(i);

                        if (value instanceof Timestamp) {
                            row.add(((Timestamp) value).toString().substring(0, 16));
                        } else if (value instanceof Date) {
                            row.add(((Date) value).toString());
                        } else if (value instanceof Boolean) {
                            row.add((Boolean) value ? "Да" : "Нет");
                        } else if (colName.equals("profession_type")) {
                            String profId = value != null ? value.toString() : "";
                            row.add(getProfessionName(profId));
                        } else if (colName.equals("brigade_id")) {
                            String brigadeId = value != null ? value.toString() : "";
                            row.add(getBrigadeName(brigadeId));
                        } else if (colName.equals("brigade_type")) {
                            String brigadeType = value != null ? value.toString() : "";
                            row.add(getBrigadeTypeName(brigadeType));
                        } else if (colName.equals("has_children")) {
                            row.add("true".equals(String.valueOf(value)) || "t".equals(String.valueOf(value)) ? "Да" : "Нет");
                        } else if (colName.equals("children_count")) {
                            row.add(value != null ? value.toString() : "0");
                        } else if (colName.equals("status")) {
                            String statusId = value != null ? value.toString() : "";
                            row.add(getStatusName(statusId));
                        } else {
                            row.add(value != null ? value.toString() : "");
                        }
                    }
                }
                data.add(row);
            }

            tableModel.setDataVector(data, columns);
            if (recordCountLabel != null) {
                recordCountLabel.setText("Записей: " + data.size());
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Ошибка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void loadTrainsTable() {
        String sql = "SELECT t.train_id, l.locomotive_number, tt.type_name " +
                "FROM trains t " +
                "LEFT JOIN locomotives l ON t.locomotive_id = l.locomotive_id " +
                "LEFT JOIN train_types tt ON t.train_type_id = tt.train_type_id";
        executeAndDisplayQuery(sql);
    }

    private String getProfessionName(String id) {
        if (id == null || id.isEmpty()) return "";
        switch (id) {
            case "1": return "Водители";
            case "2": return "Ремонтники";
            case "3": return "Диспетчеры";
            case "4": return "Подготовка поездов";
            case "5": return "Справочная служба";
            case "6": return "Кассиры";
            default: return id;
        }
    }

    private String getBrigadeName(String id) {
        if (id == null || id.isEmpty() || id.equals("null")) return "Не назначена";
        switch (id) {
            case "10": return "Локомотивная бригада";
            case "20": return "Диспетчерская бригада";
            case "30": return "Ремонтная бригада";
            case "40": return "Ремонтная бригада №2";
            case "50": return "Диспетчерская бригада №2";
            case "60": return "Бригада подготовки поездов";
            case "70": return "Справочная бригада";
            case "80": return "Бригада кассиров";
            case "90": return "Администрация";
            default: return id;
        }
    }

    private String getBrigadeTypeName(String id) {
        if (id == null || id.isEmpty()) return "";
        switch (id) {
            case "21": return "Локомотивная бригада";
            case "22": return "Ремонтная бригада";
            case "23": return "Диспетчерская бригада";
            case "24": return "Бригада подготовки поездов";
            case "25": return "Справочная бригада";
            case "26": return "Бригада кассиров";
            case "27": return "Смешанная бригада";
            default: return id;
        }
    }

    private String getStatusName(String id) {
        if (id == null || id.isEmpty()) return "";
        switch (id) {
            case "46": return "Активен";
            case "47": return "Возвращен";
            case "48": return "Использован";
            case "49": return "Просрочен";
            default: return id;
        }
    }

    public void showAddRecordDialog(Component parent) {
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + currentTable + " LIMIT 0")) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            JDialog dialog = new JDialog((Dialog) SwingUtilities.getWindowAncestor(parent), "Добавить запись в " + currentTable, true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(650, 600);
            dialog.setLocationRelativeTo(parent);

            JPanel fieldsPanel = new JPanel(new GridBagLayout());
            fieldsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            Map<String, JComponent> fields = new HashMap<>();
            Map<String, String> columnTypes = new HashMap<>();
            int row = 0;

            for (int i = 1; i <= columnCount; i++) {
                String colName = meta.getColumnName(i);
                String colType = meta.getColumnTypeName(i);
                columnTypes.put(colName, colType);

                if (isIdColumn(colName)) continue;

                gbc.gridx = 0;
                gbc.gridy = row;
                String displayName = columnTranslations.getOrDefault(colName, colName);
                fieldsPanel.add(new JLabel(displayName + ":"), gbc);

                JComponent field;

                if (colName.equals("gender")) {
                    JComboBox<String> combo = new JComboBox<>(new String[]{"m", "f"});
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colName.equals("profession_type")) {
                    JComboBox<String> combo = new JComboBox<>();
                    combo.addItem("1 - Водители");
                    combo.addItem("2 - Ремонтники");
                    combo.addItem("3 - Диспетчеры");
                    combo.addItem("4 - Подготовка поездов");
                    combo.addItem("5 - Справочная служба");
                    combo.addItem("6 - Кассиры");
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colName.equals("brigade_id")) {
                    JComboBox<String> combo = new JComboBox<>();
                    combo.addItem("NULL");
                    combo.addItem("10 - Локомотивная бригада");
                    combo.addItem("20 - Диспетчерская бригада");
                    combo.addItem("30 - Ремонтная бригада");
                    combo.addItem("40 - Ремонтная бригада №2");
                    combo.addItem("50 - Диспетчерская бригада №2");
                    combo.addItem("60 - Бригада подготовки поездов");
                    combo.addItem("70 - Справочная бригада");
                    combo.addItem("80 - Бригада кассиров");
                    combo.addItem("90 - Администрация");
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colName.equals("has_children")) {
                    JComboBox<String> combo = new JComboBox<>(new String[]{"Нет", "Да"});
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colName.equals("children_count")) {
                    JTextField textField = new JTextField("0", 10);
                    textField.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = textField;
                } else if (colName.equals("status")) {
                    JComboBox<String> combo = new JComboBox<>(new String[]{"46 - Активен", "47 - Возвращен", "48 - Использован", "49 - Просрочен"});
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colType.toLowerCase().contains("date") || colName.contains("date") || colName.equals("dismissial_date")) {
                    JTextField dateField = new JTextField("", 12);
                    dateField.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = dateField;
                } else {
                    JTextField textField = new JTextField(20);
                    textField.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = textField;
                }

                gbc.gridx = 1;
                fieldsPanel.add(field, gbc);
                fields.put(colName, field);
                row++;
            }

            JScrollPane scrollPane = new JScrollPane(fieldsPanel);
            scrollPane.setPreferredSize(new Dimension(600, 500));
            dialog.add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            JButton saveButton = new JButton("Сохранить");
            saveButton.setFont(new Font("Arial", Font.BOLD, 13));
            saveButton.setBackground(new Color(0, 153, 76));
            saveButton.setForeground(Color.BLACK);
            saveButton.setFocusPainted(false);
            saveButton.setPreferredSize(new Dimension(120, 35));

            JButton cancelButton = new JButton("Отмена");
            cancelButton.setFont(new Font("Arial", Font.BOLD, 13));
            cancelButton.setFocusPainted(false);
            cancelButton.setPreferredSize(new Dimension(120, 35));

            saveButton.addActionListener(e -> {
                try {
                    StringBuilder columns = new StringBuilder();
                    StringBuilder values = new StringBuilder();
                    List<Object> params = new ArrayList<>();

                    for (Map.Entry<String, JComponent> entry : fields.entrySet()) {
                        String colName = entry.getKey();
                        JComponent comp = entry.getValue();
                        Object value = null;

                        if (comp instanceof JTextField) {
                            String text = ((JTextField) comp).getText().trim();
                            if (!text.isEmpty()) {
                                String colType = columnTypes.get(colName);
                                if (colType != null && (colType.toLowerCase().contains("date") || colName.contains("date"))) {
                                    try {
                                        value = java.sql.Date.valueOf(text);
                                    } catch (IllegalArgumentException ex) {
                                        value = text;
                                    }
                                } else if (colName.equals("children_count")) {
                                    try {
                                        value = Integer.parseInt(text);
                                    } catch (NumberFormatException ex) {
                                        value = 0;
                                    }
                                } else {
                                    value = text;
                                }
                            }
                        } else if (comp instanceof JComboBox) {
                            String selected = (String) ((JComboBox<?>) comp).getSelectedItem();
                            if (selected != null) {
                                if (selected.contains(" - ")) {
                                    value = selected.split(" - ")[0];
                                } else if (selected.equals("NULL")) {
                                    value = null;
                                } else if (colName.equals("has_children")) {
                                    value = selected.equals("Да");
                                } else if (colName.equals("gender")) {
                                    value = selected;
                                } else {
                                    value = selected;
                                }
                            }
                        }

                        if (value != null && !value.toString().isEmpty()) {
                            if (columns.length() > 0) {
                                columns.append(", ");
                                values.append(", ");
                            }
                            columns.append(colName);
                            values.append("?");
                            params.add(value);
                        }
                    }

                    if (params.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "Нет данных для сохранения!");
                        return;
                    }

                    String insertSql = "INSERT INTO " + currentTable + " (" + columns.toString() +
                            ") VALUES (" + values.toString() + ")";

                    PreparedStatement pstmt = conn.prepareStatement(insertSql);
                    for (int i = 0; i < params.size(); i++) {
                        pstmt.setObject(i + 1, params.get(i));
                    }
                    pstmt.executeUpdate();

                    JOptionPane.showMessageDialog(dialog, "Запись успешно добавлена!");
                    dialog.dispose();
                    if (refreshCallback != null) refreshCallback.run();

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Ошибка: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });

            cancelButton.addActionListener(e -> dialog.dispose());

            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent, "Ошибка: " + e.getMessage());
        }
    }

    public void editSelectedRecord(Component parent, int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(parent, "Выберите запись для редактирования!");
            return;
        }

        String idColumn = null;
        String idValue = null;

        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + currentTable + " LIMIT 0")) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                String colName = meta.getColumnName(i);
                if (isIdColumn(colName)) {
                    idColumn = colName;
                    break;
                }
            }

            if (idColumn == null) {
                JOptionPane.showMessageDialog(parent, "Не удалось определить ID записи");
                return;
            }

            String findIdSql = "SELECT " + idColumn + " FROM " + currentTable + " LIMIT 1 OFFSET " + selectedRow;
            Statement idStmt = conn.createStatement();
            ResultSet idRs = idStmt.executeQuery(findIdSql);
            if (idRs.next()) {
                idValue = idRs.getString(1);
            }
            idRs.close();
            idStmt.close();

            if (idValue == null) {
                JOptionPane.showMessageDialog(parent, "Не удалось получить ID записи");
                return;
            }

            String selectSql = "SELECT * FROM " + currentTable + " WHERE " + idColumn + " = " + idValue;
            Statement selectStmt = conn.createStatement();
            ResultSet dataRs = selectStmt.executeQuery(selectSql);

            if (!dataRs.next()) {
                JOptionPane.showMessageDialog(parent, "Запись не найдена");
                dataRs.close();
                selectStmt.close();
                return;
            }

            JDialog dialog = new JDialog((Dialog) SwingUtilities.getWindowAncestor(parent), "Редактировать запись в " + currentTable, true);
            dialog.setLayout(new BorderLayout(10, 10));
            dialog.setSize(650, 600);
            dialog.setLocationRelativeTo(parent);

            JPanel fieldsPanel = new JPanel(new GridBagLayout());
            fieldsPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            Map<String, JComponent> fields = new HashMap<>();
            Map<String, String> columnTypes = new HashMap<>();
            int row = 0;

            for (int i = 1; i <= columnCount; i++) {
                String colName = meta.getColumnName(i);
                String colType = meta.getColumnTypeName(i);
                columnTypes.put(colName, colType);

                if (isIdColumn(colName)) continue;

                gbc.gridx = 0;
                gbc.gridy = row;
                String displayName = columnTranslations.getOrDefault(colName, colName);
                fieldsPanel.add(new JLabel(displayName + ":"), gbc);

                String currentValue = dataRs.getString(colName);
                JComponent field;

                if (colName.equals("gender")) {
                    JComboBox<String> combo = new JComboBox<>(new String[]{"m", "f"});
                    combo.setSelectedItem(currentValue);
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colName.equals("profession_type")) {
                    JComboBox<String> combo = new JComboBox<>();
                    combo.addItem("1 - Водители");
                    combo.addItem("2 - Ремонтники");
                    combo.addItem("3 - Диспетчеры");
                    combo.addItem("4 - Подготовка поездов");
                    combo.addItem("5 - Справочная служба");
                    combo.addItem("6 - Кассиры");
                    if (currentValue != null) {
                        combo.setSelectedItem(currentValue + " - " + getProfessionName(currentValue));
                    }
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colName.equals("brigade_id")) {
                    JComboBox<String> combo = new JComboBox<>();
                    combo.addItem("NULL");
                    combo.addItem("10 - Локомотивная бригада");
                    combo.addItem("20 - Диспетчерская бригада");
                    combo.addItem("30 - Ремонтная бригада");
                    combo.addItem("40 - Ремонтная бригада №2");
                    combo.addItem("50 - Диспетчерская бригада №2");
                    combo.addItem("60 - Бригада подготовки поездов");
                    combo.addItem("70 - Справочная бригада");
                    combo.addItem("80 - Бригада кассиров");
                    combo.addItem("90 - Администрация");
                    if (currentValue != null && !currentValue.equals("null")) {
                        combo.setSelectedItem(currentValue + " - " + getBrigadeName(currentValue));
                    } else {
                        combo.setSelectedItem("NULL");
                    }
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colName.equals("has_children")) {
                    JComboBox<String> combo = new JComboBox<>(new String[]{"Нет", "Да"});
                    combo.setSelectedItem("true".equals(currentValue) || "t".equals(currentValue) ? "Да" : "Нет");
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colName.equals("children_count")) {
                    JTextField textField = new JTextField(currentValue != null ? currentValue : "0", 10);
                    textField.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = textField;
                } else if (colName.equals("status")) {
                    JComboBox<String> combo = new JComboBox<>(new String[]{"46 - Активен", "47 - Возвращен", "48 - Использован", "49 - Просрочен"});
                    if (currentValue != null) {
                        combo.setSelectedItem(currentValue + " - " + getStatusName(currentValue));
                    }
                    combo.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = combo;
                } else if (colType.toLowerCase().contains("date") || colName.contains("date") || colName.equals("dismissial_date")) {
                    JTextField dateField = new JTextField(currentValue != null ? currentValue : "", 12);
                    dateField.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = dateField;
                } else {
                    JTextField textField = new JTextField(currentValue != null ? currentValue : "", 20);
                    textField.setFont(new Font("Arial", Font.PLAIN, 13));
                    field = textField;
                }

                gbc.gridx = 1;
                fieldsPanel.add(field, gbc);
                fields.put(colName, field);
                row++;
            }

            dataRs.close();
            selectStmt.close();

            JScrollPane scrollPane = new JScrollPane(fieldsPanel);
            scrollPane.setPreferredSize(new Dimension(600, 500));
            dialog.add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
            JButton saveButton = new JButton("Сохранить изменения");
            saveButton.setFont(new Font("Arial", Font.BOLD, 13));
            saveButton.setBackground(new Color(0, 153, 76));
            saveButton.setForeground(Color.BLACK);
            saveButton.setFocusPainted(false);
            saveButton.setPreferredSize(new Dimension(150, 35));

            JButton cancelButton = new JButton("Отмена");
            cancelButton.setFont(new Font("Arial", Font.BOLD, 13));
            cancelButton.setFocusPainted(false);
            cancelButton.setPreferredSize(new Dimension(120, 35));

            final String finalIdColumn = idColumn;
            final String finalIdValue = idValue;

            saveButton.addActionListener(e -> {
                try {
                    StringBuilder setClause = new StringBuilder();
                    List<Object> params = new ArrayList<>();

                    for (Map.Entry<String, JComponent> entry : fields.entrySet()) {
                        String colName = entry.getKey();
                        JComponent comp = entry.getValue();
                        Object newValue = null;

                        if (comp instanceof JTextField) {
                            String text = ((JTextField) comp).getText().trim();
                            if (!text.isEmpty()) {
                                String colType = columnTypes.get(colName);
                                if (colType != null && (colType.toLowerCase().contains("date") || colName.contains("date"))) {
                                    try {
                                        newValue = java.sql.Date.valueOf(text);
                                    } catch (IllegalArgumentException ex) {
                                        newValue = text;
                                    }
                                } else if (colName.equals("children_count")) {
                                    try {
                                        newValue = Integer.parseInt(text);
                                    } catch (NumberFormatException ex) {
                                        newValue = 0;
                                    }
                                } else {
                                    newValue = text;
                                }
                            }
                        } else if (comp instanceof JComboBox) {
                            String selected = (String) ((JComboBox<?>) comp).getSelectedItem();
                            if (selected != null) {
                                if (colName.equals("has_children")) {
                                    newValue = selected.equals("Да");
                                } else if (selected.contains(" - ")) {
                                    newValue = selected.split(" - ")[0];
                                } else if (selected.equals("NULL")) {
                                    newValue = null;
                                } else if (colName.equals("gender")) {
                                    newValue = selected;
                                } else {
                                    newValue = selected;
                                }
                            }
                        }

                        if (newValue != null && !newValue.toString().isEmpty()) {
                            if (setClause.length() > 0) setClause.append(", ");
                            setClause.append(colName).append(" = ?");
                            params.add(newValue);
                        }
                    }

                    if (params.isEmpty()) {
                        JOptionPane.showMessageDialog(dialog, "Нет изменений для сохранения");
                        dialog.dispose();
                        return;
                    }

                    String updateSql = "UPDATE " + currentTable + " SET " + setClause.toString() +
                            " WHERE " + finalIdColumn + " = ?";
                    params.add(finalIdValue);

                    PreparedStatement pstmt = conn.prepareStatement(updateSql);
                    for (int i = 0; i < params.size(); i++) {
                        pstmt.setObject(i + 1, params.get(i));
                    }
                    pstmt.executeUpdate();

                    JOptionPane.showMessageDialog(dialog, "Запись успешно обновлена!");
                    dialog.dispose();
                    if (refreshCallback != null) refreshCallback.run();

                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(dialog, "Ошибка: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });

            cancelButton.addActionListener(e -> dialog.dispose());

            buttonPanel.add(saveButton);
            buttonPanel.add(cancelButton);
            dialog.add(buttonPanel, BorderLayout.SOUTH);

            dialog.setVisible(true);

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent, "Ошибка: " + e.getMessage());
        }
    }

    public void deleteSelectedRecord(Component parent, int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(parent, "Выберите запись для удаления!");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(parent,
                "Вы уверены, что хотите удалить выбранную запись?\nЭто действие невозможно отменить!",
                "Подтверждение удаления", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection conn = DatabaseManager.getConnection()) {
            String idColumn = null;
            String idValue = null;

            String getColumnsSql = "SELECT * FROM " + currentTable + " LIMIT 0";
            Statement colStmt = conn.createStatement();
            ResultSet colRs = colStmt.executeQuery(getColumnsSql);
            ResultSetMetaData meta = colRs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String colName = meta.getColumnName(i);
                if (isIdColumn(colName)) {
                    idColumn = colName;
                    break;
                }
            }
            colRs.close();
            colStmt.close();

            if (idColumn == null) {
                JOptionPane.showMessageDialog(parent, "Не удалось определить ID записи для удаления");
                return;
            }

            String findIdSql = "SELECT " + idColumn + " FROM " + currentTable + " LIMIT 1 OFFSET " + selectedRow;
            Statement idStmt = conn.createStatement();
            ResultSet idRs = idStmt.executeQuery(findIdSql);
            if (idRs.next()) {
                idValue = idRs.getString(1);
            }
            idRs.close();
            idStmt.close();

            if (idValue == null) {
                JOptionPane.showMessageDialog(parent, "Запись не найдена");
                return;
            }

            if (currentTable.equals("employee")) {
                String checkDrivers = "SELECT COUNT(*) FROM drivers WHERE driver_id = " + idValue;
                Statement checkStmt = conn.createStatement();
                ResultSet checkRs = checkStmt.executeQuery(checkDrivers);
                checkRs.next();
                int driversCount = checkRs.getInt(1);
                checkRs.close();
                checkStmt.close();

                if (driversCount > 0) {
                    JOptionPane.showMessageDialog(parent,
                            "Невозможно удалить сотрудника, так как он является водителем!\n" +
                                    "Сначала удалите связанные записи из таблицы drivers.",
                            "Ошибка удаления", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            String deleteSql = "DELETE FROM " + currentTable + " WHERE " + idColumn + " = ?";
            PreparedStatement pstmt = conn.prepareStatement(deleteSql);
            pstmt.setObject(1, idValue);
            int rows = pstmt.executeUpdate();

            if (rows > 0) {
                JOptionPane.showMessageDialog(parent, "Запись успешно удалена!");
                if (refreshCallback != null) refreshCallback.run();
            } else {
                JOptionPane.showMessageDialog(parent, "Запись не найдена");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(parent, "Ошибка удаления: " + e.getMessage());
            e.printStackTrace();
        }
    }
}