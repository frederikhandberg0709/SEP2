package via.sep2.model;

import java.sql.SQLException;

import via.sep2.dao.UserDAO;
import via.sep2.exception.AuthenticationException;
import via.sep2.shared.dto.UserDTO;
import via.sep2.utils.PasswordHasher;

public class AuthModelManager implements AuthModel {

    private final UserDAO userDAO;
    private final PasswordHasher passwordHasher;

    public AuthModelManager() {
        this.userDAO = UserDAO.getInstance();
        this.passwordHasher = PasswordHasher.getInstance();
    }

    @Override
    public UserDTO login(String username, String password) throws AuthenticationException {
        validateLoginInput(username, password);

        try {
            String storedPasswordHash = userDAO.getPasswordHash(username);
            if (storedPasswordHash == null) {
                throw new AuthenticationException("Invalid username or password");
            }

            if (!passwordHasher.verifyPassword(password, storedPasswordHash)) {
                throw new AuthenticationException("Invalid username or password");
            }

            UserDTO user = userDAO.findByUsername(username);
            if (user == null) {
                throw new AuthenticationException("User not found");
            }

            return user;

        } catch (SQLException e) {
            throw new AuthenticationException("Database error during login", e);
        }
    }

    @Override
    public UserDTO createAccount(String username, String password, String firstName, String lastName)
            throws AuthenticationException {
        validateAccountCreationInput(username, password, firstName, lastName);

        try {
            if (userDAO.usernameExists(username)) {
                throw new AuthenticationException("Username already exists");
            }

            String passwordHash = passwordHasher.hashPassword(password);

            UserDTO createdUser = userDAO.createUser(username, firstName, lastName, passwordHash);

            return createdUser;

        } catch (SQLException e) {
            throw new AuthenticationException("Database error during account creation", e);
        }
    }

    @Override
    public boolean usernameExists(String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }

        try {
            return userDAO.usernameExists(username.trim());
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    public boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }

        if (password.length() < 8) {
            return false;
        }

        return true;
    }

    @Override
    public UserDTO getUserById(int userId) {
        try {
            return userDAO.findById(userId);
        } catch (SQLException e) {
            return null;
        }
    }

    private void validateLoginInput(String username, String password) throws AuthenticationException {
        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("Username cannot be empty");
        }

        if (password == null || password.isEmpty()) {
            throw new AuthenticationException("Password cannot be empty");
        }
    }

    private void validateAccountCreationInput(String username, String password, String firstName, String lastName)
            throws AuthenticationException {

        if (username == null || username.trim().isEmpty()) {
            throw new AuthenticationException("Username cannot be empty");
        }

        if (username.length() < 3) {
            throw new AuthenticationException("Username must be at least 3 characters long");
        }

        if (username.length() > 50) {
            throw new AuthenticationException("Username cannot exceed 50 characters");
        }

        if (!isValidPassword(password)) {
            throw new AuthenticationException("Password must be at least 8 characters long");
        }

        if (firstName == null || firstName.trim().isEmpty()) {
            throw new AuthenticationException("First name cannot be empty");
        }

        if (lastName == null || lastName.trim().isEmpty()) {
            throw new AuthenticationException("Last name cannot be empty");
        }

        if (firstName.length() > 100) {
            throw new AuthenticationException("First name cannot exceed 100 characters");
        }

        if (lastName.length() > 100) {
            throw new AuthenticationException("Last name cannot exceed 100 characters");
        }

        // Check for valid characters (letters, spaces, hyphens, apostrophes)
        if (!firstName.matches("^[a-zA-Z\\s'-]+$")) {
            throw new AuthenticationException("First name contains invalid characters");
        }

        if (!lastName.matches("^[a-zA-Z\\s'-]+$")) {
            throw new AuthenticationException("Last name contains invalid characters");
        }

        // Check for valid username characters (only allowing alphanumeric and
        // underscore)
        if (!username.matches("^[a-zA-Z0-9_]+$")) {
            throw new AuthenticationException("Username can only contain letters, numbers, and underscores");
        }
    }
}
