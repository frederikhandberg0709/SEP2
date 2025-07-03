package via.sep2.client.view.chat;

public class ChatItemData {

    public enum ChatType {
        DIRECT, GROUP
    }

    private final int id;
    private final String name;
    private final String preview;
    private final String avatarText;
    private final String time;
    private final ChatType type;
    private final boolean isOnline;
    private final int unreadCount;

    public ChatItemData(int id, String name, String preview, String avatarText,
            String time, ChatType type, boolean isOnline, int unreadCount) {
        this.id = id;
        this.name = name;
        this.preview = preview;
        this.avatarText = avatarText;
        this.time = time;
        this.type = type;
        this.isOnline = isOnline;
        this.unreadCount = unreadCount;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPreview() {
        return preview;
    }

    public String getAvatarText() {
        return avatarText;
    }

    public String getTime() {
        return time;
    }

    public ChatType getType() {
        return type;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    @Override
    public String toString() {
        return name;
    }
}
