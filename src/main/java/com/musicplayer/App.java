package com.musicplayer;

import com.musicplayer.ui.MusicPlayerGUI;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Entry point. Launches the music player on the Swing Event Dispatch Thread.
 */
public class App {

    public static void main(String[] args) {
        // Try to use the system look-and-feel for native file dialogs
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Default Swing look is fine too
        }

        SwingUtilities.invokeLater(() -> {
            MusicPlayerGUI gui = new MusicPlayerGUI();
            gui.setVisible(true);
        });
    }
}
