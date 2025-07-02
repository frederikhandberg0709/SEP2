package via.sep2.client.view.chat;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class ChatItemCell extends ListCell<ChatItemData> {

    private HBox container;
    private Label avatarLabel;
    private VBox infoContainer;
    private Label nameLabel;
    private Label previewLabel;
    private VBox metaContainer;
    private Label timeLabel;
    private Label unreadBadge;
    private Label onlineIndicator;

    public ChatItemCell() {
        createComponents();
        layoutComponents();
        styleComponents();
    }

    private void createComponents() {
        container = new HBox();
        avatarLabel = new Label();
        infoContainer = new VBox();
        nameLabel = new Label();
        previewLabel = new Label();
        metaContainer = new VBox();
        timeLabel = new Label();
        unreadBadge = new Label();
        onlineIndicator = new Label();
    }

    private void layoutComponents() {
        // Avatar with online indicator
        HBox avatarContainer = new HBox();
        avatarContainer.getChildren().addAll(avatarLabel, onlineIndicator);
        avatarContainer.setAlignment(Pos.CENTER);

        // Chat info (name and preview)
        infoContainer.getChildren().addAll(nameLabel, previewLabel);
        infoContainer.setSpacing(2);
        HBox.setHgrow(infoContainer, Priority.ALWAYS);

        // Meta info (time and unread count)
        metaContainer.getChildren().addAll(timeLabel, unreadBadge);
        metaContainer.setSpacing(4);
        metaContainer.setAlignment(Pos.TOP_RIGHT);

        // Main container
        container
            .getChildren()
            .addAll(avatarContainer, infoContainer, metaContainer);
        container.setSpacing(12);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(12));
    }

    private void styleComponents() {
        // Avatar styling
        avatarLabel.getStyleClass().add("chat-avatar");
        avatarLabel.setMinSize(48, 48);
        avatarLabel.setMaxSize(48, 48);
        avatarLabel.setAlignment(Pos.CENTER);
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 18));

        // Online indicator
        onlineIndicator.getStyleClass().add("online-indicator");
        onlineIndicator.setMinSize(12, 12);
        onlineIndicator.setMaxSize(12, 12);
        onlineIndicator.setVisible(false);

        // Name label
        nameLabel.getStyleClass().add("chat-name");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 15));

        // Preview label
        previewLabel.getStyleClass().add("chat-preview");
        previewLabel.setFont(Font.font("System", 13));

        // Time label
        timeLabel.getStyleClass().add("chat-time");
        timeLabel.setFont(Font.font("System", 12));

        // Unread badge
        unreadBadge.getStyleClass().add("unread-badge");
        unreadBadge.setFont(Font.font("System", FontWeight.BOLD, 11));
        unreadBadge.setMinHeight(18);
        unreadBadge.setAlignment(Pos.CENTER);
        unreadBadge.setVisible(false);

        // Container styling
        container.getStyleClass().add("chat-item");
    }

    @Override
    protected void updateItem(ChatItemData item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
            return;
        }

        // Update avatar
        avatarLabel.setText(item.getAvatarText());
        if (item.getType() == ChatItemData.ChatType.GROUP) {
            avatarLabel.getStyleClass().add("group-avatar");
        } else {
            avatarLabel.getStyleClass().remove("group-avatar");
        }

        // Update info
        nameLabel.setText(item.getName());
        previewLabel.setText(item.getPreview());

        // Update meta info
        timeLabel.setText(item.getTime());

        // Update online indicator
        onlineIndicator.setVisible(item.isOnline());

        // Update unread badge
        if (item.getUnreadCount() > 0) {
            unreadBadge.setText(String.valueOf(item.getUnreadCount()));
            unreadBadge.setVisible(true);
        } else {
            unreadBadge.setVisible(false);
        }

        setGraphic(container);
    }
}
