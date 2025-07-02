package via.sep2.shared.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemberDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int roomId;
    private String username;
    private MemberRole role;
    private long joinedTimestamp;
    private String invitedBy;

    public boolean isAdmin() {
        return role == MemberRole.ADMIN || role == MemberRole.CREATOR;
    }

    public boolean canManageRoom() {
        return role == MemberRole.CREATOR || role == MemberRole.ADMIN;
    }

    public boolean canInviteUsers() {
        return role != MemberRole.MEMBER; // only admins and creator can invite
    }
}
