package via.sep2.shared.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

// TODO: decide if we should use Lombok for automatic constructor, getters and setters.
// also, might be good to use library like Jackson instead of Serializable interface.
public class MessageDTO implements Serializable {
    private int id;
    private UserDTO sender;
    private String content;
    private LocalDateTime sentAt;
}
