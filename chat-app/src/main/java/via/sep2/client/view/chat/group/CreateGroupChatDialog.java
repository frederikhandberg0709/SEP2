package via.sep2.client.view.chat.group;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import via.sep2.client.factory.ServiceFactory;
import via.sep2.client.service.ChatService;
import via.sep2.shared.dto.ChatRoomDTO;

public class CreateGroupChatDialog {

    @FXML
    private TextField groupNameField;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private CheckBox privateCheckBox;

    @FXML
    private Spinner<Integer> maxMembersSpinner;

    @FXML
    private Button createButton;

    @FXML
    private Button cancelButton;

    @FXML
    private Label errorLabel;

    private Stage dialogStage;
    private ChatRoomDTO createdGroup = null;
    private ChatService chatService;

    @FXML
    private void initialize() {
        chatService = ServiceFactory.getInstance().getService(
                ChatService.class);

        maxMembersSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(2, 500, 50));

        setupValidation();

        createButton.setDisable(true);
    }

    private void setupValidation() {
        groupNameField
                .textProperty()
                .addListener((obs, oldVal, newVal) -> {
                    validateInput();
                    clearError();
                });

        descriptionArea
                .textProperty()
                .addListener((obs, oldVal, newVal) -> {
                    clearError();
                });
    }

    private void validateInput() {
        String groupName = groupNameField.getText().trim();
        boolean isValid = !groupName.isEmpty() &&
                groupName.length() >= 2 &&
                groupName.length() <= 50;
        createButton.setDisable(!isValid);
    }

    private void clearError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    @FXML
    private void handleCreate() {
        String groupName = groupNameField.getText().trim();
        String description = descriptionArea.getText().trim();
        boolean isPrivate = privateCheckBox.isSelected();
        int maxMembers = maxMembersSpinner.getValue();

        if (groupName.isEmpty()) {
            showError("Group name is required");
            return;
        }

        if (groupName.length() < 2) {
            showError("Group name must be at least 2 characters");
            return;
        }

        if (groupName.length() > 50) {
            showError("Group name must be less than 50 characters");
            return;
        }

        createButton.setDisable(true);
        createButton.setText("Creating...");

        chatService
                .createGroupChatAsync(groupName, description, isPrivate, maxMembers)
                .thenAccept(group -> {
                    javafx.application.Platform.runLater(() -> {
                        createdGroup = group;
                        closeDialog();
                    });
                })
                .exceptionally(throwable -> {
                    javafx.application.Platform.runLater(() -> {
                        showError(
                                "Failed to create group: " + throwable.getMessage());
                        createButton.setDisable(false);
                        createButton.setText("Create");
                    });
                    return null;
                });
    }

    @FXML
    private void handleCancel() {
        closeDialog();
    }

    private void closeDialog() {
        if (dialogStage != null) {
            dialogStage.close();
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public ChatRoomDTO getCreatedGroup() {
        return createdGroup;
    }
}
