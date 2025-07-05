package via.sep2.shared.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private int roomId;
    private String senderUsername;
    private String content;
    private long timestamp;
    private boolean isEdited;
    private long editedTimestamp;


    public MessageDTO(int roomId, String senderUsername, String content) {
        this.roomId = roomId;
        this.senderUsername = senderUsername;
        this.content = content;
        this.timestamp = System.currentTimeMillis();
        this.isEdited = false;

    }

    public boolean isDirectMessage() {
        return roomId < 0;
    }

    public boolean isGroupMessage() {
        return roomId > 0;
    }

     public boolean canUserEdit(String username) {
    // return username.equals(senderUsername) && !isDeleted;
    // }
}
