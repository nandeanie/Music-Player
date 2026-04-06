package com.musicplayer.player;
 
import com.musicplayer.model.Song;
import javazoom.jl.player.advanced.AdvancedPlayer;
import javazoom.jl.player.advanced.PlaybackEvent;
import javazoom.jl.player.advanced.PlaybackListener;
 
import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
 
/**
 * Core music playback engine.
 * Handles play, pause, resume, next, previous, shuffle, repeat, volume, and playlist loading.
 * Notifies the UI via PlayerListener callbacks.
 *
 * EDITABLE:
 *   - DEFAULT_VOLUME  → starting volume (0-100)
 */
public class MusicPlayer extends PlaybackListener {
 
    private static final int DEFAULT_VOLUME = 70; // EDITABLE: change starting volume
 
    private static final Object playSignal = new Object();
 
    private final PlayerListener listener;
 
    private Song currentSong;
    private List<Song> playlist;
    private List<Song> shuffledPlaylist; // used when shuffle is on
    private int playlistIndex;
 
    private AdvancedPlayer advancedPlayer;
    private boolean isPaused;
    private boolean songFinished;
    private boolean pressedNext;
    private boolean pressedPrev;
 
    private int currentFrame;
    private int currentTimeMs;
 
    // ── Feature flags ─────────────────────────────────────────────────────────
    private boolean shuffleOn  = false;
    private boolean repeatOne  = false; // repeat current song
    private boolean repeatAll  = false; // repeat whole playlist
    private int volume         = DEFAULT_VOLUME; // 0–100
 
    public MusicPlayer(PlayerListener listener) {
        this.listener = listener;
    }
 
    // ─── Public API ──────────────────────────────────────────────────────────
 
    public void loadSong(Song song) {
        currentSong = song;
        playlist = null;
        shuffledPlaylist = null;
 
        if (!songFinished) stopSong();
 
        if (currentSong != null) {
            resetPlayback();
            playCurrent();
        }
    }
 
    public void loadPlaylist(File playlistFile) {
        playlist = new ArrayList<>();
 
        try (BufferedReader reader = new BufferedReader(new FileReader(playlistFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    playlist.add(new Song(line));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load playlist: " + e.getMessage());
        }
 
        if (!playlist.isEmpty()) {
            playlistIndex = 0;
            rebuildShuffleList();
            currentSong = getActivePlaylist().get(0);
            resetPlayback();
            listener.onSongChanged(currentSong);
            playCurrent();
        }
    }
    public void loadM3uPlaylist(File m3uFile) {
        playlist = new ArrayList<>();
 
        try (BufferedReader reader = new BufferedReader(new FileReader(m3uFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    playlist.add(new Song(line));
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load M3U playlist: " + e.getMessage());
        }
 
        if (!playlist.isEmpty()) {
            playlistIndex = 0;
            rebuildShuffleList();
            currentSong = getActivePlaylist().get(0);
            resetPlayback();
            listener.onSongChanged(currentSong);
            playCurrent();
        }
    } 
    public void pauseSong() {
        if (advancedPlayer != null) {
            isPaused = true;
            stopSong();
        }
    }
 
    public void stopSong() {
        if (advancedPlayer != null) {
            advancedPlayer.stop();
            advancedPlayer.close();
            advancedPlayer = null;
        }
    }
 
    public void playCurrent() {
        if (currentSong == null) return;
 
        try {
            FileInputStream fis = new FileInputStream(currentSong.getFilePath());
            BufferedInputStream bis = new BufferedInputStream(fis);
 
            advancedPlayer = new AdvancedPlayer(bis);
            advancedPlayer.setPlayBackListener(this);
 
            startPlaybackThread();
            startSliderThread();
 
        } catch (Exception e) {
            System.err.println("Playback error: " + e.getMessage());
        }
    }
 
    public void nextSong() {
        List<Song> active = getActivePlaylist();
        if (active == null || playlistIndex + 1 > active.size() - 1) return;
 
        pressedNext = true;
        if (!songFinished) stopSong();
 
        playlistIndex++;
        currentSong = active.get(playlistIndex);
        resetPlayback();
        listener.onSongChanged(currentSong);
        playCurrent();
    }
 
    public void prevSong() {
        List<Song> active = getActivePlaylist();
        if (active == null || playlistIndex - 1 < 0) return;
 
        pressedPrev = true;
        if (!songFinished) stopSong();
 
        playlistIndex--;
        currentSong = active.get(playlistIndex);
        resetPlayback();
        listener.onSongChanged(currentSong);
        playCurrent();
    }
 
    // ─── Shuffle ──────────────────────────────────────────────────────────────
 
    public void toggleShuffle() {
        shuffleOn = !shuffleOn;
        rebuildShuffleList();
        playlistIndex = 0; // reset index when toggling
    }
 
    public boolean isShuffleOn() {
        return shuffleOn;
    }
 
    private void rebuildShuffleList() {
        if (playlist == null) return;
        shuffledPlaylist = new ArrayList<>(playlist);
        if (shuffleOn) {
            Collections.shuffle(shuffledPlaylist);
        }
    }
 
    private List<Song> getActivePlaylist() {
        if (playlist == null) return null;
        return shuffleOn ? shuffledPlaylist : playlist;
    }
 
    // ─── Repeat ───────────────────────────────────────────────────────────────
 
    /**
     * Cycles through: OFF → REPEAT ONE → REPEAT ALL → OFF
     * Returns current state as string for UI label.
     */
    public String cycleRepeat() {
        if (!repeatOne && !repeatAll) {
            repeatOne = true;
            repeatAll = false;
            return "ONE";
        } else if (repeatOne) {
            repeatOne = false;
            repeatAll = true;
            return "ALL";
        } else {
            repeatOne = false;
            repeatAll = false;
            return "OFF";
        }
    }
 
    public boolean isRepeatOne() { return repeatOne; }
    public boolean isRepeatAll() { return repeatAll; }
 
    // ─── Volume ───────────────────────────────────────────────────────────────
 
    /**
     * Sets volume 0–100.
     * Uses JavaSound FloatControl on the current output line.
     * NOTE: Only works when audio is playing through a SourceDataLine.
     * With pure JLayer AdvancedPlayer this may have no effect — see README.
     */
    public void setVolume(int vol) {
        this.volume = vol;
        applyVolumeToLine();
    }
 
    public int getVolume() {
        return volume;
    }
 
    private void applyVolumeToLine() {
        try {
            // Get the current active mixer lines and apply gain
            Mixer.Info[] mixers = AudioSystem.getMixerInfo();
            for (Mixer.Info info : mixers) {
                Mixer mixer = AudioSystem.getMixer(info);
                Line[] lines = mixer.getSourceLines();
                for (Line line : lines) {
                    if (line.isOpen() && line instanceof SourceDataLine) {
                        FloatControl gainControl =
                            (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                        // Convert 0–100 to dB range (typically -80 to 6)
                        float min = gainControl.getMinimum();
                        float max = gainControl.getMaximum();
                        float gain = min + ((max - min) * (volume / 100.0f));
                        gainControl.setValue(gain);
                    }
                }
            }
        } catch (Exception e) {
            // Volume control not available for this audio line — silently ignore
        }
    }
 
    // ─── Setters used by slider ───────────────────────────────────────────────
 
    public void setCurrentFrame(int frame)  { this.currentFrame = frame; }
    public void setCurrentTimeMs(int ms)    { this.currentTimeMs = ms; }
    public Song getCurrentSong()            { return currentSong; }
    public List<Song> getPlaylist()         { return playlist; }
 
    // ─── Private helpers ──────────────────────────────────────────────────────
 
    private void resetPlayback() {
        currentFrame = 0;
        currentTimeMs = 0;
        listener.onSliderUpdate(0);
    }
 
    private void startPlaybackThread() {
        new Thread(() -> {
            try {
                if (isPaused) {
                    synchronized (playSignal) {
                        isPaused = false;
                        playSignal.notify();
                    }
                    advancedPlayer.play(currentFrame, Integer.MAX_VALUE);
                } else {
                    advancedPlayer.play();
                }
                applyVolumeToLine();
            } catch (Exception e) {
                System.err.println("Playback thread error: " + e.getMessage());
            }
        }).start();
    }
 
    private void startSliderThread() {
        new Thread(() -> {
            if (isPaused) {
                try {
                    synchronized (playSignal) { playSignal.wait(); }
                } catch (Exception e) {
                    System.err.println("Slider wait error: " + e.getMessage());
                }
            }
 
            while (!isPaused && !songFinished && !pressedNext && !pressedPrev) {
                try {
                    currentTimeMs++;
                    int frame = (int) (currentTimeMs * 2.08 * currentSong.getFrameRatePerMillisecond());
                    listener.onSliderUpdate(frame);
                    Thread.sleep(1);
                } catch (Exception e) {
                    System.err.println("Slider thread error: " + e.getMessage());
                }
            }
        }).start();
    }
 
    // ─── PlaybackListener callbacks ───────────────────────────────────────────
 
    @Override
    public void playbackStarted(PlaybackEvent evt) {
        songFinished = false;
        pressedNext  = false;
        pressedPrev  = false;
        listener.onPlaybackStarted();
    }
 
    @Override
    public void playbackFinished(PlaybackEvent evt) {
        if (isPaused) {
            currentFrame += (int) (evt.getFrame() * currentSong.getFrameRatePerMillisecond());
            return;
        }
 
        if (pressedNext || pressedPrev) return;
 
        songFinished = true;
 
        // Repeat one — restart same song
        if (repeatOne) {
            resetPlayback();
            playCurrent();
            return;
        }
 
        List<Song> active = getActivePlaylist();
 
        if (active == null) {
            listener.onPlaybackStopped();
            return;
        }
 
        // End of playlist
        if (playlistIndex == active.size() - 1) {
            if (repeatAll) {
                // Loop back to start
                playlistIndex = 0;
                currentSong = active.get(0);
                resetPlayback();
                listener.onSongChanged(currentSong);
                playCurrent();
            } else {
                listener.onPlaybackStopped();
            }
        } else {
            nextSong();
        }
    }
}
 