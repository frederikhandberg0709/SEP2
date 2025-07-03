package via.sep2.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import via.sep2.server.database.DatabaseConnection;
import via.sep2.shared.dto.UserDTO;

public class UserDAO {

    private static UserDAO instance;

    private UserDAO() {}

    public static synchronized UserDAO getInstance() {
        if (instance == null) {
            instance = new UserDAO();
        }
        return instance;
    }

    public UserDTO findByUsername(String username) throws SQLException {
        String sql =
            "SELECT id, username, first_name, last_name FROM users WHERE username = ?";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new UserDTO(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("first_name"),
                    rs.getString("last_name")
                );
            }
            return null;
        }
    }

    public UserDTO findById(int id) throws SQLException {
        String sql =
            "SELECT id, username, first_name, last_name FROM users WHERE id = ?";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new UserDTO(
                    rs.getInt("id"),
                    rs.getString("username"),
                    rs.getString("first_name"),
                    rs.getString("last_name")
                );
            }
            return null;
        }
    }

    public String getPasswordHash(String username) throws SQLException {
        String sql = "SELECT password FROM users WHERE username = ?";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getString("password");
            }
            return null;
        }
    }

    public boolean usernameExists(String username) throws SQLException {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public UserDTO createUser(
        String username,
        String firstName,
        String lastName,
        String passwordHash
    ) throws SQLException {
        String sql =
            "INSERT INTO users (username, first_name, last_name, password) VALUES (?, ?, ?, ?) RETURNING id";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, username);
            stmt.setString(2, firstName);
            stmt.setString(3, lastName);
            stmt.setString(4, passwordHash);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int generatedId = rs.getInt("id");
                return new UserDTO(generatedId, username, firstName, lastName);
            }
            throw new SQLException("Failed to create user, no ID returned");
        }
    }

    public List<UserDTO> searchUsers(String searchTerm, int limit)
        throws SQLException {
        String sql = """
            SELECT id, username, first_name, last_name
            FROM users
            WHERE LOWER(username) LIKE LOWER(?)
               OR LOWER(first_name) LIKE LOWER(?)
               OR LOWER(last_name) LIKE LOWER(?)
               OR LOWER(CONCAT(first_name, ' ', last_name)) LIKE LOWER(?)
            ORDER BY
                CASE
                    WHEN LOWER(username) = LOWER(?) THEN 1
                    WHEN LOWER(username) LIKE LOWER(?) THEN 2
                    WHEN LOWER(first_name) LIKE LOWER(?) THEN 3
                    WHEN LOWER(last_name) LIKE LOWER(?) THEN 4
                    ELSE 5
                END,
                username
            LIMIT ?
            """;

        List<UserDTO> users = new ArrayList<>();
        String searchPattern = "%" + searchTerm + "%";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, searchPattern); // username LIKE
            stmt.setString(2, searchPattern); // first_name LIKE
            stmt.setString(3, searchPattern); // last_name LIKE
            stmt.setString(4, searchPattern); // full name LIKE
            stmt.setString(5, searchTerm); // exact username match (highest priority)
            stmt.setString(6, searchTerm + "%"); // username starts with (second priority)
            stmt.setString(7, searchTerm + "%"); // first_name starts with
            stmt.setString(8, searchTerm + "%"); // last_name starts with
            stmt.setInt(9, limit);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                users.add(
                    new UserDTO(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("first_name"),
                        rs.getString("last_name")
                    )
                );
            }
        }

        return users;
    }
}
