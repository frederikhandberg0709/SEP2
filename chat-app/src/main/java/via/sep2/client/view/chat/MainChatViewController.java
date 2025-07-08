package via.sep2.client.view.chat;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import via.sep2.client.util.SceneManager;
import via.sep2.client.view.chat.group.CreateGroupChatDialog;
import via.sep2.client.view.chat.group.ManageGroupDialog;
import via.sep2.client.viewmodel.chat.MainChatViewModel;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.MessageDTO;
import via.sep2.shared.dto.UserDTO;

public class MainChatViewController implements Initializable {

    private static final Logger logger = Logger.getLogger(
            MainChatViewController.class.getName());

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

    @FXML
    private VBox userSearchContainer;

    @FXML
    private ListView<UserDTO> userSearchListView;

    @FXML
    private Button createGroupButton;

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
    private MenuButton chatOptionsButton;

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

    private MainChatViewModel viewModel;

    private Map<Integer, MessageLabel> messageLabels;

    public MainChatViewController() {
        this.messageLabels = new HashMap<>();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        logger.info("Initializing MainChatViewController with ViewModel");

        initializeViewModel();
        setupDataBinding();
        setupUI();
        setupEventHandlers();

        loadUserInfo();
        viewModel.loadChats();
    }

    private void initializeViewModel() {
        this.viewModel = new MainChatViewModel();
    }

    private void setupDataBinding() {
        searchField
                .textProperty()
                .bindBidirectional(viewModel.searchTextProperty());

        messageInput
                .textProperty()
                .bindBidirectional(viewModel.messageInputProperty());

        chatListView.setItems(viewModel.getFilteredChats());

        setupUserSearchList();

        chatHeader.managedProperty().bind(chatHeader.visibleProperty());
        messagesScrollPane
                .managedProperty()
                .bind(messagesScrollPane.visibleProperty());
        messageInputArea
                .managedProperty()
                .bind(messageInputArea.visibleProperty());
        emptyStateArea.managedProperty().bind(emptyStateArea.visibleProperty());

        chatOptionsButton
                .visibleProperty()
                .bind(viewModel.showChatOptionsProperty());
        chatOptionsButton
                .managedProperty()
                .bind(viewModel.showChatOptionsProperty());

        showEmptyState();
    }

    private void setupUserSearchList() {
        userSearchListView.setItems(viewModel.getSearchedUsers());
        userSearchListView.setCellFactory(listView -> new UserSearchCell());

        userSearchListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                UserDTO selectedUser = userSearchListView
                        .getSelectionModel()
                        .getSelectedItem();
                if (selectedUser != null) {
                    logger.info(
                            "Starting chat with user: " + selectedUser.getUsername());
                    viewModel.startDirectChatWithUser(selectedUser);
                }
            }
        });

        logger.info(
                "Initial userSearchContainer visibility: " +
                        userSearchContainer.isVisible());
        logger.info(
                "Initial userSearchContainer managed: " +
                        userSearchContainer.isManaged());
        logger.info(
                "Initial chatListView visibility: " + chatListView.isVisible());

        viewModel
                .showUserSearchResultsProperty()
                .addListener((obs, oldVal, newVal) -> {
                    logger.info(
                            "User search results visibility changed from " +
                                    oldVal +
                                    " to " +
                                    newVal);
                    logger.info(
                            "Setting userSearchContainer visible: " +
                                    newVal +
                                    ", managed: " +
                                    newVal);
                    logger.info(
                            "Setting chatListView visible: " +
                                    !newVal +
                                    ", managed: " +
                                    !newVal);

                    userSearchContainer.setVisible(newVal);
                    userSearchContainer.setManaged(newVal);
                    chatListView.setVisible(!newVal);
                    chatListView.setManaged(!newVal);

                    Platform.runLater(() -> {
                        logger.info(
                                "After change - userSearchContainer visible: " +
                                        userSearchContainer.isVisible() +
                                        ", managed: " +
                                        userSearchContainer.isManaged());
                        logger.info(
                                "After change - chatListView visible: " +
                                        chatListView.isVisible() +
                                        ", managed: " +
                                        chatListView.isManaged());
                    });
                });

        viewModel
                .getSearchedUsers()
                .addListener(
                        (javafx.collections.ListChangeListener<UserDTO>) change -> {
                            while (change.next()) {
                                if (change.wasAdded()) {
                                    logger.info(
                                            "Added " +
                                                    change.getAddedSubList().size() +
                                                    " users to search results");
                                    for (UserDTO user : change.getAddedSubList()) {
                                        logger.info(
                                                "User: " +
                                                        user.getUsername() +
                                                        " (" +
                                                        user.getFirstName() +
                                                        " " +
                                                        user.getLastName() +
                                                        ")");
                                    }

                                    logger.info(
                                            "showUserSearchResults property value: " +
                                                    viewModel.showUserSearchResultsProperty().get());
                                }
                            }
                        });

        logger.info(
                "Initial showUserSearchResults value: " +
                        viewModel.showUserSearchResultsProperty().get());
    }

    private void setupUI() {
        setupChatList();
        setupMessageDisplay();
        setupFilters();
        setupCreateGroupButton();
        setupChatOptionsMenu();
    }

    private void setupChatList() {
        chatListView.setCellFactory(
                new Callback<ListView<ChatItemData>, ListCell<ChatItemData>>() {
                    @Override
                    public ListCell<ChatItemData> call(
                            ListView<ChatItemData> listView) {
                        return new ChatItemCell();
                    }
                });

        chatListView
                .getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        viewModel.selectChat(newSelection);
                        updateChatHeader(newSelection);
                        showChatInterface();
                    }
                });
    }

    private void setupMessageDisplay() {
        viewModel
                .getCurrentMessages()
                .addListener(
                        (javafx.collections.ListChangeListener<MessageDTO>) change -> {
                            while (change.next()) {
                                if (change.wasAdded()) {
                                    for (MessageDTO message : change.getAddedSubList()) {
                                        addMessageToUI(message);
                                    }
                                } else if (change.wasRemoved()) {
                                }
                            }

                            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
                        });
    }

    private void setupFilters() {
        chatFilterGroup
                .selectedToggleProperty()
                .addListener((obs, oldToggle, newToggle) -> {
                    if (newToggle == allChatsFilter) {
                        viewModel.setFilter(MainChatViewModel.ChatFilter.ALL);
                    } else if (newToggle == directChatsFilter) {
                        viewModel.setFilter(MainChatViewModel.ChatFilter.DIRECT);
                    } else if (newToggle == groupChatsFilter) {
                        viewModel.setFilter(MainChatViewModel.ChatFilter.GROUP);
                    }
                });
    }

    private void setupCreateGroupButton() {
        createGroupButton.setOnAction(e -> showCreateGroupDialog());
    }

    private void setupChatOptionsMenu() {
        chatOptionsButton.getItems().clear();

        // Listen for role changes to update menu
        viewModel
                .currentUserRoleProperty()
                .addListener((obs, oldVal, newVal) -> {
                    updateChatOptionsMenu();
                });
    }

    private void updateChatOptionsMenu() {
        chatOptionsButton.getItems().clear();

        if (viewModel.getSelectedChat() == null ||
                viewModel.getSelectedChat().getType() != ChatItemData.ChatType.GROUP) {
            return;
        }

        if (viewModel.canManageGroup()) {
            MenuItem manageItem = new MenuItem("Manage Group");
            manageItem.setOnAction(e -> showManageGroupDialog());
            chatOptionsButton.getItems().add(manageItem);
        }

        if (viewModel.canLeaveGroup()) {
            MenuItem leaveItem = new MenuItem("Leave Group");
            leaveItem.setOnAction(e -> confirmLeaveGroup());
            chatOptionsButton.getItems().add(leaveItem);
        }

        if (viewModel.canDeleteGroup()) {
            MenuItem deleteItem = new MenuItem("Delete Group");
            deleteItem.setOnAction(e -> confirmDeleteGroup());
            chatOptionsButton.getItems().add(deleteItem);
        }
    }

    private void setupEventHandlers() {
        messageInput.setOnAction(e -> viewModel.sendMessage());

        viewModel
                .errorMessageProperty()
                .addListener((obs, oldVal, newVal) -> {
                    if (newVal != null && !newVal.isEmpty()) {
                        showErrorAlert(newVal);
                    }
                });

        chatListView.setOnMouseClicked(event -> {
            if (viewModel.showUserSearchResultsProperty().get()) {
                viewModel.clearSearch();
            }
        });

        viewModel
                .searchTextProperty()
                .addListener((obs, oldVal, newVal) -> {
                    logger.info(
                            "Search text changed from '" +
                                    oldVal +
                                    "' to '" +
                                    newVal +
                                    "'");
                });
    }

    private void showCreateGroupDialog() {
        try {
            Stage dialogStage = SceneManager.getInstance().showCreateGroupDialog();
            if (dialogStage == null) {
                showErrorAlert(("Failed to open create group dialog"));
                return;
            }

            CreateGroupChatDialog controller = SceneManager.getInstance()
                    .getController(SceneManager.CREATE_GROUP_DIALOG);

            if (controller != null) {
                controller.setDialogStage(dialogStage);
            }

            dialogStage.showAndWait();

            if (controller != null) {
                ChatRoomDTO createdGroup = controller.getCreatedGroup();
                if (createdGroup != null) {
                    viewModel.loadChats();
                }
            }
        } catch (Exception e) {
            logger.severe(
                    "Failed to load create group dialog: " + e.getMessage());
            e.printStackTrace();
            showErrorAlert("Failed to open create group dialog");
        }
    }

    private void showManageGroupDialog() {
        if (viewModel.getSelectedChat() == null)
            return;

        try {
            String groupName = viewModel.getSelectedChat().getName();
            Stage dialogStage = SceneManager.getInstance().showManageGroupDialog(groupName);
            if (dialogStage == null) {
                showErrorAlert("Failed to open manage group dialog");
                return;
            }

            ManageGroupDialog controller = SceneManager.getInstance().getController(SceneManager.MANAGE_GROUP_DIALOG);

            viewModel
                    .getGroupMembers(viewModel.getSelectedChat().getId())
                    .thenAccept(members -> {
                        Platform.runLater(() -> {
                            ChatRoomDTO groupChat = new ChatRoomDTO(
                                    viewModel.getSelectedChat().getId(),
                                    viewModel.getSelectedChat().getName(),
                                    "", // creator
                                    System.currentTimeMillis(),
                                    viewModel.getSelectedChat().getPreview(),
                                    false,
                                    100);

                            controller.setGroupChat(groupChat);

                            controller.setDialogStage(dialogStage);
                            dialogStage.show();
                        });
                    })
                    .exceptionally(throwable -> {
                        Platform.runLater(() -> {
                            showErrorAlert("Failed to load group information");
                        });
                        return null;
                    });
        } catch (Exception e) {
            logger.severe(
                    "Failed to load manage group dialog: " + e.getMessage());
            showErrorAlert("Failed to open manage group dialog");
        }
    }

    private void confirmLeaveGroup() {
        if (viewModel.getSelectedChat() == null)
            return;

        showConfirmationDialog(
                "Leave Group",
                "Are you sure you want to leave \"" +
                        viewModel.getSelectedChat().getName() +
                        "\"?\n\nYou won't be able to see new messages or rejoin unless invited again.",
                "Leave Group",
                true,
                () -> leaveGroup());
    }

    private void confirmDeleteGroup() {
        if (viewModel.getSelectedChat() == null)
            return;

        showConfirmationDialog(
                "Delete Group",
                "Are you sure you want to delete \"" +
                        viewModel.getSelectedChat().getName() +
                        "\"?\n\nThis action cannot be undone and all messages will be permanently deleted.",
                "Delete Group",
                true,
                () -> deleteGroup());
    }

    private void leaveGroup() {
        if (viewModel.getSelectedChat() == null)
            return;

        viewModel
                .leaveGroup(viewModel.getSelectedChat().getId())
                .thenRun(() -> Platform.runLater(() -> {
                    showEmptyState();
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showErrorAlert(
                                "Failed to leave group: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void deleteGroup() {
        // TODO: implement a deleteGroup method in ChatService
        showErrorAlert("Group deletion functionality is not yet implemented.");
    }

    private void showConfirmationDialog(
            String title,
            String message,
            String confirmText,
            boolean isDanger,
            Runnable onConfirm) {
        try {
            Stage dialogStage = SceneManager.getInstance().showConfirmationDialog(title);
            if (dialogStage == null) {
                showErrorAlert("Failed to open confirmation dialog");
                return;
            }

            ConfirmationDialog controller = SceneManager.getInstance().getController(SceneManager.CONFIRMATION_DIALOG);

            if (controller != null) {
                controller.setDialogStage(dialogStage);
                controller.setContent(title, message, confirmText);
                controller.setDangerAction(isDanger);
            }

            dialogStage.showAndWait();

            if (controller != null && controller.isConfirmed() && onConfirm != null) {
                onConfirm.run();
            }

        } catch (Exception e) {
            logger.severe("Failed to show confirmation dialog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadUserInfo() {
        UserDTO currentUser = viewModel.getCurrentUser();
        if (currentUser != null) {
            userNameLabel.setText(
                    currentUser.getFirstName() + " " + currentUser.getLastName());
            userAvatarLabel.setText(
                    getInitials(
                            currentUser.getFirstName(),
                            currentUser.getLastName()));
        }
    }

    private void updateChatHeader(ChatItemData chatItem) {
        chatHeaderName.setText(chatItem.getName());
        chatHeaderAvatar.setText(chatItem.getAvatarText());

        if (chatItem.getType() == ChatItemData.ChatType.DIRECT) {
            chatHeaderStatus.setText("Direct message");
        } else {
            chatHeaderStatus.setText("Group chat");
        }
    }

    private void addMessageToUI(MessageDTO message) {
        String currentUsername = viewModel.getCurrentUser().getUsername();

        MessageLabel messageLabel = new MessageLabel(
                message,
                currentUsername,
                this::handleEditMessage,
                this::handleDeleteMessage);

        messageLabels.put(message.getId(), messageLabel);
        messagesContainer.getChildren().add(messageLabel);
    }

    private void handleEditMessage(Integer messageId, String newContent) {
        viewModel.editMessage(messageId, newContent);
    }

    private void handleDeleteMessage(Integer messageId) {
        viewModel.deleteMessage(messageId);
    }

    private void showEmptyState() {
        chatHeader.setVisible(false);
        messagesScrollPane.setVisible(false);
        messageInputArea.setVisible(false);
        emptyStateArea.setVisible(true);

        messagesContainer.getChildren().clear();
    }

    private void showChatInterface() {
        emptyStateArea.setVisible(false);
        chatHeader.setVisible(true);
        messagesScrollPane.setVisible(true);
        messageInputArea.setVisible(true);

        messagesContainer.getChildren().clear();
        for (MessageDTO message : viewModel.getCurrentMessages()) {
            addMessageToUI(message);
        }

        Platform.runLater(() -> messageInput.requestFocus());
    }

    private void showErrorAlert(String errorMessage) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("An error occurred");
            alert.setContentText(errorMessage);
            alert.showAndWait();
        });
    }

    @FXML
    private void onLogout() {
        logger.info("User logging out");
        viewModel.cleanup();
        SceneManager.getInstance().showLogin();
    }

    @FXML
    private void onFilterChanged() {
        // Filter change is handled by setupFilters()
    }

    @FXML
    private void onSearchChanged() {
        // Search change is handled by data binding
    }

    @FXML
    private void onSendMessage() {
        viewModel.sendMessage();
    }

    @FXML
    private void onMessageInputKeyPressed(KeyEvent event) {
        if (event.getCode() == KeyCode.ENTER && !event.isShiftDown()) {
            viewModel.sendMessage();
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

    public MainChatViewModel getViewModel() {
        return viewModel;
    }
}
