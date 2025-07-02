package via.sep2.util;

import via.sep2.shared.dto.UserDTO;

public class SessionManager {
    private static SessionManager instance;
    private UserDTO currentUser;
    private boolean isLoggedIn;

    private SessionManager() {
        this.currentUser = null;
        this.isLoggedIn = false;
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    // Login user
    public void loginUser(UserDTO user) {
        this.currentUser = user;
        this.isLoggedIn = true;
    }

    // Logout user
    public void logout() {
        this.currentUser = null;
        this.isLoggedIn = false;

        // Clear any cached data or perform cleanup
        // You can add more cleanup logic here if needed
        System.out.println("User logged out successfully");
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        return isLoggedIn && currentUser != null;
    }

    // Get current user
    public UserDTO getCurrentUser() {
        return currentUser;
    }

    // Get current user ID
    public int getCurrentUserId() {
        if (currentUser != null) {
            return currentUser.getId();
        }
        return -1;
    }

    // Get current username
    public String getCurrentUsername() {
        if (currentUser != null) {
            return currentUser.getUsername();
        }
        return null;
    }


}
