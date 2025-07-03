package via.sep2.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import via.sep2.server.database.DatabaseConnection;
import via.sep2.shared.dto.DirectChatDTO;

public class DirectChatDAO {

    private static DirectChatDAO instance;

    private DirectChatDAO() {
    }

    public static synchronized DirectChatDAO getInstance() {
        if (instance == null) {
            instance = new DirectChatDAO();
        }
        return instance;
    }

    public DirectChatDTO createDirectChat(String user1, String user2) throws SQLException {
        String firstUser = user1.compareTo(user2) < 0 ? user1 : user2;
        String secondUser = user1.compareTo(user2) < 0 ? user2 : user1;

        String sql = """
                INSERT INTO direct_chats (user1_username, user2_username, created_timestamp)
                VALUES (?, ?, ?)
                ON CONFLICT (user1_username, user2_username) DO NOTHING
                RETURNING id, created_timestamp
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = new Timestamp(System.currentTimeMillis());
            stmt.setString(1, firstUser);
            stmt.setString(2, secondUser);
            stmt.setTimestamp(3, now);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new DirectChatDTO(
                        rs.getInt("id"),
                        firstUser,
                        secondUser,
                        rs.getTimestamp("created_timestamp").getTime(),
                        0, false, false, false, false);
            } else {
                return getDirectChat(user1, user2);
            }
        }
    }

    public DirectChatDTO getDirectChat(String user1, String user2) throws SQLException {
        String firstUser = user1.compareTo(user2) < 0 ? user1 : user2;
        String secondUser = user1.compareTo(user2) < 0 ? user2 : user1;

        String sql = """
                SELECT id, user1_username, user2_username, created_timestamp, last_message_timestamp,
                       user1_archived, user2_archived, user1_blocked, user2_blocked
                FROM direct_chats
                WHERE user1_username = ? AND user2_username = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, firstUser);
            stmt.setString(2, secondUser);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new DirectChatDTO(
                        rs.getInt("id"),
                        rs.getString("user1_username"),
                        rs.getString("user2_username"),
                        rs.getTimestamp("created_timestamp").getTime(),
                        rs.getTimestamp("last_message_timestamp") != null
                                ? rs.getTimestamp("last_message_timestamp").getTime()
                                : 0,
                        rs.getBoolean("user1_archived"),
                        rs.getBoolean("user2_archived"),
                        rs.getBoolean("user1_blocked"),
                        rs.getBoolean("user2_blocked"));
            }
            return null;
        }
    }

    public List<DirectChatDTO> getUserDirectChats(String username) throws SQLException {
        String sql = """
                SELECT id, user1_username, user2_username, created_timestamp, last_message_timestamp,
                       user1_archived, user2_archived, user1_blocked, user2_blocked
                FROM direct_chats
                WHERE (user1_username = ? OR user2_username = ?)
                  AND NOT ((user1_username = ? AND user1_archived = true) OR
                          (user2_username = ? AND user2_archived = true))
                ORDER BY last_message_timestamp DESC NULLS LAST, created_timestamp DESC
                """;

        List<DirectChatDTO> chats = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            stmt.setString(2, username);
            stmt.setString(3, username);
            stmt.setString(4, username);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                DirectChatDTO chat = new DirectChatDTO(
                        rs.getInt("id"),
                        rs.getString("user1_username"),
                        rs.getString("user2_username"),
                        rs.getTimestamp("created_timestamp").getTime(),
                        rs.getTimestamp("last_message_timestamp") != null
                                ? rs.getTimestamp("last_message_timestamp").getTime()
                                : 0,
                        rs.getBoolean("user1_archived"),
                        rs.getBoolean("user2_archived"),
                        rs.getBoolean("user1_blocked"),
                        rs.getBoolean("user2_blocked"));
                chats.add(chat);
            }
        }

        return chats;
    }

    public void updateDirectChatSettings(String username, int directChatId, Boolean archived, Boolean blocked)
            throws SQLException {
        DirectChatDTO chat = getDirectChatById(directChatId);
        if (chat == null)
            return;

        String column = null;
        if (username.equals(chat.getUser1Username())) {
            column = archived != null ? "user1_archived" : "user1_blocked";
        } else if (username.equals(chat.getUser2Username())) {
            column = archived != null ? "user2_archived" : "user2_blocked";
        }

        if (column != null) {
            String sql = "UPDATE direct_chats SET " + column + " = ? WHERE id = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setBoolean(1, archived != null ? archived : blocked);
                stmt.setInt(2, directChatId);
                stmt.executeUpdate();
            }
        }
    }

    public DirectChatDTO getDirectChatById(int id) throws SQLException {
        String sql = """
                SELECT id, user1_username, user2_username, created_timestamp, last_message_timestamp,
                       user1_archived, user2_archived, user1_blocked, user2_blocked
                FROM direct_chats WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new DirectChatDTO(
                        rs.getInt("id"),
                        rs.getString("user1_username"),
                        rs.getString("user2_username"),
                        rs.getTimestamp("created_timestamp").getTime(),
                        rs.getTimestamp("last_message_timestamp") != null
                                ? rs.getTimestamp("last_message_timestamp").getTime()
                                : 0,
                        rs.getBoolean("user1_archived"),
                        rs.getBoolean("user2_archived"),
                        rs.getBoolean("user1_blocked"),
                        rs.getBoolean("user2_blocked"));
            }
            return null;
        }
    }

    public void updateLastMessageTimestamp(int directChatId) throws SQLException {
        String sql = "UPDATE direct_chats SET last_message_timestamp = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(2, directChatId);
            stmt.executeUpdate();
        }
    }
}
