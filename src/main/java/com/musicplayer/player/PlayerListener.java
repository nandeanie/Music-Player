package com.musicplayer.player;
 
import com.musicplayer.model.Song;
 
/**
 * Interface that the UI implements to receive events from MusicPlayer.
 * Keeps the player engine decoupled from the Swing UI.
 */
public interface PlayerListener {
 
    /** Called when a new song starts (manually or auto-advance in playlist). */
    void onSongChanged(Song song);
 
    /** Called every millisecond to update the playback slider position. */
    void onSliderUpdate(int frame);
 
    /** Called when playback starts — UI should show pause button. */
    void onPlaybackStarted();
 
    /** Called when playback ends — UI should show play button. */
    void onPlaybackStopped();
}
 