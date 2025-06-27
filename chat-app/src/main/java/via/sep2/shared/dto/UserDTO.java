package via.sep2.shared.dto;

import java.io.Serializable;

// TODO: decide if we should use Lombok for automatic constructor, getters and setters.
// also, might be good to use library like Jackson instead of Serializable interface.
public class UserDTO implements Serializable {
    private int id;
    private String username;
    private String displayName;
}
