package via.sep2.client.command;

import via.sep2.client.service.ChatService;

public class SendMessageCommand implements Command {

    private final ChatService chatService;
    private final int roomId;
    private final String content;
    private final boolean isGroupMessage;

    public SendMessageCommand(ChatService chatService, int roomId, String content, boolean isGroupMessage) {
        this.chatService = chatService;
        this.roomId = roomId;
        this.content = content;
        this.isGroupMessage = isGroupMessage;
    }

    @Override
    public void execute() throws Exception {
        if (isGroupMessage) {
            chatService.sendGroupMessageAsync(roomId, content).get();
        } else {
            chatService.sendDirectMessageAsync(roomId, content).get();
        }
    }

    @Override
    public void undo() throws Exception {
        throw new UnsupportedOperationException("Message sending cannot be undone");
    }

    @Override
    public boolean canUndo() {
        return false;
    }

    @Override
    public String getDescription() {
        return "Send " + (isGroupMessage ? "group" : "direct") + " message: " +
                content.substring(0, Math.min(content.length(), 50)) +
                (content.length() > 50 ? "..." : "");
    }
}
