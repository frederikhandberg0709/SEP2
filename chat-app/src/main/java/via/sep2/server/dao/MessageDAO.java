package via.sep2.server.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import via.sep2.server.database.DatabaseConnection;
import via.sep2.shared.dto.MessageDTO;

public class MessageDAO {

    private static MessageDAO instance;

    private MessageDAO() {}

    public static synchronized MessageDAO getInstance() {
        if (instance == null) {
            instance = new MessageDAO();
        }
        return instance;
    }

    public MessageDTO saveMessage(MessageDTO message) throws SQLException {
        String sql = """
            INSERT INTO messages (room_id, direct_chat_id, sender_username, content, timestamp)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            if (message.getRoomId() > 0) {
                stmt.setInt(1, message.getRoomId());
                stmt.setNull(2, java.sql.Types.INTEGER);
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
                stmt.setInt(2, Math.abs(message.getRoomId()));
            }
            /*stmt.setObject(
                1,
                message.getRoomId() > 0 ? message.getRoomId() : null
            );
            stmt.setObject(
                2,
                message.getRoomId() <= 0 ? Math.abs(message.getRoomId()) : null
                );*/
            stmt.setString(3, message.getSenderUsername());
            stmt.setString(4, message.getContent());
            stmt.setTimestamp(5, new Timestamp(message.getTimestamp()));

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                message.setId(rs.getInt("id"));

                if (message.getRoomId() <= 0) {
                    DirectChatDAO.getInstance().updateLastMessageTimestamp(
                        Math.abs(message.getRoomId())
                    );
                }

                return message;
            }
            throw new SQLException("Failed to save message, no ID returned");
        }
    }

    public List<MessageDTO> getGroupChatMessages(int roomId, int limit)
        throws SQLException {
        String sql = """
            SELECT id, room_id, sender_username, content, timestamp
            FROM messages
            WHERE room_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        return getMessagesFromQuery(sql, roomId, limit);
    }

    public List<MessageDTO> getDirectChatMessages(int directChatId, int limit)
        throws SQLException {
        String sql = """
            SELECT id, direct_chat_id as room_id, sender_username, content, timestamp
            FROM messages
            WHERE direct_chat_id = ?
            ORDER BY timestamp DESC
            LIMIT ?
            """;

        List<MessageDTO> messages = getMessagesFromQuery(
            sql,
            directChatId,
            limit
        );
        messages.forEach(msg -> msg.setRoomId(-msg.getRoomId()));
        return messages;
    }

    public List<MessageDTO> getMessagesAfter(
        int roomId,
        long timestamp,
        boolean isDirectChat
    ) throws SQLException {
        String sql;
        if (isDirectChat) {
            sql = """
                SELECT id, direct_chat_id as room_id, sender_username, content, timestamp
                FROM messages
                WHERE direct_chat_id = ? AND timestamp > ?
                ORDER BY timestamp ASC
                """;
        } else {
            sql = """
                SELECT id, room_id, sender_username, content, timestamp
                FROM messages
                WHERE room_id = ? AND timestamp > ?
                ORDER BY timestamp ASC
                """;
        }

        List<MessageDTO> messages = new ArrayList<>();

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, Math.abs(roomId));
            stmt.setTimestamp(2, new Timestamp(timestamp));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MessageDTO message = createMessageFromResultSet(rs);
                if (isDirectChat) {
                    message.setRoomId(-message.getRoomId());
                }
                messages.add(message);
            }
        }

        return messages;
    }

    /*public void editMessage(
        int messageId,
        String newContent,
        String editorUsername
    ) throws SQLException {
        if (!canUserEditMessage(messageId, editorUsername)) {
            throw new SQLException("User not authorized to edit this message");
        }

        String sql = """
            UPDATE messages
            SET content = ?, is_edited = true, edited_timestamp = ?
            WHERE id = ? AND is_deleted = false
            """;

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, newContent);
            stmt.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
            stmt.setInt(3, messageId);

            stmt.executeUpdate();
        }
        }*/

    public void deleteMessage(int messageId, String deleterUsername)
        throws SQLException {
        if (!canUserDeleteMessage(messageId, deleterUsername)) {
            throw new SQLException(
                "User not authorized to delete this message"
            );
        }

        String sql = "UPDATE messages SET is_deleted = true WHERE id = ?";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, messageId);
            stmt.executeUpdate();
        }
    }

    public MessageDTO getMessageById(int messageId) throws SQLException {
        String sql = """
            SELECT id, COALESCE(room_id, -direct_chat_id) as room_id, sender_username, content, timestamp
            FROM messages WHERE id = ?
            """;

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return createMessageFromResultSet(rs);
            }
            return null;
        }
    }

    /*private boolean canUserEditMessage(int messageId, String username)
        throws SQLException {
        String sql =
            "SELECT sender_username FROM messages WHERE id = ? AND is_deleted = false";

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return username.equals(rs.getString("sender_username"));
            }
            return false;
        }
    }*/

    private boolean canUserDeleteMessage(int messageId, String username)
        throws SQLException {
        String sql = """
            SELECT m.sender_username, m.room_id, m.direct_chat_id,
                   gm.role
            FROM messages m
            LEFT JOIN group_members gm ON m.room_id = gm.room_id AND gm.username = ?
            WHERE m.id = ? AND m.is_deleted = false
            """;

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setString(1, username);
            stmt.setInt(2, messageId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String sender = rs.getString("sender_username");
                String role = rs.getString("role");

                return (
                    username.equals(sender) ||
                    "ADMIN".equals(role) ||
                    "CREATOR".equals(role)
                );
            }
            return false;
        }
    }

    private List<MessageDTO> getMessagesFromQuery(String sql, int id, int limit)
        throws SQLException {
        List<MessageDTO> messages = new ArrayList<>();

        try (
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
        ) {
            stmt.setInt(1, id);
            stmt.setInt(2, limit);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                MessageDTO message = createMessageFromResultSet(rs);
                messages.add(0, message);
            }
        }

        return messages;
    }

    private MessageDTO createMessageFromResultSet(ResultSet rs)
        throws SQLException {
        MessageDTO message = new MessageDTO();
        message.setId(rs.getInt("id"));
        message.setRoomId(rs.getInt("room_id"));
        message.setSenderUsername(rs.getString("sender_username"));
        message.setContent(rs.getString("content"));
        message.setTimestamp(rs.getTimestamp("timestamp").getTime());
        // message.setEdited(rs.getBoolean("is_edited"));

        // Timestamp editedTs = rs.getTimestamp("edited_timestamp");
        // message.setEditedTimestamp(editedTs != null ? editedTs.getTime() : 0);

         message.setDeleted(rs.getBoolean("is_deleted"));

        return message;
    }
}
