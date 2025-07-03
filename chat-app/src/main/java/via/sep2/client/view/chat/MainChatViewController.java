package via.sep2.client.view.chat;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import via.sep2.client.factory.ServiceFactory;
import via.sep2.client.service.AuthService;
import via.sep2.client.service.ChatService;
import via.sep2.client.util.SceneManager;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.DirectChatDTO;
import via.sep2.shared.dto.MessageDTO;
import via.sep2.shared.dto.UserDTO;

public class MainChatViewController implements Initializable {

    private static final Logger logger = Logger.getLogger(
        MainChatViewController.class.getName()
    );

    // Sidebar
    @FXML
    private Label userAvatarLabel;

    @FXML
    private Label userNameLabel;

    @FXML
    private Button logoutButton;

    @FXML
    private TextField searchField;

    @FXML
    private ToggleGroup chatFilterGroup;

    @FXML
    private ToggleButton allChatsFilter;

    @FXML
    private ToggleButton directChatsFilter;

    @FXML
    private ToggleButton groupChatsFilter;

    @FXML
    private ListView<ChatItemData> chatListView;

    // Main chat
    @FXML
    private VBox chatMainArea;

    @FXML
    private HBox chatHeader;

    @FXML
    private Label chatHeaderAvatar;

    @FXML
    private Label chatHeaderName;

    @FXML
    private Label chatHeaderStatus;

    @FXML
    private ScrollPane messagesScrollPane;

    @FXML
    private VBox messagesContainer;

    @FXML
    private HBox messageInputArea;

    @FXML
    private TextField messageInput;

    @FXML
    private Button sendButton;

    @FXML
    private VBox emptyStateArea;

    private AuthService authService;
    private ChatService chatService;

    private ObservableList<ChatItemData> allChats;
    private FilteredList<ChatItemData> filteredChats;
    private ChatItemData selectedChat;
    private ObservableList<MessageDTO> currentMessages;

    private enum ChatFilter {
        ALL,
        DIRECT,
        GROUP,
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing MainChatViewController");

        initializeServices();
        initializeData();
        setupUI();
        loadUserInfo();
        loadChats();
    }

    private void initializeServices() {
        this.authService = ServiceFactory.getInstance().getService(
            AuthService.class
        );
        this.chatService = ServiceFactory.getInstance().getService(
            ChatService.class
        );
    }

    private void initializeData() {
        this.allChats = FXCollections.observableArrayList();
        this.filteredChats = new FilteredList<>(allChats);
        this.currentMessages = FXCollections.observableArrayList();
    }

    private void setupUI() {
        setupDataBinding();
        setupChatList();
        setupMessageInput();
        setupFilters();
        setupSearch();

        // Initially show empty state
        showEmptyState();
    }

    private void setupDataBinding() {
        chatHeader.managedProperty().bind(chatHeader.visibleProperty());
        messagesScrollPane
            .managedProperty()
            .bind(messagesScrollPane.visibleProperty());
        messageInputArea
            .managedProperty()
            .bind(messageInputArea.visibleProperty());
        emptyStateArea.managedProperty().bind(emptyStateArea.visibleProperty());
    }

    private void setupChatList() {
        chatListView.setItems(filteredChats);
        chatListView.setCellFactory(
            new Callback<ListView<ChatItemData>, ListCell<ChatItemData>>() {
                @Override
                public ListCell<ChatItemData> call(
                    ListView<ChatItemData> listView
                ) {
                    return new ChatItemCell();
                }
            }
        );

        chatListView
            .getSelectionModel()
            .selectedItemProperty()
            .addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    selectChat(newSelection);
                }
            });
    }

    private void setupMessageInput() {
        messageInput.setOnAction(e -> onSendMessage());
    }

    private void setupFilters() {
        chatFilterGroup
            .selectedToggleProperty()
            .addListener((obs, oldToggle, newToggle) -> {
                if (newToggle != null) {
                    updateChatFilter();
                }
            });
    }

    private void setupSearch() {
        searchField
            .textProperty()
            .addListener((obs, oldText, newText) -> {
                updateChatFilter();
            });
    }

    private void loadUserInfo() {
        UserDTO currentUser = authService.getCurrentUser();
        if (currentUser != null) {
            userNameLabel.setText(
                currentUser.getFirstName() + " " + currentUser.getLastName()
            );
            userAvatarLabel.setText(
                getInitials(
                    currentUser.getFirstName(),
                    currentUser.getLastName()
                )
            );
        }
    }

    private void loadChats() {
        Platform.runLater(() -> {
            allChats.clear();

            // Load direct chats
            chatService
                .getDirectChatsAsync()
                .thenAccept(directChats ->
                    Platform.runLater(() -> {
                        for (DirectChatDTO directChat : directChats) {
                            ChatItemData chatItem =
                                createChatItemFromDirectChat(directChat);
                            allChats.add(chatItem);
                        }
                    })
                )
                .exceptionally(throwable -> {
                    logger.warning(
                        "Failed to load direct chats: " + throwable.getMessage()
                    );
                    return null;
                });

            // Load group chats
            chatService
                .getMyGroupChatsAsync()
                .thenAccept(groupChats ->
                    Platform.runLater(() -> {
                        for (ChatRoomDTO groupChat : groupChats) {
                            ChatItemData chatItem = createChatItemFromGroupChat(
                                groupChat
                            );
                            allChats.add(chatItem);
                        }
                    })
                )
                .exceptionally(throwable -> {
                    logger.warning(
                        "Failed to load group chats: " + throwable.getMessage()
                    );
                    return null;
                });
        });
    }

    private ChatItemData createChatItemFromDirectChat(
        DirectChatDTO directChat
    ) {
        String currentUsername = authService.getCurrentUser().getUsername();
        String otherUser = directChat.getOtherUser(currentUsername);

        return new ChatItemData(
            directChat.getId(),
            otherUser,
            "Click to start chatting...",
            getInitials(otherUser, ""),
            formatTime(directChat.getLastMessageTimestamp()),
            ChatItemData.ChatType.DIRECT,
            false,
            0
        );
    }

    private ChatItemData createChatItemFromGroupChat(ChatRoomDTO groupChat) {
        return new ChatItemData(
            groupChat.getId(),
            groupChat.getName(),
            groupChat.getDescription() != null
                ? groupChat.getDescription()
                : "Group chat",
            getInitials(groupChat.getName(), ""),
            formatTime(groupChat.getCreatedTimestamp()),
            ChatItemData.ChatType.GROUP,
            false,
            0
        );
    }

    private void selectChat(ChatItemData chatItem) {
        this.selectedChat = chatItem;

        // Update chat header
        chatHeaderName.setText(chatItem.getName());
        chatHeaderAvatar.setText(chatItem.getAvatarText());

        if (chatItem.getType() == ChatItemData.ChatType.DIRECT) {
            chatHeaderStatus.setText("Direct message");
        } else {
            chatHeaderStatus.setText("Group chat");
        }

        showChatInterface();

        loadMessagesForChat(chatItem);
    }

    private void loadMessagesForChat(ChatItemData chatItem) {
        currentMessages.clear();

        CompletableFuture<List<MessageDTO>> messagesFuture;

        if (chatItem.getType() == ChatItemData.ChatType.DIRECT) {
            messagesFuture = chatService.getDirectChatMessagesAsync(
                chatItem.getId(),
                50
            );
        } else {
            messagesFuture = chatService.getGroupChatMessagesAsync(
                chatItem.getId(),
                50
            );
        }

        messagesFuture
            .thenAccept(messages ->
                Platform.runLater(() -> {
                    currentMessages.addAll(messages);
                    displayMessages();
                })
            )
            .exceptionally(throwable -> {
                logger.warning(
                    "Failed to load messages: " + throwable.getMessage()
                );
                return null;
            });
    }

    private void displayMessages() {
        messagesContainer.getChildren().clear();

        String currentUsername = authService.getCurrentUser().getUsername();

        for (MessageDTO message : currentMessages) {
            Label messageLabel = createMessageLabel(message, currentUsername);
            messagesContainer.getChildren().add(messageLabel);
        }

        Platform.runLater(() -> {
            messagesScrollPane.setVvalue(1.0);
        });
    }

    private Label createMessageLabel(
        MessageDTO message,
        String currentUsername
    ) {
        Label messageLabel = new Label();

        boolean isOwnMessage = message
            .getSenderUsername()
            .equals(currentUsername);
        String displayText =
            message.getSenderUsername() + ": " + message.getContent();

        messageLabel.setText(displayText);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(400);

        if (isOwnMessage) {
            messageLabel.getStyleClass().addAll("message", "own-message");
        } else {
            messageLabel.getStyleClass().addAll("message", "other-message");
        }

        return messageLabel;
    }

    private void updateChatFilter() {
        String searchText = searchField.getText().toLowerCase().trim();
        Toggle selectedToggle = chatFilterGroup.getSelectedToggle();

        ChatFilter filter = ChatFilter.ALL;
        if (selectedToggle == directChatsFilter) {
            filter = ChatFilter.DIRECT;
        } else if (selectedToggle == groupChatsFilter) {
            filter = ChatFilter.GROUP;
        }

        final ChatFilter finalFilter = filter;

        filteredChats.setPredicate(chatItem -> {
            // Apply search filter
            boolean matchesSearch =
                searchText.isEmpty() ||
                chatItem.getName().toLowerCase().contains(searchText) ||
                chatItem.getPreview().toLowerCase().contains(searchText);

            // Apply type filter
            boolean matchesType = true;
            switch (finalFilter) {
                case DIRECT:
                    matchesType =
                        chatItem.getType() == ChatItemData.ChatType.DIRECT;
                    break;
                case GROUP:
                    matchesType =
                        chatItem.getType() == ChatItemData.ChatType.GROUP;
                    break;
                case ALL:
                default:
                    matchesType = true;
                    break;
            }

            return matchesSearch && matchesType;
        });
    }

    private void showEmptyState() {
        chatHeader.setVisible(false);
        messagesScrollPane.setVisible(false);
        messageInputArea.setVisible(false);
        emptyStateArea.setVisible(true);
    }

    private void showChatInterface() {
        emptyStateArea.setVisible(false);
        chatHeader.setVisible(true);
        messagesScrollPane.setVisible(true);
        messageInputArea.setVisible(true);
    }

    // Event handlers
    @FXML
    private void onLogout() {
        logger.info("User logging out");
        authService.logout();
        SceneManager.getInstance().showLogin();
    }

    @FXML
    private void onFilterChanged() {
        updateChatFilter();
    }

    @FXML
    private void onSearchChanged() {
        updateChatFilter();
    }

    @FXML
    private void onSendMessage() {
        String content = messageInput.getText().trim();
        if (content.isEmpty() || selectedChat == null) {
            return;
        }

        CompletableFuture<Void> sendFuture;

        if (selectedChat.getType() == ChatItemData.ChatType.DIRECT) {
            sendFuture = chatService.sendDirectMessageAsync(
                selectedChat.getId(),
                content
            );
        } else {
            sendFuture = chatService.sendGroupMessageAsync(
                selectedChat.getId(),
                content
            );
        }

        sendFuture
            .thenRun(() ->
                Platform.runLater(() -> {
                    messageInput.clear();
                    // Refresh messages
                    loadMessagesForChat(selectedChat);
                })
            )
            .exceptionally(throwable -> {
                logger.warning(
                    "Failed to send message: " + throwable.getMessage()
                );
                Platform.runLater(() -> {
                    // Show error to user
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Send Message Failed");
                    alert.setHeaderText("Could not send message");
                    alert.setContentText(throwable.getMessage());
                    alert.showAndWait();
                });
                return null;
            });
    }

    @FXML
    private void onMessageInputKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
            onSendMessage();
            event.consume();
        }
    }

    // Utility methods
    private String getInitials(String firstName, String lastName) {
        StringBuilder initials = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            initials.append(firstName.charAt(0));
        }
        if (lastName != null && !lastName.isEmpty()) {
            initials.append(lastName.charAt(0));
        }
        return initials.toString().toUpperCase();
    }

    private String formatTime(long timestamp) {
        if (timestamp == 0) return "";

        long now = System.currentTimeMillis();
        long diff = now - timestamp;

        if (diff < 60000) {
            // Less than 1 minute
            return "Just now";
        } else if (diff < 3600000) {
            // Less than 1 hour
            return (diff / 60000) + "m";
        } else if (diff < 86400000) {
            // Less than 1 day
            return (diff / 3600000) + "h";
        } else {
            return (diff / 86400000) + "d";
        }
    }
}
