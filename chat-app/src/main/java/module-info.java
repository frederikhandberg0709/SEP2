module via.sep2 {
    requires javafx.controls;
    requires javafx.fxml;
    requires lombok;
    requires java.sql;
    requires java.base;
    requires io.github.cdimascio.dotenv.java;

    opens via.sep2 to javafx.fxml;
    opens via.sep2.view.auth to javafx.fxml;

    exports via.sep2;
}
