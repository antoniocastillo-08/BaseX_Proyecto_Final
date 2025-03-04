module org.example.basexfinal {
    requires javafx.controls;
    requires javafx.fxml;
    requires basex;


    opens org.example.basexfinal to javafx.fxml;
    exports org.example.basexfinal;
}