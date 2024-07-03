package com.example.edrsystem;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

public class FileMonitor {
    private final WatchService watchService;
    private final Map<WatchKey, Path> keys;
    private final boolean recursive;
    private final ExecutorService executorService;
    private static final Logger logger = Logger.getLogger(FileMonitor.class.getName());

    private final WhitelistManager whitelistManager;
    private final SuspicionScoreManager suspicionScoreManager;
    private final FileHashCalculator fileHashCalculator;
    private final FileEventHandler fileEventHandler;
    private final MalwareChecker malwareChecker;


    private static final int THREAD_POOL_SIZE = 10;
    private  static  final String PATH="D://Edr testing//";

    public FileMonitor() throws IOException, NoSuchAlgorithmException {
        this.watchService = FileSystems.getDefault().newWatchService();
        this.keys = new ConcurrentHashMap<>();
        this.recursive = true;
        this.executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        this.whitelistManager = new WhitelistManager();
        this.suspicionScoreManager = new SuspicionScoreManager();
        this.fileHashCalculator = new FileHashCalculator();
        this.fileEventHandler = new FileEventHandler(whitelistManager, suspicionScoreManager, fileHashCalculator);
        this.malwareChecker = new MalwareChecker();

        LoggerConfig.configureLogger(logger);
    }

    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDirectory(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDirectory(Path dir) throws IOException {
        WatchKey key = dir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);
        keys.put(key, dir);
    }

    public void startMonitoring() {
        try {
            Path monitoredDirectory = Paths.get(PATH);
            registerAll(monitoredDirectory);
            logger.info("Successfully registered directory: " + monitoredDirectory);

            while (true) {
                WatchKey key = watchService.take();
                Path dir = keys.get(key);
                if (dir == null) continue;

                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) {
                        logger.warning("OVERFLOW: Some events may have been lost or discarded.");
                        continue;
                    }

                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path name = ev.context();
                    Path child = dir.resolve(name);

                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                        fileEventHandler.handleFileEvent(child, "File Created");
                        if (recursive && Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                            registerAll(child);
                        }
                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        fileEventHandler.handleFileEvent(child, "File Modified");
                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                        fileEventHandler.handleDeleteEvent(child);
                    }
                }

                if (!key.reset()) {
                    keys.remove(key);
                    if (keys.isEmpty()) break;
                }
            }
        } catch (IOException | InterruptedException e) {
            logger.severe("Error in file monitoring: " + e.getMessage());
        } finally {
            executorService.shutdown();
        }
    }
    //This method is use for monitor all the files in the device

//    public void startMonitoring() {
//        try {
//            for (Path root : FileSystems.getDefault().getRootDirectories()) {
//                try {
//                    registerAll(root);
//                    logger.info("Successfully registered all directories under root: " + root);
//                } catch (AccessDeniedException e) {
//                    logger.warning("Access denied to root directory: " + root);
//                    // Continue with the next root directory
//                }
//            }
//
//            while (true) {
//                WatchKey key = watchService.take();
//                Path dir = keys.get(key);
//                if (dir == null) continue;
//
//                for (WatchEvent<?> event : key.pollEvents()) {
//                    WatchEvent.Kind<?> kind = event.kind();
//                    if (kind == StandardWatchEventKinds.OVERFLOW) {
//                        logger.warning("OVERFLOW: Some events may have been lost or discarded.");
//                        continue;
//                    }
//
//                    @SuppressWarnings("unchecked")
//                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
//                    Path name = ev.context();
//                    Path child = dir.resolve(name);
//
//                    if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
//                        handleFileEvent(child, "File Created");
//                        if (recursive && Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
//                            registerAll(child);
//                        }
//                    } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
//                        handleFileEvent(child, "File Modified");
//                    } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
//                        logger.info(String.format("File Deleted: %s", child));
//                        fileStates.remove(child);
//                        suspicionScores.remove(child);
//                    }
//                }
//
//                if (!key.reset()) {
//                    keys.remove(key);
//                    if (keys.isEmpty()) break;
//                }
//            }
//        } catch (IOException | InterruptedException e) {
//            logger.severe("Error in file monitoring: " + e.getMessage());
//        } finally {
//            executorService.shutdown();
//        }
//    }
}