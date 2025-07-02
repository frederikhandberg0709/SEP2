package via.sep2.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.github.cdimascio.dotenv.Dotenv;

public class DatabaseConnection {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String DB_URL = dotenv.get("DB_URL");
    private static final String DB_USER = dotenv.get("DB_USER");
    private static final String DB_PASSWORD = dotenv.get("DB_PASSWORD");

    private static Connection connection = null;

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            validateEnvironmentVariables();
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }

    private static void validateEnvironmentVariables() {
        if (DB_URL == null || DB_URL.isEmpty()) {
            throw new IllegalStateException("DB_URL environment variable is not set");
        }
        if (DB_USER == null || DB_USER.isEmpty()) {
            throw new IllegalStateException("DB_USER environment variable is not set");
        }
        if (DB_PASSWORD == null || DB_PASSWORD.isEmpty()) {
            throw new IllegalStateException("DB_PASSWORD environment variable is not set");
        }
    }
}
