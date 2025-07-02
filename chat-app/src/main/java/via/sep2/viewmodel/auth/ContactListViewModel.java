package via.sep2.viewmodel.auth;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import via.sep2.dao.ContactDAO;
import via.sep2.model.Contact;

import java.sql.SQLException;
import java.util.List;

public class ContactListViewModel {
    private final ObservableList<Contact> contacts = FXCollections.observableArrayList();
    private final ContactDAO contactDAO = new ContactDAO();

    public ObservableList<Contact> getContacts() {
        return contacts;
    }

    public void loadContacts(int userId) {
        try {
            List<Contact> contactList = contactDAO.getContactsForUser(userId);
            contacts.setAll(contactList);
        } catch (SQLException e) {
            e.printStackTrace();
            // Handle error
        }
    }
}