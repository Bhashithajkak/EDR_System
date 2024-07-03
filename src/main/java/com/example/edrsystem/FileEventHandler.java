package com.example.edrsystem;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class FileEventHandler {
    private static final Logger logger = Logger.getLogger(FileEventHandler.class.getName());
    private final Map<Path, FileState> fileStates;
    private final Map<Path, List<FileAccessEvent>> fileAccessHistory;
    private final WhitelistManager whitelistManager;
    private final SuspicionScoreManager suspicionScoreManager;
    private final FileHashCalculator fileHashCalculator;
    private final MalwareChecker malwareChecker;
    private final boolean isPosixFileSystem;
    private static final int FILE_ACCESS_HISTORY_SIZE = 10;
    private static final int SUSPICION_THRESHOLD = 30;

    public FileEventHandler(WhitelistManager whitelistManager, SuspicionScoreManager suspicionScoreManager,
                            FileHashCalculator fileHashCalculator, MalwareChecker malwareChecker) {
        this.fileStates = new ConcurrentHashMap<>();
        this.fileAccessHistory = new ConcurrentHashMap<>();
        this.whitelistManager = whitelistManager;
        this.suspicionScoreManager = suspicionScoreManager;
        this.fileHashCalculator = fileHashCalculator;
        this.malwareChecker = malwareChecker;
        this.isPosixFileSystem = FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    }

    public void handleFileEvent(Path file, String eventType) {
        if (whitelistManager.isWhitelisted(file) || whitelistManager.isCommonDataFile(file)) {
            logger.fine("Whitelisted or common data file event: " + file);
            return;
        }

        try {
            if (!Files.isRegularFile(file)) return;

            FileState currentState = getCurrentFileState(file);
            FileState previousState = fileStates.get(file);

            logFileEvent(file, eventType);
            updateFileAccessHistory(file, eventType);

            if (previousState == null) {
                fileStates.put(file, currentState);
                checkForSharingOrExecution(file, currentState.posixPermissions, currentState.isReadOnly);
            } else {
                detectChanges(file, previousState, currentState);
            }

            if (isSuspiciousBehavior(file)) {

                // Trigger malware check or other actions
                logger.warning("Suspicious behavior detected for file: " + file);
                String hash = fileHashCalculator.calculateHash(file);
                malwareChecker.checkFileForMalware(file,hash);
            }

        } catch (IOException e) {
            logger.severe("Error handling file event for: " + file);
        }
    }

    public void handleDeleteEvent(Path file) {
        logger.info(String.format("File Deleted: %s", file));
        fileStates.remove(file);
        suspicionScoreManager.removeSuspicionScore(file);
    }

    private FileState getCurrentFileState(Path file) throws IOException {
        long lastModified = Files.getLastModifiedTime(file).toMillis();
        long size = Files.size(file);
        Set<PosixFilePermission> posixPermissions = null;
        boolean isReadOnly = false;

        if (isPosixFileSystem) {
            posixPermissions = Files.getPosixFilePermissions(file);
        } else {
            isReadOnly = !Files.isWritable(file);
        }

        return new FileState(fileHashCalculator.calculateHash(file), lastModified, size, posixPermissions, isReadOnly);
    }

    private void logFileEvent(Path file, String eventType) {
        logger.info(String.format("%s: %s", eventType, file));
    }

    private void updateFileAccessHistory(Path file, String eventType) {
        fileAccessHistory.computeIfAbsent(file, k -> new ArrayList<>())
                .add(new FileAccessEvent(eventType, System.currentTimeMillis()));
        if (fileAccessHistory.get(file).size() > FILE_ACCESS_HISTORY_SIZE) {
            fileAccessHistory.get(file).remove(0);
        }
    }

    private void detectChanges(Path file, FileState previousState, FileState currentState) {
        if (!previousState.hash.equals(currentState.hash)) {
            logger.info(String.format("File Modified: %s (New Hash: %s)", file, currentState.hash));
            suspicionScoreManager.updateSuspicionScore(file, "File Modified", 2);
        }
        if (isPosixFileSystem && !previousState.posixPermissions.equals(currentState.posixPermissions)) {
            logger.info(String.format("Permissions Changed: %s", file));
            suspicionScoreManager.updateSuspicionScore(file, "Permissions Changed", 3);
        } else if (!isPosixFileSystem && previousState.isReadOnly != currentState.isReadOnly) {
            logger.info(String.format("Read-only status changed: %s", file));
            suspicionScoreManager.updateSuspicionScore(file, "Read-only Changed", 2);
        }
        if (previousState.size != currentState.size) {
            logger.info(String.format("File Size Changed: %s (Old: %d, New: %d)", file, previousState.size, currentState.size));
            suspicionScoreManager.updateSuspicionScore(file, "Size Changed", 1);
        }
        fileStates.put(file, currentState);
    }

    private void checkForSharingOrExecution(Path file, Set<PosixFilePermission> permissions, boolean isReadOnly) {
        if (whitelistManager.isWhitelisted(file) || whitelistManager.isCommonDataFile(file)) {
            return;
        }

        if (isPosixFileSystem) {
            if (permissions.contains(PosixFilePermission.OWNER_EXECUTE) ||
                    permissions.contains(PosixFilePermission.GROUP_EXECUTE) ||
                    permissions.contains(PosixFilePermission.OTHERS_EXECUTE)) {
                logger.warning(String.format("Potential Execution Preparation: %s", file));
                suspicionScoreManager.updateSuspicionScore(file, "Potential Execution Preparation", 5);
            }
            if (permissions.contains(PosixFilePermission.OTHERS_READ) ||
                    permissions.contains(PosixFilePermission.OTHERS_WRITE)) {
                logger.warning(String.format("Potential Sharing Preparation: %s", file));
                suspicionScoreManager.updateSuspicionScore(file, "Potential Sharing Preparation", 4);
            }
        } else {
            try {
                if (Files.isExecutable(file)) {
                    logger.warning(String.format("Potential Execution Preparation: %s", file));
                    suspicionScoreManager.updateSuspicionScore(file, "Potential Execution Preparation", 5);
                }
                if (!isReadOnly) {
                    logger.warning(String.format("Potential Sharing Preparation: %s", file));
                    suspicionScoreManager.updateSuspicionScore(file, "Potential Sharing Preparation", 4);
                }
            } catch (SecurityException e) {
                logger.severe("Security exception when checking file permissions: " + file);
            }
        }

        if (isRecentlyDownloaded(file) && isPotentiallyExecutable(file)) {
            logger.warning("Potentially suspicious executable file: " + file);
            suspicionScoreManager.updateSuspicionScore(file, "Potential Execution Preparation", 6);
        } else if (isRecentlyDownloaded(file) && isPotentiallySensitive(file)) {
            logger.warning("Potentially sensitive file: " + file);
            suspicionScoreManager.updateSuspicionScore(file, "Potential Sharing Preparation", 5);
        }
    }

    private boolean isRecentlyDownloaded(Path file) {
        try {
            FileTime creationTime = (FileTime) Files.getAttribute(file, "creationTime");
            return Duration.between(creationTime.toInstant(), Instant.now()).toHours() < 24;
        } catch (IOException e) {
            logger.warning("Error checking file creation time: " + e.getMessage());
            return false;
        }
    }

    private boolean isPotentiallyExecutable(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".exe") || fileName.endsWith(".dll") || fileName.endsWith(".bat") || fileName.endsWith(".ps1");
    }

    private boolean isPotentiallySensitive(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.contains("password") || fileName.contains("credit") || fileName.contains("ssn");
    }

    private boolean isSuspiciousBehavior(Path file) {
        int suspicionScore = suspicionScoreManager.getSuspicionScore(file);
        if (suspicionScore > SUSPICION_THRESHOLD) {
            return true;
        }

        List<FileAccessEvent> accessEvents = fileAccessHistory.getOrDefault(file, Collections.emptyList());
        if (accessEvents.size() >= 3) {
            long timeWindow = 5000; // 5 seconds
            long lastEventTime = accessEvents.get(accessEvents.size() - 1).timestamp;
            int rapidAccessCount = 0;
            for (int i = accessEvents.size() - 2; i >= 0; i--) {
                if (lastEventTime - accessEvents.get(i).timestamp <= timeWindow) {
                    rapidAccessCount++;
                } else {
                    break;
                }
            }
            if (rapidAccessCount >= 2) {
                logger.warning(String.format("Rapid file access detected: %s", file));
                suspicionScoreManager.updateSuspicionScore(file, "Rapid Access", 5);
                return true;
            }
        }

        return false;
    }
}