package com.example.edrsystem.static_analysis;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SuspicionScoreManager {
    private final Map<Path, Integer> suspicionScores;

    public SuspicionScoreManager() {
        this.suspicionScores = new ConcurrentHashMap<>();
    }

    public void updateSuspicionScore(Path file, String eventType, int scoreIncrement) {
        int currentScore = suspicionScores.getOrDefault(file, 0);
        suspicionScores.put(file, Math.min(currentScore + scoreIncrement, 100));
    }

    public int getSuspicionScore(Path file) {
        return suspicionScores.getOrDefault(file, 0);
    }

    public void removeSuspicionScore(Path file) {
        suspicionScores.remove(file);
    }
}