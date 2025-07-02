package via.sep2.dao;

import via.sep2.database.DatabaseConnection;
import via.sep2.model.Contact;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ContactDAO {

    public List<Contact> getContactsForUser(int userId) throws SQLException {
        String sql = """
            SELECT u.id, u.username, u.first_name, u.last_name
            FROM users u
            WHERE u.id IN (
                SELECT CASE
                    WHEN c.user1_id = ? THEN c.user2_id
                    ELSE c.user1_id
                END
                FROM conversations c
                WHERE c.user1_id = ? OR c.user2_id = ?
            )
            """;
        List<Contact> contacts = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                contacts.add(new Contact(
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("first_name"),
                        rs.getString("last_name")
                ));
            }
        }
        return contacts;
    }
}