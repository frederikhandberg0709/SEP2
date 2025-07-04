package via.sep2.client.test;

import via.sep2.shared.dto.UserDTO;

public class AuthTestUtils {

    public static UserDTO createTestUser() {
        return new UserDTO(1, "testuser", "Test", "User");
    }

    public static UserDTO createTestUser(int id, String username, String firstName, String lastName) {
        return new UserDTO(id, username, firstName, lastName);
    }

    public static boolean isValidUsername(String username) {
        return username != null &&
                username.length() >= 3 &&
                username.matches("^[a-zA-Z0-9_]+$");
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 8;
    }

    public static boolean isValidName(String name) {
        return name != null &&
                !name.trim().isEmpty() &&
                name.trim().matches("^[a-zA-Z\\s'-]+$");
    }
}
