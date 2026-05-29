package org.example;

public class SessionManager {
    private static SessionManager instance;
    private String currentRole;
    private int currentUserId;
    private String currentUserName;
    private String currentUserFio;

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(String role, int userId, String userName, String userFio) {
        this.currentRole = role;
        this.currentUserId = userId;
        this.currentUserName = userName;
        this.currentUserFio = userFio;
        System.out.println("Вход выполнен: " + userFio + " (ID: " + userId + ", роль: " + role + ")");
    }

    public void logout() {
        this.currentRole = null;
        this.currentUserId = -1;
        this.currentUserName = null;
        this.currentUserFio = null;
    }

    public String getCurrentRole() {
        return currentRole;
    }

    public int getCurrentUserId() {
        return currentUserId;
    }

    public String getCurrentUserName() {
        return currentUserName;
    }

    public String getCurrentUserFio() {
        return currentUserFio;
    }

    public boolean isAuthenticated() {
        return currentRole != null;
    }

    public boolean isAdmin() {
        return "Администратор".equals(currentRole);
    }

    public boolean isDriver() {
        return "Водитель".equals(currentRole);
    }

    public boolean isPassenger() {
        return "Пассажир".equals(currentRole);
    }
}