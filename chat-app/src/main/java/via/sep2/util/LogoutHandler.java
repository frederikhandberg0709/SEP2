package via.sep2.util;

import via.sep2.util.SceneManager;

public class LogoutHandler {
    private SessionManager sessionManager;
    private SceneManager sceneManager;

    public LogoutHandler() {
        this.sessionManager = SessionManager.getInstance();
        this.sceneManager = SceneManager.getInstance();
    }

    // Perform complete logout operation
    public void performLogout() {
        if (sessionManager.isLoggedIn()) {
            // Clear user session
            sessionManager.logout();

            // Clear scene cache
            sceneManager.clearCache();


            // Navigate back to login screen
            sceneManager.showLogin();

            System.out.println("Logout completed successfully");
        }
    }

    // Confirm logout with user
    public boolean confirmLogout() {
        return true;
    }
}
