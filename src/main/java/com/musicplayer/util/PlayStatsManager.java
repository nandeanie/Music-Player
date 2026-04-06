package com.musicplayer.util;
 
import java.io.*;
import java.util.*;
 
/**
 * NEW FILE
 * Tracks how many times each song has been played.
 * Persisted to ~/.musicplayer_stats.txt
 * Format: filePath|||count
 */
public class PlayStatsManager {
 
    private static final String FILE_NAME = System.getProperty("user.home")
            + File.separator + ".musicplayer_stats.txt";
 
    private static PlayStatsManager instance;
    private final Map<String, Integer> playCounts = new HashMap<>();
 
    private PlayStatsManager() {
        load();
    }
 
    public static synchronized PlayStatsManager getInstance() {
        if (instance == null) instance = new PlayStatsManager();
        return instance;
    }
 
    public void increment(String filePath) {
        playCounts.merge(filePath, 1, Integer::sum);
        save();
    }
 
    public int getCount(String filePath) {
        return playCounts.getOrDefault(filePath, 0);
    }
 
    /** Returns the file path of the most played song, or null if no data. */
    public String getMostPlayedPath() {
        return playCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
    }
 
    public int getMostPlayedCount() {
        return playCounts.values().stream().max(Integer::compareTo).orElse(0);
    }
 
    private void load() {
        try {
            File f = new File(FILE_NAME);
            if (!f.exists()) return;
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    int sep = line.lastIndexOf("|||");
                    if (sep > 0) {
                        try {
                            String path = line.substring(0, sep);
                            int count = Integer.parseInt(line.substring(sep + 3).trim());
                            playCounts.put(path, count);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("PlayStatsManager load error: " + e.getMessage());
        }
    }
 
    private void save() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Map.Entry<String, Integer> e : playCounts.entrySet()) {
                w.write(e.getKey() + "|||" + e.getValue());
                w.newLine();
            }
        } catch (Exception e) {
            System.err.println("PlayStatsManager save error: " + e.getMessage());
        }
    }
}