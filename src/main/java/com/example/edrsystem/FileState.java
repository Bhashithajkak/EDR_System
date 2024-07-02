package com.example.edrsystem;

import java.nio.file.attribute.PosixFilePermission;
import java.util.Set;

public class FileState {
    String hash;
    long lastModified;
    long size;
    Set<PosixFilePermission> posixPermissions;
    boolean isReadOnly;
    long timestamp;

    public FileState(String hash, long lastModified, long size, Set<PosixFilePermission> posixPermissions, boolean isReadOnly) {
        this.hash = hash;
        this.lastModified = lastModified;
        this.size = size;
        this.posixPermissions = posixPermissions;
        this.isReadOnly = isReadOnly;
        this.timestamp = System.currentTimeMillis();
    }
}