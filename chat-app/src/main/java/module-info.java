module via.sep2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;
    requires java.sql;
    requires java.base;
    requires java.rmi;
    requires io.github.cdimascio.dotenv.java;

    requires java.logging;

    exports via.sep2.server.rmi to java.rmi;
    exports via.sep2.client.rmi to java.rmi;
    exports via.sep2.shared.dto to java.rmi;
    exports via.sep2.shared.interfaces to java.rmi;

    exports via.sep2.client.connection;

    exports via.sep2.client.event;
    exports via.sep2.client.event.events;

    exports via.sep2.client.state;

    exports via.sep2.client.service;

    exports via.sep2.client.command;

    exports via.sep2.client.factory;

    exports via.sep2 to javafx.fxml, javafx.graphics;
    exports via.sep2.client.view.auth to javafx.fxml;
    exports via.sep2.client.view.chat to javafx.fxml;
    exports via.sep2.client.viewmodel.auth to javafx.fxml;
    exports via.sep2.client.viewmodel.chat to javafx.fxml;

    opens via.sep2 to javafx.fxml;
    opens via.sep2.client.view.auth to javafx.fxml;
    opens via.sep2.client.viewmodel.auth to javafx.fxml;
    opens via.sep2.client.view.chat to javafx.fxml;

    opens via.sep2.shared.dto to java.rmi;

    opens via.sep2.client.factory;
}
