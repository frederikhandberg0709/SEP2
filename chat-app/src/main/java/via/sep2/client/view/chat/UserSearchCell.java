package via.sep2.client.view.chat;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import via.sep2.shared.dto.UserDTO;

public class UserSearchCell extends ListCell<UserDTO> {

    private HBox container;
    private Label avatarLabel;
    private VBox infoContainer;
    private Label nameLabel;
    private Label usernameLabel;

    public UserSearchCell() {
        createComponents();
        layoutComponents();
        styleComponents();
    }

    private void createComponents() {
        container = new HBox();
        avatarLabel = new Label();
        infoContainer = new VBox();
        nameLabel = new Label();
        usernameLabel = new Label();
    }

    private void layoutComponents() {
        infoContainer.getChildren().addAll(nameLabel, usernameLabel);
        infoContainer.setSpacing(2);

        container.getChildren().addAll(avatarLabel, infoContainer);
        container.setSpacing(12);
        container.setAlignment(Pos.CENTER_LEFT);
        container.setPadding(new Insets(8, 12, 8, 12));
    }

    private void styleComponents() {
        avatarLabel.getStyleClass().add("user-avatar-small");
        avatarLabel.setMinSize(32, 32);
        avatarLabel.setMaxSize(32, 32);
        avatarLabel.setAlignment(Pos.CENTER);
        avatarLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        nameLabel.getStyleClass().add("user-display-name");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        usernameLabel.getStyleClass().add("user-username");
        usernameLabel.setFont(Font.font("System", 12));

        container.getStyleClass().add("user-search-item");
    }

    @Override
    protected void updateItem(UserDTO user, boolean empty) {
        super.updateItem(user, empty);

        if (empty || user == null) {
            setGraphic(null);
            return;
        }

        String initials = getInitials(user.getFirstName(), user.getLastName());
        avatarLabel.setText(initials);

        nameLabel.setText(user.getFirstName() + " " + user.getLastName());
        usernameLabel.setText("@" + user.getUsername());

        setGraphic(container);
    }

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
}
