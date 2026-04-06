package com.musicplayer.util;
 
import java.io.*;
import java.util.HashMap;
import java.util.Map;
 
/**
 * NEW FILE
 * Manages personal notes per song — persisted to ~/.musicplayer_notes.txt
 * Format: filePath|||note text
 */
public class SongNotesManager {
 
    private static final String FILE_NAME = System.getProperty("user.home")
            + File.separator + ".musicplayer_notes.txt";
 
    private static SongNotesManager instance;
    private final Map<String, String> notes = new HashMap<>();
 
    private SongNotesManager() {
        load();
    }
 
    public static synchronized SongNotesManager getInstance() {
        if (instance == null) instance = new SongNotesManager();
        return instance;
    }
 
    public String getNote(String filePath) {
        return notes.getOrDefault(filePath, "");
    }
 
    public boolean hasNote(String filePath) {
        return notes.containsKey(filePath) && !notes.get(filePath).isBlank();
    }
 
    public void setNote(String filePath, String note) {
        if (note == null || note.isBlank()) notes.remove(filePath);
        else notes.put(filePath, note.trim());
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
                        String path = line.substring(0, sep);
                        String note = line.substring(sep + 3);
                        notes.put(path, note);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("SongNotesManager load error: " + e.getMessage());
        }
    }
 
    private void save() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (Map.Entry<String, String> e : notes.entrySet()) {
                // escape newlines in note text
                String safeNote = e.getValue().replace("\n", "\\n");
                w.write(e.getKey() + "|||" + safeNote);
                w.newLine();
            }
        } catch (Exception e) {
            System.err.println("SongNotesManager save error: " + e.getMessage());
        }
    }
}
 