module com.example.edrsystem {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    requires org.apache.httpcomponents.httpcore;
    requires org.apache.httpcomponents.httpclient;
    requires commons.logging;
    requires com.google.common;
    requires org.json;
    requires com.sun.jna;
    requires com.sun.jna.platform;


    opens com.example.edrsystem to javafx.fxml;
    exports com.example.edrsystem;
}