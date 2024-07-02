package com.example.edrsystem;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class WhitelistManager {
    private final Set<String> whitelistedDirectories;
    private final Set<String> whitelistedProcesses;

    public WhitelistManager() {
        this.whitelistedDirectories = loadWhitelistedDirectories();
        this.whitelistedProcesses = loadWhitelistedProcesses();
    }

    private Set<String> loadWhitelistedDirectories() {
        Set<String> directories = new HashSet<>();
        directories.add("C:\\");
        directories.add("E:\\");
        directories.add("F:\\");
        return directories;
    }

    private Set<String> loadWhitelistedProcesses() {
        Set<String> processes = new HashSet<>();
        processes.add("svchost.exe");
        processes.add("explorer.exe");
        processes.add("chrome.exe");
        processes.add("msedge.exe");
        processes.add("mongod.exe");
        return processes;
    }

    public boolean isWhitelisted(Path file) {
        return whitelistedDirectories.stream().anyMatch(dir -> file.startsWith(dir)) ||
                whitelistedProcesses.contains(file.getFileName().toString().toLowerCase());
    }

    public boolean isCommonDataFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".log") ||
                fileName.endsWith(".tmp") ||
                fileName.endsWith(".cache") ||
                fileName.contains("quotamanager") ||
                fileName.contains("leveldb") ||
                fileName.endsWith(".ldb");
    }
}