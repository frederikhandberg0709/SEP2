package via.sep2.client.state;

import via.sep2.client.connection.ConnectionManager;
import via.sep2.shared.dto.UserDTO;

public abstract class SessionState {

    protected ConnectionManager context;

    public void setContext(ConnectionManager context) {
        this.context = context;
    }

    public abstract boolean canSendMessage();

    public abstract boolean canJoinGroup();

    public abstract boolean canCreateGroup();

    public abstract boolean canViewPublicGroups();

    public abstract void handleLogin(UserDTO user);

    public abstract void handleLogout();

    public abstract String getStateName();

    protected void changeState(SessionState newState) {
        context.setSessionState(newState);
    }
}
