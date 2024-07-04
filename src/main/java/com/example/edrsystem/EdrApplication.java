package com.example.edrsystem;

import com.example.edrsystem.dynamic_analysis.ExtractPacketFeatures;
import com.example.edrsystem.static_analysis.FileMonitor;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

public class EdrApplication extends Application {
    private static final String PATH = "D://Edr testing//";
    private static final int INTERFACE = 4;
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(EdrApplication.class.getResource("hello-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 320, 240);
        stage.setTitle("Hello!");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        try {
            FileMonitor fileMonitor = new FileMonitor(PATH);
            ExtractPacketFeatures extractPacketFeatures = new ExtractPacketFeatures();
            new Thread(fileMonitor::startMonitoring).start();
            new Thread(() -> {
                try {
                    extractPacketFeatures.runPacketMonitor(INTERFACE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }
}