package com.musicplayer.ui;

import com.musicplayer.util.Theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Dialog that lets the user pick MP3 files, review the list,
 * and save them as a .txt playlist file.
 */
public class PlaylistDialog extends JDialog {

    private final List<String> songPaths = new ArrayList<>();
    private final JPanel songListPanel;

    public PlaylistDialog(JFrame parent) {
        super(parent, "Create Playlist", true);

        setSize(420, 420);
        setResizable(false);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.BACKGROUND);
        setLayout(new BorderLayout(10, 10));

        // Song list area (scrollable)
        songListPanel = new JPanel();
        songListPanel.setLayout(new BoxLayout(songListPanel, BoxLayout.Y_AXIS));
        songListPanel.setBackground(Theme.SURFACE);

        JScrollPane scrollPane = new JScrollPane(songListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(Theme.SURFACE);
        scrollPane.setBorder(new EmptyBorder(8, 8, 8, 8));
        add(scrollPane, BorderLayout.CENTER);

        // Button row at the bottom
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setBackground(Theme.BACKGROUND);

        JButton addButton = styledButton("+ Add Song");
        JButton saveButton = styledButton("Save Playlist");

        addButton.addActionListener(e -> addSong(parent));
        saveButton.addActionListener(e -> savePlaylist());

        buttonPanel.add(addButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void addSong(JFrame parent) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("MP3 Files", "mp3"));
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION && chooser.getSelectedFile() != null) {
            String path = chooser.getSelectedFile().getPath();
            songPaths.add(path);

            JLabel label = new JLabel(chooser.getSelectedFile().getName());
            label.setForeground(Theme.TEXT_PRIMARY);
            label.setFont(Theme.FONT_SMALL);
            label.setBorder(new EmptyBorder(4, 6, 4, 6));
            songListPanel.add(label);
            songListPanel.revalidate();
        }
    }

    private void savePlaylist() {
        if (songPaths.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No songs added yet.", "Empty Playlist", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = chooser.showSaveDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            // Ensure .txt extension
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }

            try {
                file.createNewFile();
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                    for (String path : songPaths) {
                        writer.write(path);
                        writer.newLine();
                    }
                }
                JOptionPane.showMessageDialog(this, "Playlist saved successfully!");
                dispose();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Failed to save playlist: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_SMALL);
        btn.setBackground(Theme.ACCENT);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
}
