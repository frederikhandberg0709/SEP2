package via.sep2.client.command;

import via.sep2.client.service.ChatService;
import via.sep2.shared.dto.ChatRoomDTO;

public class CreateGroupCommand implements Command {

    private final ChatService chatService;
    private final String roomName;
    private final String description;
    private final boolean isPrivate;
    private final int maxMembers;
    private ChatRoomDTO createdRoom;

    public CreateGroupCommand(ChatService chatService, String roomName, String description,
            boolean isPrivate, int maxMembers) {
        this.chatService = chatService;
        this.roomName = roomName;
        this.description = description;
        this.isPrivate = isPrivate;
        this.maxMembers = maxMembers;
    }

    @Override
    public void execute() throws Exception {
        createdRoom = chatService.createGroupChatAsync(roomName, description, isPrivate, maxMembers).get();
    }

    @Override
    public void undo() throws Exception {
        if (createdRoom != null) {
            chatService.leaveGroupChatAsync(createdRoom.getId()).get();
            createdRoom = null;
        }
    }

    @Override
    public boolean canUndo() {
        return createdRoom != null;
    }

    @Override
    public String getDescription() {
        return "Create group chat: " + roomName;
    }

    public ChatRoomDTO getCreatedRoom() {
        return createdRoom;
    }
}
