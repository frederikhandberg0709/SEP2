package via.sep2.client.view.chat;

import javafx.fxml.FXML;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import via.sep2.shared.dto.UserDTO;

public class ContactListView {
    @FXML
    private ListView<UserDTO> contactListView;

    @FXML
    public void initialize() {
        contactListView.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(UserDTO user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                } else {
                    setText(user.getFirstName() + " " + user.getLastName() + " (" +
                            user.getUsername() + ")");
                }
            }
        });

        // for testing
        contactListView.getItems().addAll(
                new UserDTO(1, "alice123", "Alice", "Andersen"),
                new UserDTO(2, "bobby", "Bob", "Berg"));
    }
}
