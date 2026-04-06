package com.musicplayer.util;
 
import com.musicplayer.util.MemoryManager;
import com.musicplayer.util.PlayStatsManager;
import com.musicplayer.util.Theme;
 
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
 
/**
 * NEW FILE
 * Stats screen — shows most-played song with its memory photo at top.
 * Gen-Z aesthetic: big photo, pixel font, pink accents.
 */
public class StatsDialog extends JDialog {
 
    public StatsDialog(JFrame parent) {
        super(parent, "✨ Your Stats", true);
        setSize(320, 420);
        setResizable(false);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.dialogBackground());
        setLayout(new BorderLayout(0, 0));
 
        String mostPlayed = PlayStatsManager.getInstance().getMostPlayedPath();
        int count = PlayStatsManager.getInstance().getMostPlayedCount();
 
        // Header
        JLabel header = new JLabel("your most played 🎵", SwingConstants.CENTER);
        header.setFont(new Font("Dialog", Font.BOLD, 15));
        header.setForeground(Theme.accent());
        header.setBorder(new EmptyBorder(18, 10, 8, 10));
        add(header, BorderLayout.NORTH);
 
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(Theme.dialogBackground());
        center.setBorder(new EmptyBorder(0, 20, 10, 20));
 
        if (mostPlayed == null) {
            JLabel none = new JLabel("no plays tracked yet :(", SwingConstants.CENTER);
            none.setFont(Theme.LCD_FONT_BOLD);
            none.setForeground(Theme.textMuted());
            none.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(Box.createVerticalGlue());
            center.add(none);
            center.add(Box.createVerticalGlue());
        } else {
            // Memory photo
            String photoPath = MemoryManager.getInstance().getPhotoPath(mostPlayed);
            JLabel photoLabel = new JLabel();
            photoLabel.setPreferredSize(new Dimension(240, 200));
            photoLabel.setMaximumSize(new Dimension(240, 200));
            photoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            photoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            photoLabel.setBackground(Theme.panelBackground());
            photoLabel.setOpaque(true);
            photoLabel.setBorder(BorderFactory.createLineBorder(Theme.accentMuted(), 2));
 
            if (photoPath != null) {
                try {
                    BufferedImage img = ImageIO.read(new File(photoPath));
                    if (img != null) {
                        Image scaled = img.getScaledInstance(240, 200, Image.SCALE_SMOOTH);
                        photoLabel.setIcon(new ImageIcon(scaled));
                    }
                } catch (Exception ignored) {}
            }
            if (photoLabel.getIcon() == null) {
                photoLabel.setText("📷 no memory photo");
                photoLabel.setFont(Theme.FONT_SMALL);
                photoLabel.setForeground(Theme.textMuted());
            }
 
            // Song name
            String songName = new File(mostPlayed).getName().replace(".mp3", "");
            JLabel nameLabel = new JLabel(songName, SwingConstants.CENTER);
            nameLabel.setFont(Theme.LCD_TITLE);
            nameLabel.setForeground(Theme.lcdText());
            nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            nameLabel.setBorder(new EmptyBorder(12, 0, 4, 0));
 
            JLabel countLabel = new JLabel("played " + count + " time" + (count == 1 ? "" : "s"), SwingConstants.CENTER);
            countLabel.setFont(new Font("Dialog", Font.BOLD, 13));
            countLabel.setForeground(Theme.accent());
            countLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
 
            center.add(photoLabel);
            center.add(nameLabel);
            center.add(countLabel);
        }
 
        add(center, BorderLayout.CENTER);
 
        // Close button
        JButton close = styledButton("close");
        close.addActionListener(e -> dispose());
        JPanel south = new JPanel();
        south.setBackground(Theme.dialogBackground());
        south.setBorder(new EmptyBorder(0, 0, 14, 0));
        south.add(close);
        add(south, BorderLayout.SOUTH);
    }
 
    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_BUTTON);
        btn.setBackground(Theme.buttonBackground());
        btn.setForeground(Theme.buttonText());
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(100, 30));
        return btn;
    }
}
 