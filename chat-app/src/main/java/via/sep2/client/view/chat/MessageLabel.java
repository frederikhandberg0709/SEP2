package via.sep2.client.view.chat;

import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import via.sep2.shared.dto.MessageDTO;

public class MessageLabel extends HBox {

    private final MessageDTO message;
    private final String currentUsername;
    private final VBox messageContent;
    private final Label contentLabel;
    private final Label metaLabel;
    private final BiConsumer<Integer, String> onEditMessage;
    private final Consumer<Integer> onDeleteMessage;

    public MessageLabel(
        MessageDTO message,
        String currentUsername,
        BiConsumer<Integer, String> onEditMessage,
        Consumer<Integer> onDeleteMessage
    ) {
        this.message = message;
        this.currentUsername = currentUsername;
        this.onEditMessage = onEditMessage;
        this.onDeleteMessage = onDeleteMessage;

        this.messageContent = new VBox();
        this.contentLabel = new Label();
        this.metaLabel = new Label();

        setupComponents();
        setupContextMenu();
        updateContent();
    }

    private void setupComponents() {
        messageContent.getChildren().addAll(contentLabel, metaLabel);
        messageContent.setSpacing(2);
        messageContent.setMaxWidth(400);
        messageContent.setPrefWidth(Region.USE_COMPUTED_SIZE);
        messageContent.setMinWidth(Region.USE_PREF_SIZE);

        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(400);
        contentLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);
        contentLabel.setFont(Font.font(14));

        metaLabel.setFont(Font.font(10));
        metaLabel.getStyleClass().add("message-meta");
        metaLabel.setMaxWidth(400);
        metaLabel.setPrefWidth(Region.USE_COMPUTED_SIZE);

        setSpacing(0);
        setMaxWidth(Double.MAX_VALUE);
        setPrefWidth(Region.USE_COMPUTED_SIZE);

        boolean isOwnMessage = message
            .getSenderUsername()
            .equals(currentUsername);
        if (isOwnMessage) {
            messageContent.getStyleClass().addAll("message", "own-message");
            contentLabel.getStyleClass().add("own-message-content");

            setAlignment(Pos.CENTER_RIGHT);
            getChildren().add(messageContent);
        } else {
            messageContent.getStyleClass().addAll("message", "other-message");
            contentLabel.getStyleClass().add("other-message-content");

            setAlignment(Pos.CENTER_LEFT);
            getChildren().add(messageContent);
        }
    }

    private void setupContextMenu() {
        // Only show context menu for own messages
        if (!message.getSenderUsername().equals(currentUsername)) {
            return;
        }

        ContextMenu contextMenu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> showEditDialog());

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> showDeleteConfirmation());

        contextMenu.getItems().addAll(editItem, deleteItem);

        // Show context menu on right-click
        setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                contextMenu.show(this, event.getScreenX(), event.getScreenY());
            } else {
                contextMenu.hide();
            }
        });
    }

    private void showEditDialog() {
        TextInputDialog dialog = new TextInputDialog(message.getContent());
        dialog.setTitle("Edit Message");
        dialog.setHeaderText("Edit your message:");
        dialog.setContentText("Message:");

        dialog
            .getDialogPane()
            .getStylesheets()
            .add(
                getClass()
                    .getResource("/via/sep2/css/chat.css")
                    .toExternalForm()
            );

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String newContent = result.get().trim();
            if (!newContent.equals(message.getContent())) {
                onEditMessage.accept(message.getId(), newContent);
            }
        }
    }

    private void showDeleteConfirmation() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Message");
        alert.setHeaderText("Are you sure you want to delete this message?");
        alert.setContentText("This action cannot be undone.");

        alert
            .getDialogPane()
            .getStylesheets()
            .add(
                getClass()
                    .getResource("/via/sep2/css/chat.css")
                    .toExternalForm()
            );

        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (
            result.isPresent() &&
            result.get() == javafx.scene.control.ButtonType.OK
        ) {
            onDeleteMessage.accept(message.getId());
        }
    }

    public void updateMessage(MessageDTO updatedMessage) {
        message.setContent(updatedMessage.getContent());
        message.setEdited(updatedMessage.isEdited());
        message.setEditedTimestamp(updatedMessage.getEditedTimestamp());

        Platform.runLater(this::updateContent);
    }

    private void updateContent() {
        String displayText = message.getContent();
        contentLabel.setText(displayText);

        updateMetaLabel();
    }

    private void updateMetaLabel() {
        StringBuilder meta = new StringBuilder();

        meta.append(formatTime(message.getTimestamp()));

        // Add edited indicator
        if (message.isEdited()) {
            meta.append(" (edited)");
        }

        metaLabel.setText(meta.toString());

        if (message.isEdited()) {
            metaLabel.setFont(
                Font.font(
                    metaLabel.getFont().getFamily(),
                    FontWeight.NORMAL,
                    10
                )
            );
            metaLabel.getStyleClass().add("edited-indicator");
        }
    }

    private String formatTime(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {
            return "now";
        } else if (diff < 3600000) {
            return (diff / 60000) + "m ago";
        } else if (diff < 86400000) {
            return (diff / 3600000) + "h ago";
        } else {
            return (diff / 86400000) + "d ago";
        }
    }

    public MessageDTO getMessage() {
        return message;
    }
}
