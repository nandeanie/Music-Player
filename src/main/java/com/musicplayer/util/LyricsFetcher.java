package com.musicplayer.util;
 
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
 
/**
 * Fetches song lyrics from lyrics.ovh (free API, no key needed).
 *
 * API format: https://api.lyrics.ovh/v1/{artist}/{title}
 *
 * EDITABLE:
 *   - BASE_URL → swap to a different lyrics API if needed
 */
public class LyricsFetcher {
 
    // EDITABLE: swap API base URL here if you want a different provider
    private static final String BASE_URL = "https://api.lyrics.ovh/v1/";
 
    /**
     * Fetches lyrics for the given artist and title.
     * Runs on the calling thread — always call from a background thread, not EDT.
     *
     * @return lyrics string, or an error message if not found
     */
    public static String fetchLyrics(String artist, String title) {
        if (artist == null || artist.isBlank() || title == null || title.isBlank()) {
            return "No artist/title info available to search lyrics.";
        }
 
        try {
            String encodedArtist = URLEncoder.encode(artist.trim(), StandardCharsets.UTF_8);
            String encodedTitle  = URLEncoder.encode(title.trim(),  StandardCharsets.UTF_8);
            String urlString     = BASE_URL + encodedArtist + "/" + encodedTitle;
 
            HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Accept", "application/json");
 
            int status = conn.getResponseCode();
 
            if (status == 200) {
                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)
                );
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                return parseLyricsFromJson(response.toString());
            } else {
                return "Lyrics not found for: " + title + " by " + artist;
            }
 
        } catch (Exception e) {
            return "Could not fetch lyrics. Check your internet connection.";
        }
    }
 
    /**
     * Simple JSON parser — pulls out the "lyrics" field without a JSON library.
     * lyrics.ovh returns: {"lyrics":"line1\nline2\n..."}
     */
    private static String parseLyricsFromJson(String json) {
        int start = json.indexOf("\"lyrics\":\"");
        if (start == -1) return "Lyrics not found.";
 
        start += 10; // skip past "lyrics":"
        int end = json.lastIndexOf("\"");
        if (end <= start) return "Lyrics not found.";
 
        return json.substring(start, end)
                   .replace("\\n", "\n")
                   .replace("\\r", "")
                   .replace("\\\"", "\"")
                   .trim();
    }
}
