package org.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConfig {
    private static final String URL = "jdbc:postgresql://localhost:5432/RailwayStation?useUnicode=true&characterEncoding=UTF-8";
    private static final String USER = "postgres";
    private static final String PASSWORD = "***"; //Пароль

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            Properties props = new Properties();
            props.setProperty("user", USER);
            props.setProperty("password", PASSWORD);
            props.setProperty("charSet", "UTF-8");
            props.setProperty("useUnicode", "true");
            props.setProperty("characterEncoding", "UTF-8");

            Connection conn = DriverManager.getConnection(URL, props);
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Драйвер PostgreSQL не найден!", e);
        }
    }
}
