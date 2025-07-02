package via.sep2.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import via.sep2.database.DatabaseConnection;
import via.sep2.shared.dto.ChatMemberDTO;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.MemberRole;

public class GroupChatDAO {

    private static GroupChatDAO instance;

    private GroupChatDAO() {
    }

    public static synchronized GroupChatDAO getInstance() {
        if (instance == null) {
            instance = new GroupChatDAO();
        }
        return instance;
    }

    public ChatRoomDTO createGroupChat(String name, String creatorUsername, String description,
            boolean isPrivate, int maxMembers) throws SQLException {
        String sql = """
                INSERT INTO group_chats (name, creator_username, description, is_private, max_members, created_timestamp)
                VALUES (?, ?, ?, ?, ?, ?)
                RETURNING id, created_timestamp
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            Timestamp now = new Timestamp(System.currentTimeMillis());
            stmt.setString(1, name);
            stmt.setString(2, creatorUsername);
            stmt.setString(3, description);
            stmt.setBoolean(4, isPrivate);
            stmt.setInt(5, maxMembers);
            stmt.setTimestamp(6, now);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int roomId = rs.getInt("id");
                long timestamp = rs.getTimestamp("created_timestamp").getTime();

                addMemberToGroup(roomId, creatorUsername, MemberRole.CREATOR, creatorUsername);

                return new ChatRoomDTO(roomId, name, creatorUsername, timestamp, description, isPrivate, maxMembers);
            }
            throw new SQLException("Failed to create group chat, no ID returned");
        }
    }

    public void addMemberToGroup(int roomId, String username, MemberRole role, String invitedBy) throws SQLException {
        String sql = """
                INSERT INTO group_members (room_id, username, role, joined_timestamp, invited_by)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (room_id, username) DO NOTHING
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            stmt.setString(2, username);
            stmt.setString(3, role.name());
            stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            stmt.setString(5, invitedBy);

            stmt.executeUpdate();
        }
    }

    public void removeMemberFromGroup(int roomId, String username) throws SQLException {
        String sql = "DELETE FROM group_members WHERE room_id = ? AND username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            stmt.setString(2, username);
            stmt.executeUpdate();
        }
    }

    public void updateMemberRole(int roomId, String username, MemberRole newRole) throws SQLException {
        String sql = "UPDATE group_members SET role = ? WHERE room_id = ? AND username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newRole.name());
            stmt.setInt(2, roomId);
            stmt.setString(3, username);
            stmt.executeUpdate();
        }
    }

    public List<ChatMemberDTO> getGroupMembers(int roomId) throws SQLException {
        String sql = """
                SELECT room_id, username, role, joined_timestamp, is_muted, mute_expiry, invited_by
                FROM group_members
                WHERE room_id = ?
                ORDER BY role DESC, joined_timestamp ASC
                """;

        List<ChatMemberDTO> members = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ChatMemberDTO member = new ChatMemberDTO(
                        rs.getInt("room_id"),
                        rs.getString("username"),
                        MemberRole.valueOf(rs.getString("role")),
                        rs.getTimestamp("joined_timestamp").getTime(),
                        rs.getString("invited_by"));
                members.add(member);
            }
        }

        return members;
    }

    public boolean isUserInGroup(String username, int roomId) throws SQLException {
        String sql = "SELECT 1 FROM group_members WHERE room_id = ? AND username = ? LIMIT 1";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            stmt.setString(2, username);

            ResultSet rs = stmt.executeQuery();
            return rs.next();
        }
    }

    public MemberRole getUserRole(String username, int roomId) throws SQLException {
        String sql = "SELECT role FROM group_members WHERE room_id = ? AND username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            stmt.setString(2, username);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return MemberRole.valueOf(rs.getString("role"));
            }
            return null;
        }
    }

    public List<ChatRoomDTO> getPublicGroupChats() throws SQLException {
        String sql = """
                SELECT id, name, creator_username, description, is_private, max_members, created_timestamp
                FROM group_chats
                WHERE is_private = false
                ORDER BY created_timestamp DESC
                """;

        return getGroupChatsFromQuery(sql);
    }

    public List<ChatRoomDTO> getUserGroupChats(String username) throws SQLException {
        String sql = """
                SELECT gc.id, gc.name, gc.creator_username, gc.description, gc.is_private, gc.max_members, gc.created_timestamp
                FROM group_chats gc
                JOIN group_members gm ON gc.id = gm.room_id
                WHERE gm.username = ?
                ORDER BY gc.created_timestamp DESC
                """;

        List<ChatRoomDTO> chats = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ChatRoomDTO chat = new ChatRoomDTO(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("creator_username"),
                        rs.getTimestamp("created_timestamp").getTime(),
                        rs.getString("description"),
                        rs.getBoolean("is_private"),
                        rs.getInt("max_members"));
                chats.add(chat);
            }
        }

        return chats;
    }

    public List<ChatRoomDTO> searchGroupChats(String query) throws SQLException {
        String sql = """
                SELECT id, name, creator_username, description, is_private, max_members, created_timestamp
                FROM group_chats
                WHERE is_private = false
                  AND (LOWER(name) LIKE LOWER(?) OR LOWER(description) LIKE LOWER(?))
                ORDER BY created_timestamp DESC
                """;

        List<ChatRoomDTO> chats = new ArrayList<>();
        String searchPattern = "%" + query + "%";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ChatRoomDTO chat = new ChatRoomDTO(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("creator_username"),
                        rs.getTimestamp("created_timestamp").getTime(),
                        rs.getString("description"),
                        rs.getBoolean("is_private"),
                        rs.getInt("max_members"));
                chats.add(chat);
            }
        }

        return chats;
    }

    public ChatRoomDTO getGroupChatById(int roomId) throws SQLException {
        String sql = """
                SELECT id, name, creator_username, description, is_private, max_members, created_timestamp
                FROM group_chats WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, roomId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new ChatRoomDTO(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("creator_username"),
                        rs.getTimestamp("created_timestamp").getTime(),
                        rs.getString("description"),
                        rs.getBoolean("is_private"),
                        rs.getInt("max_members"));
            }
            return null;
        }
    }

    public void updateGroupChatSettings(int roomId, String name, String description,
            boolean isPrivate, int maxMembers) throws SQLException {
        String sql = """
                UPDATE group_chats
                SET name = ?, description = ?, is_private = ?, max_members = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, name);
            stmt.setString(2, description);
            stmt.setBoolean(3, isPrivate);
            stmt.setInt(4, maxMembers);
            stmt.setInt(5, roomId);

            stmt.executeUpdate();
        }
    }

    public void transferOwnership(int roomId, String newOwner) throws SQLException {
        String sql = "UPDATE group_chats SET creator_username = ? WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, newOwner);
            stmt.setInt(2, roomId);
            stmt.executeUpdate();
        }

        updateMemberRole(roomId, newOwner, MemberRole.CREATOR);
    }

    private List<ChatRoomDTO> getGroupChatsFromQuery(String sql) throws SQLException {
        List<ChatRoomDTO> chats = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ChatRoomDTO chat = new ChatRoomDTO(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("creator_username"),
                        rs.getTimestamp("created_timestamp").getTime(),
                        rs.getString("description"),
                        rs.getBoolean("is_private"),
                        rs.getInt("max_members"));
                chats.add(chat);
            }
        }

        return chats;
    }
}
