package via.sep2.view.auth;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import via.sep2.model.Contact;
import via.sep2.viewmodel.auth.ContactListViewModel;

public class ContactListController {
    @FXML
    private ListView<Contact> contactListView;

    private final ContactListViewModel viewModel = new ContactListViewModel();

    public void initialize() {
        contactListView.setItems(viewModel.getContacts());
        // Optionally set a cell factory for custom display
    }

    public void loadContactsForUser(int userId) {
        viewModel.loadContacts(userId);
    }
}
