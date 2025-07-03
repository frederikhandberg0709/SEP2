package via.sep2.shared.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DirectChatDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String user1Username;
    private String user2Username;
    private long createdTimestamp;
    private long lastMessageTimestamp;
    private boolean user1Archived;
    private boolean user2Archived;
    private boolean user1Blocked;
    private boolean user2Blocked;

    public String getOtherUser(String currentUser) {
        return currentUser.equals(user1Username) ? user2Username : user1Username;
    }

    public boolean isBlockedBy(String username) {
        if (username.equals(user1Username)) {
            return user2Blocked;
        } else if (username.equals(user2Username)) {
            return user1Blocked;
        }
        return false;
    }

    public boolean isArchivedBy(String username) {
        if (username.equals(user1Username)) {
            return user1Archived;
        } else if (username.equals(user2Username)) {
            return user2Archived;
        }
        return false;
    }
}
