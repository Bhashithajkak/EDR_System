package com.example.edrsystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

public class FileHashCalculator {
    private final MessageDigest md;
    private static final Logger logger = Logger.getLogger(FileHashCalculator.class.getName());
    private static final int MAX_FILE_SIZE_MB = 100;

    public FileHashCalculator() throws NoSuchAlgorithmException {
        this.md = MessageDigest.getInstance("SHA-256");
    }

    public String calculateHash(Path file) {
        try {
            if (Files.size(file) > MAX_FILE_SIZE_MB * 1024 * 1024) {
                logger.warning("File too large to hash: " + file);
                return "";
            }
            byte[] hash = md.digest(Files.readAllBytes(file));
            return bytesToHex(hash);
        } catch (IOException e) {
            logger.severe("Error calculating hash for file: " + file + ". Error: " + e.getMessage());
            return "";
        } catch (SecurityException e) {
            logger.severe("Security error calculating hash for file: " + file + ". Error: " + e.getMessage());
            return "";
        }
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}