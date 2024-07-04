package com.example.edrsystem.static_analysis;

public class FileAccessEvent {
    String eventType;
    long timestamp;

    public FileAccessEvent(String eventType, long timestamp) {
        this.eventType = eventType;
        this.timestamp = timestamp;
    }
}
