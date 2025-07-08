package via.sep2.client.view.chat;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class ConfirmationDialog {

    @FXML
    private Label titleLabel;

    @FXML
    private Label messageLabel;

    @FXML
    private Button confirmButton;

    @FXML
    private Button cancelButton;

    private Stage dialogStage;
    private boolean confirmed = false;

    @FXML
    private void initialize() {
        confirmButton.getStyleClass().add("danger-button");
    }

    public void setContent(String title, String message, String confirmText) {
        titleLabel.setText(title);
        messageLabel.setText(message);
        confirmButton.setText(confirmText);
    }

    public void setDangerAction(boolean isDanger) {
        if (isDanger) {
            confirmButton.getStyleClass().add("danger-button");
        } else {
            confirmButton.getStyleClass().remove("danger-button");
        }
    }

    @FXML
    private void handleConfirm() {
        confirmed = true;
        closeDialog();
    }

    @FXML
    private void handleCancel() {
        confirmed = false;
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

    public boolean isConfirmed() {
        return confirmed;
    }
}
