package via.sep2.client.view.chat.group;

import java.util.Optional;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import via.sep2.client.factory.ServiceFactory;
import via.sep2.client.service.AuthService;
import via.sep2.client.service.ChatService;
import via.sep2.shared.dto.ChatMemberDTO;
import via.sep2.shared.dto.ChatRoomDTO;
import via.sep2.shared.dto.MemberRole;

public class ManageGroupDialog {

    @FXML
    private Label groupNameLabel;

    @FXML
    private TextField editGroupNameField;

    @FXML
    private Button updateNameButton;

    @FXML
    private TextField addUserField;

    @FXML
    private Button addUserButton;

    @FXML
    private ListView<ChatMemberDTO> membersListView;

    @FXML
    private Button closeButton;

    @FXML
    private Label errorLabel;

    private Stage dialogStage;
    private ChatRoomDTO groupChat;
    private ChatService chatService;
    private AuthService authService;
    private ObservableList<ChatMemberDTO> members;
    private String currentUsername;
    private MemberRole currentUserRole;

    @FXML
    private void initialize() {
        chatService = ServiceFactory.getInstance().getService(
                ChatService.class);
        authService = ServiceFactory.getInstance().getService(
                AuthService.class);
        currentUsername = authService.getCurrentUser().getUsername();

        members = FXCollections.observableArrayList();
        membersListView.setItems(members);

        setupMemberListView();
        setupValidation();
    }

    private void setupMemberListView() {
        membersListView.setCellFactory(listView -> new MemberListCell());
    }

    private void setupValidation() {
        addUserField
                .textProperty()
                .addListener((obs, oldVal, newVal) -> {
                    addUserButton.setDisable(newVal.trim().isEmpty());
                    clearError();
                });

        editGroupNameField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean hasChanged = groupChat != null && !newVal.trim().equals(groupChat.getName());
            boolean isValid = !newVal.trim().isEmpty() && newVal.trim().length() >= 2;
            updateNameButton.setDisable(!hasChanged || !isValid);
            clearError();
        });
    }

    public void setGroupChat(ChatRoomDTO groupChat) {
        this.groupChat = groupChat;
        groupNameLabel.setText(groupChat.getName());
        editGroupNameField.setText(groupChat.getName());
        loadMembers();
    }

    private void loadMembers() {
        chatService
                .getGroupChatMembersAsync(groupChat.getId())
                .thenAccept(memberList -> {
                    Platform.runLater(() -> {
                        members.clear();
                        members.addAll(memberList);

                        currentUserRole = memberList.stream()
                                .filter(member -> member.getUsername().equals(currentUsername))
                                .map(ChatMemberDTO::getRole)
                                .findFirst()
                                .orElse(MemberRole.MEMBER);

                        updateUIPermissions();
                    });
                })
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        showError(
                                "Failed to load members: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    private void updateUIPermissions() {
        boolean canManage = canManageGroup();
        addUserField.setDisable(!canManage);
        addUserButton.setDisable(!canManage || addUserField.getText().trim().isEmpty());
        editGroupNameField.setDisable(!canManage);
        updateNameButton.setDisable(!canManage);
    }

    @FXML
    private void handleUpdateName() {
        String newName = editGroupNameField.getText().trim();
        if (newName.isEmpty() || newName.length() < 2) {
            showError("Group name must be at least 2 characters");
            return;
        }

        if (newName.equals(groupChat.getName())) {
            showError("Please enter a different name");
            return;
        }

        updateNameButton.setDisable(true);
        updateNameButton.setText("Updating...");

        chatService.updateGroupNameAsync(groupChat.getId(), newName)
                .thenRun(() -> Platform.runLater(() -> {
                    groupChat.setName(newName);
                    groupNameLabel.setText(newName);
                    updateNameButton.setDisable(false);
                    updateNameButton.setText("Update");
                    showError("Group name updated successfully!");
                }))
                .exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        updateNameButton.setDisable(false);
                        updateNameButton.setText("Update");
                        showError("Failed to update group name: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void handleAddUser() {
        String username = addUserField.getText().trim();
        if (username.isEmpty()) {
            showError("Username is required");
            return;
        }

        boolean userExists = members
                .stream()
                .anyMatch(member -> member.getUsername().equals(username));

        if (userExists) {
            showError("User is already in the group");
            return;
        }

        addUserButton.setDisable(true);
        addUserButton.setText("Adding...");

        chatService.addUserToGroupAsync(groupChat.getId(), username)
                .thenRun(() -> Platform.runLater(() -> {
                    addUserField.clear();
                    addUserButton.setDisable(false);
                    addUserButton.setText("Add User");
                    loadMembers(); // Refresh the list
                })).exceptionally(throwable -> {
                    Platform.runLater(() -> {
                        addUserButton.setDisable(false);
                        addUserButton.setText("Add User");
                        showError("Failed to add user: " + throwable.getMessage());
                    });
                    return null;
                });
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    private boolean canManageGroup() {
        return currentUserRole == MemberRole.CREATOR || currentUserRole == MemberRole.ADMIN;
    }

    private boolean canManageMembers() {
        return members
                .stream()
                .filter(member -> member.getUsername().equals(currentUsername))
                .findFirst()
                .map(ChatMemberDTO::canManageRoom)
                .orElse(false);
    }

    private class MemberListCell extends ListCell<ChatMemberDTO> {

        private HBox container;
        private Label usernameLabel;
        private ComboBox<MemberRole> roleComboBox;
        private Button removeButton;

        public MemberListCell() {
            container = new HBox(10);
            usernameLabel = new Label();
            roleComboBox = new ComboBox<>();
            removeButton = new Button("Remove");

            usernameLabel.setStyle("-fx-font-weight: bold;");
            roleComboBox.getItems().addAll(MemberRole.MEMBER, MemberRole.ADMIN);
            removeButton.getStyleClass().add("danger-button");

            HBox.setHgrow(usernameLabel, Priority.ALWAYS);
            container
                    .getChildren()
                    .addAll(usernameLabel, roleComboBox, removeButton);

            // Setup event handlers
            roleComboBox.setOnAction(e -> handleRoleChange());
            removeButton.setOnAction(e -> handleRemoveMember());
        }

        @Override
        protected void updateItem(ChatMemberDTO member, boolean empty) {
            super.updateItem(member, empty);

            if (empty || member == null) {
                setGraphic(null);
                return;
            }

            usernameLabel.setText(member.getUsername());

            roleComboBox.setValue(member.getRole());

            boolean isCreator = member.getRole() == MemberRole.CREATOR;
            boolean canManage = canManageMembers();
            boolean isSelf = member.getUsername().equals(currentUsername);

            roleComboBox.setDisable(isCreator || !canManage || isSelf);
            removeButton.setDisable(isCreator || !canManage || isSelf);

            // Creator is not allowed to change role
            if (isCreator) {
                roleComboBox.setVisible(false);
                Label creatorLabel = new Label("CREATOR");
                creatorLabel.setStyle(
                        "-fx-text-fill: #ff6b35; -fx-font-weight: bold;");
                container.getChildren().set(1, creatorLabel);
            } else {
                roleComboBox.setVisible(true);
                if (container.getChildren().size() > 1 &&
                        !(container.getChildren().get(1) instanceof ComboBox)) {
                    container.getChildren().set(1, roleComboBox);
                }
            }

            setGraphic(container);
        }

        private void handleRoleChange() {
            ChatMemberDTO member = getItem();
            if (member == null)
                return;

            MemberRole newRole = roleComboBox.getValue();
            if (newRole == member.getRole())
                return;

            if (newRole == MemberRole.ADMIN) {
                chatService
                        .promoteToAdminAsync(
                                member.getUsername(),
                                groupChat.getId())
                        .thenRun(() -> Platform.runLater(() -> loadMembers()))
                        .exceptionally(throwable -> {
                            Platform.runLater(() -> {
                                showError(
                                        "Failed to promote user: " +
                                                throwable.getMessage());
                                roleComboBox.setValue(member.getRole()); // Revert
                            });
                            return null;
                        });
            } else if (newRole == MemberRole.MEMBER) {
                chatService
                        .demoteFromAdminAsync(
                                member.getUsername(),
                                groupChat.getId())
                        .thenRun(() -> Platform.runLater(() -> loadMembers()))
                        .exceptionally(throwable -> {
                            Platform.runLater(() -> {
                                showError(
                                        "Failed to demote user: " +
                                                throwable.getMessage());
                                roleComboBox.setValue(member.getRole()); // Revert
                            });
                            return null;
                        });
            }
        }

        private void handleRemoveMember() {
            ChatMemberDTO member = getItem();
            if (member == null)
                return;

            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Remove Member");
            alert.setHeaderText(
                    "Remove " + member.getUsername() + " from group?");
            alert.setContentText("This action cannot be undone.");

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                removeButton.setDisable(true);
                removeButton.setText("Removing...");

                chatService.removeUserFromGroupAsync(groupChat.getId(), member.getUsername())
                        .thenRun(() -> Platform.runLater(() -> {
                            removeButton.setDisable(false);
                            removeButton.setText("Remove");
                            loadMembers(); // Refresh the list
                            showError("User removed successfully!");
                        }))
                        .exceptionally(throwable -> {
                            Platform.runLater(() -> {
                                removeButton.setDisable(false);
                                removeButton.setText("Remove");
                                showError("Failed to remove user: " + throwable.getMessage());
                            });
                            return null;
                        });
            }
        }
    }
}
