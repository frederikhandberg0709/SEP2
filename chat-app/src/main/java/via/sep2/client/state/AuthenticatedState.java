package via.sep2.client.state;

import via.sep2.shared.dto.UserDTO;

public class AuthenticatedState extends SessionState {

    @Override
    public boolean canSendMessage() {
        return true;
    }

    @Override
    public boolean canJoinGroup() {
        return true;
    }

    @Override
    public boolean canCreateGroup() {
        return true;
    }

    @Override
    public boolean canViewPublicGroups() {
        return true;
    }

    @Override
    public void handleLogin(UserDTO user) {
        context.setCurrentUser(user);
    }

    @Override
    public void handleLogout() {
        context.setCurrentUser(null);
        changeState(new ConnectedState());
    }

    @Override
    public String getStateName() {
        return "Authenticated";
    }
}
