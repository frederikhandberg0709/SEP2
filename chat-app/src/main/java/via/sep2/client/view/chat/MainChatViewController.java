package via.sep2.client.view.chat;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import via.sep2.client.util.SceneManager;
import via.sep2.client.viewmodel.chat.MainChatViewModel;
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

    @FXML
    private VBox userSearchContainer;

    @FXML
    private ListView<UserDTO> userSearchListView;

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

    private MainChatViewModel viewModel;

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
                        "Starting chat with user: " + selectedUser.getUsername()
                    );
                    viewModel.startDirectChatWithUser(selectedUser);
                }
            }
        });

        logger.info(
            "Initial userSearchContainer visibility: " +
            userSearchContainer.isVisible()
        );
        logger.info(
            "Initial userSearchContainer managed: " +
            userSearchContainer.isManaged()
        );
        logger.info(
            "Initial chatListView visibility: " + chatListView.isVisible()
        );

        viewModel
            .showUserSearchResultsProperty()
            .addListener((obs, oldVal, newVal) -> {
                logger.info(
                    "User search results visibility changed from " +
                    oldVal +
                    " to " +
                    newVal
                );
                logger.info(
                    "Setting userSearchContainer visible: " +
                    newVal +
                    ", managed: " +
                    newVal
                );
                logger.info(
                    "Setting chatListView visible: " +
                    !newVal +
                    ", managed: " +
                    !newVal
                );

                userSearchContainer.setVisible(newVal);
                userSearchContainer.setManaged(newVal);
                chatListView.setVisible(!newVal);
                chatListView.setManaged(!newVal);

                Platform.runLater(() -> {
                    logger.info(
                        "After change - userSearchContainer visible: " +
                        userSearchContainer.isVisible() +
                        ", managed: " +
                        userSearchContainer.isManaged()
                    );
                    logger.info(
                        "After change - chatListView visible: " +
                        chatListView.isVisible() +
                        ", managed: " +
                        chatListView.isManaged()
                    );
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
                                " users to search results"
                            );
                            for (UserDTO user : change.getAddedSubList()) {
                                logger.info(
                                    "User: " +
                                    user.getUsername() +
                                    " (" +
                                    user.getFirstName() +
                                    " " +
                                    user.getLastName() +
                                    ")"
                                );
                            }

                            logger.info(
                                "showUserSearchResults property value: " +
                                viewModel.showUserSearchResultsProperty().get()
                            );
                        }
                    }
                }
            );

        logger.info(
            "Initial showUserSearchResults value: " +
            viewModel.showUserSearchResultsProperty().get()
        );
    }

    private void setupUI() {
        setupChatList();
        setupMessageDisplay();
        setupFilters();
    }

    private void setupChatList() {
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
                            // handle message removal if needed
                        }
                    }

                    Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
                }
            );
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
                    "'"
                );
            });
    }

    private void loadUserInfo() {
        UserDTO currentUser = viewModel.getCurrentUser();
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
        Label messageLabel = createMessageLabel(message);
        messagesContainer.getChildren().add(messageLabel);
    }

    private Label createMessageLabel(MessageDTO message) {
        Label messageLabel = new Label();
        UserDTO currentUser = viewModel.getCurrentUser();

        boolean isOwnMessage = message
            .getSenderUsername()
            .equals(currentUser.getUsername());
        String displayText;

        if (isOwnMessage) {
            displayText = message.getContent();
        } else {
            displayText =
                message.getSenderUsername() + ": " + message.getContent();
        }

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

    // Event handlers
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
