package com.musicplayer.model;
 
import com.mpatric.mp3agic.Mp3File;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
 
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import javax.imageio.ImageIO;
 
/**
 * Represents a single MP3 song with its metadata and embedded album art.
 *
 * EDITABLE:
 *   - In loadMetadata(), you can add more tag fields (e.g., ALBUM, YEAR)
 *     by following the same pattern as title/artist.
 */
public class Song {
 
    private final String filePath;
    private String title;
    private String artist;
    private String duration;
    private Mp3File mp3File;
    private double frameRatePerMillisecond;
    private Image albumArt; // null if no embedded art
 
    public Song(String filePath) {
        this.filePath = filePath;
        loadMetadata();
    }
 
    private void loadMetadata() {
        try {
            mp3File = new Mp3File(filePath);
            frameRatePerMillisecond = (double) mp3File.getFrameCount() / mp3File.getLengthInMilliseconds();
            duration = formatDuration(mp3File.getLengthInSeconds());
 
            AudioFile audioFile = AudioFileIO.read(new File(filePath));
            Tag tag = audioFile.getTag();
 
            if (tag != null) {
                title  = tag.getFirst(FieldKey.TITLE);
                artist = tag.getFirst(FieldKey.ARTIST);
 
                // Load embedded album art
                Artwork artwork = tag.getFirstArtwork();
                if (artwork != null && artwork.getBinaryData() != null) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(artwork.getBinaryData());
                    BufferedImage img = ImageIO.read(bais);
                    if (img != null) {
                        albumArt = img;
                    }
                }
            }
 
            // Fallbacks
            if (title  == null || title.isBlank())  title  = new File(filePath).getName().replace(".mp3", "");
            if (artist == null || artist.isBlank()) artist = "Unknown Artist";
 
        } catch (Exception e) {
            System.err.println("Could not load metadata: " + filePath);
            title    = new File(filePath).getName().replace(".mp3", "");
            artist   = "Unknown Artist";
            duration = "00:00";
        }
    }
 
    private String formatDuration(long totalSeconds) {
        long min = totalSeconds / 60;
        long sec = totalSeconds % 60;
        return String.format("%02d:%02d", min, sec);
    }
 
    // ── Getters ───────────────────────────────────────────────────────────────
 
    public String getFilePath()                   { return filePath; }
    public String getTitle()                      { return title; }
    public String getArtist()                     { return artist; }
    public String getDuration()                   { return duration; }
    public Mp3File getMp3File()                   { return mp3File; }
    public double getFrameRatePerMillisecond()    { return frameRatePerMillisecond; }
    public Image getAlbumArt()                    { return albumArt; } // null if not available
 
    @Override
    public String toString() {
        return title + " — " + artist;
    }
}
 