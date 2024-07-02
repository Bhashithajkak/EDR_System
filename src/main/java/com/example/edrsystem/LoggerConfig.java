package com.example.edrsystem;

import java.io.IOException;
import java.util.logging.*;

public class LoggerConfig {
    public static void configureLogger(Logger logger) {
        try {
            FileHandler fileHandler = new FileHandler("edr_system.log", true);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}