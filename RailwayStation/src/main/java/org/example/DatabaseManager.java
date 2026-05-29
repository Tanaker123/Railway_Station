package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    private static final String URL = "jdbc:postgresql://localhost:5432/RailwayStation";
    private static final String USER = "postgres";
    private static final String PASSWORD = "tanya123789";

    private static Connection connection = null;

    public static Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("org.postgresql.Driver");
                Properties props = new Properties();
                props.setProperty("user", USER);
                props.setProperty("password", PASSWORD);
                props.setProperty("tcpKeepAlive", "true");
                props.setProperty("socketTimeout", "0");
                props.setProperty("loginTimeout", "30");
                props.setProperty("connectTimeout", "30");

                connection = DriverManager.getConnection(URL, props);
            }
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC Driver not found", e);
        }
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("🔌 Соединение с БД закрыто");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}
