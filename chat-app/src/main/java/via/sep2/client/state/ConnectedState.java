package via.sep2.client.state;

import via.sep2.shared.dto.UserDTO;

public class ConnectedState extends SessionState {

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
        return true;
    }

    @Override
    public void handleLogin(UserDTO user) {
        context.setCurrentUser(user);
        changeState(new AuthenticatedState());
    }

    @Override
    public void handleLogout() {
    }

    @Override
    public String getStateName() {
        return "Connected";
    }
}
