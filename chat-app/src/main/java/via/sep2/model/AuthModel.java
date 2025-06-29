package via.sep2.model;

import via.sep2.exception.AuthenticationException;
import via.sep2.shared.dto.UserDTO;

public interface AuthModel {

    UserDTO login(String username, String password) throws AuthenticationException;

    UserDTO createAccount(String username, String password, String firstName, String lastName)
            throws AuthenticationException;

    boolean usernameExists(String username);

    boolean isValidPassword(String password);

    UserDTO getUserById(int userId);
}
