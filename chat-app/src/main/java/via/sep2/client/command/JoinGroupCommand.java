package via.sep2.client.command;

import via.sep2.client.service.ChatService;

public class JoinGroupCommand implements Command {

    private final ChatService chatService;
    private final int roomId;
    private boolean wasExecuted = false;

    public JoinGroupCommand(ChatService chatService, int roomId) {
        this.chatService = chatService;
        this.roomId = roomId;
    }

    @Override
    public void execute() throws Exception {
        chatService.joinGroupChatAsync(roomId).get();
        wasExecuted = true;
    }

    @Override
    public void undo() throws Exception {
        if (wasExecuted) {
            chatService.leaveGroupChatAsync(roomId).get();
            wasExecuted = false;
        }
    }

    @Override
    public boolean canUndo() {
        return wasExecuted;
    }

    @Override
    public String getDescription() {
        return "Join group chat (ID: " + roomId + ")";
    }
}
