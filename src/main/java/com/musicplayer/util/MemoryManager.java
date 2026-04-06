package com.musicplayer.util;
 
import java.io.*;
import java.util.HashMap;
import java.util.Map;
 
/**
 * NEW FILE
 * Manages a custom memory photo path per song.
 * User can set/change/remove at any time — NOT permanent.
 * Persisted to ~/.musicplayer_memories.txt across sessions.
 * Format: filePath|||photoPath
 */
public class MemoryManager {
 
    private static final String FILE_NAME = System.getProperty("user.home")
            + File.separator + ".musicplayer_memories.txt";
 
    private static MemoryManager instance;
    private final Map<String, String> memories = new HashMap<>(); // songPath → photoPat
 
    private MemoryManager() {
        load();
    }
 
    public static synchronized MemoryManager getInstance() {
        if (instance == null) instance = new MemoryManager();
        return instance;
    }
 
    /** Returns the memory photo path for this song, or null if none set. */
    public String getPhotoPath(String songFilePath) {
        return memories.get(songFilePath);
    }
 
    public boolean hasMemory(String songFilePath) {
        return memories.containsKey(songFilePath);
    }
 
    /** Set or update the memory photo for a song. Pass null to remove. */
    public void setPhoto(String songFilePath, String photoPath) {
        if (photoPath == null) memories.remove(songFilePath);
        else memories.put(songFilePath, photoPath);
        save();
    }
 
    public void removeMemory(String songFilePath) {
        memories.remove(songFilePath);
        save();
    }
 
    private void load() {
        try {
            File f = new File(FILE_NAME);
            if (!f.exists()) return;
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    int sep = line.indexOf("|||");
                    if (sep > 0) {
                        memories.put(line.substring(0, sep), line.substring(sep + 3));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("MemoryManager load error: " + e.getMessage());
        }
    }
 
    private void save() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Map.Entry<String, String> e : memories.entrySet()) {
                w.write(e.getKey() + "|||" + e.getValue());
                w.newLine();
            }
        } catch (Exception e) {
            System.err.println("MemoryManager save error: " + e.getMessage());
        }
    }
}