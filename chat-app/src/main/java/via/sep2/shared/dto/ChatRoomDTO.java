package via.sep2.shared.dto;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private ChatRoomType type;
    private String creatorUsername;
    private long createdTimestamp;
    private List<String> members;
    private List<String> admins;
    private String description;
    private boolean isPrivate;
    private int maxMembers;

    // Direct chat constructor
    public ChatRoomDTO(int id, String user1, String user2, long createdTimestamp) {
        this.id = id;
        this.name = generateDirectChatName(user1, user2);
        this.type = ChatRoomType.DIRECT;
        this.creatorUsername = user1;
        this.createdTimestamp = createdTimestamp;
        this.isPrivate = true;
        this.maxMembers = 2;
    }

    // Group chat constructor
    public ChatRoomDTO(int id, String name, String creatorUsername, long createdTimestamp,
            String description, boolean isPrivate, int maxMembers) {
        this.id = id;
        this.name = name;
        this.type = ChatRoomType.GROUP;
        this.creatorUsername = creatorUsername;
        this.createdTimestamp = createdTimestamp;
        this.description = description;
        this.isPrivate = isPrivate;
        this.maxMembers = maxMembers;
    }

    private String generateDirectChatName(String user1, String user2) {
        return user1.compareTo(user2) < 0 ? user1 + " & " + user2 : user2 + " & " + user1;
    }

    public boolean isDirectChat() {
        return type == ChatRoomType.DIRECT;
    }

    public boolean isGroupChat() {
        return type == ChatRoomType.GROUP;
    }

    public boolean isUserAdmin(String username) {
        return admins != null && admins.contains(username);
    }

    public boolean canUserManage(String username) {
        return username.equals(creatorUsername) || isUserAdmin(username);
    }
}
