package com.musicplayer.util;
 
import java.io.*;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Set;
 
/**
 * NEW FILE
 * Manages favorite songs — persisted to favorites.txt in user's home dir.
 * Thread-safe singleton.
 */
public class FavoritesManager {
 
    private static final String FILE_NAME = System.getProperty("user.home")
            + File.separator + ".musicplayer_favorites.txt";
 
    private static FavoritesManager instance;
    private final Set<String> favorites = new HashSet<>();
 
    private FavoritesManager() {
        load();
    }
 
    public static synchronized FavoritesManager getInstance() {
        if (instance == null) instance = new FavoritesManager();
        return instance;
    }
 
    public boolean isFavorite(String filePath) {
        return favorites.contains(filePath);
    }
 
    public void toggle(String filePath) {
        if (favorites.contains(filePath)) favorites.remove(filePath);
        else favorites.add(filePath);
        save();
    }
 
    public void add(String filePath) {
        favorites.add(filePath);
        save();
    }
 
    public void remove(String filePath) {
        favorites.remove(filePath);
        save();
    }
 
    private void load() {
        try {
            File f = new File(FILE_NAME);
            if (!f.exists()) return;
            try (BufferedReader r = new BufferedReader(new FileReader(f))) {
                String line;
                while ((line = r.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) favorites.add(line);
                }
            }
        } catch (Exception e) {
            System.err.println("FavoritesManager load error: " + e.getMessage());
        }
    }
 
    private void save() {
        try (BufferedWriter w = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (String path : favorites) {
                w.write(path);
                w.newLine();
            }
        } catch (Exception e) {
            System.err.println("FavoritesManager save error: " + e.getMessage());
        }
    }
}
 
