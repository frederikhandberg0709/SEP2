module via.sep2 {
    requires javafx.controls;
    requires javafx.fxml;

    opens via.sep2 to javafx.fxml;
    exports via.sep2;
}
