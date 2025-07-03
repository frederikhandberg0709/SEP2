package via.sep2.server.model;

import via.sep2.shared.dto.UserDTO;
import via.sep2.shared.exception.AuthenticationException;

public interface AuthModel {

    UserDTO login(String username, String password) throws AuthenticationException;

    UserDTO createAccount(String username, String password, String firstName, String lastName)
            throws AuthenticationException;

    boolean usernameExists(String username);

    boolean isValidPassword(String password);

    UserDTO getUserById(int userId);
}
