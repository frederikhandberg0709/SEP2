package via.sep2.client.state;

import via.sep2.shared.dto.UserDTO;

public class DisconnectedState extends SessionState {

    @Override
    public boolean canSendMessage() {
        return false;
    }

    @Override
    public boolean canJoinGroup() {
        return false;
    }

    @Override
    public boolean canCreateGroup() {
        return false;
    }

    @Override
    public boolean canViewPublicGroups() {
        return false;
    }

    @Override
    public void handleLogin(UserDTO user) {
        throw new IllegalStateException("Cannot login while disconnected. Connect to server first.");
    }

    @Override
    public void handleLogout() {
        // Already disconnected, no action needed
    }

    @Override
    public String getStateName() {
        return "Disconnected";
    }
}
