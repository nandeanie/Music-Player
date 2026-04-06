package com.musicplayer.util;
 
import com.musicplayer.util.SleepTimer;
import com.musicplayer.util.Theme;
 
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
 
/**
 * NEW FILE
 * Sleep Timer dialog — presets (15/30/45/60) + custom input field.
 * Shows live countdown once started.
 */
public class SleepTimerDialog extends JDialog {
 
    private final SleepTimer sleepTimer;
    private JLabel countdownLabel;
    private JButton startBtn;
    private JButton cancelBtn;
    private JTextField customField;
    private boolean timerRunning = false;
 
    public SleepTimerDialog(JFrame parent, SleepTimer existingTimer) {
        super(parent, "⏰ Sleep Timer", true);
        this.sleepTimer = existingTimer;
        setSize(300, 320);
        setResizable(false);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.dialogBackground());
        setLayout(new BorderLayout(0, 10));
 
        // Title
        JLabel title = new JLabel("sleep timer 😴", SwingConstants.CENTER);
        title.setFont(new Font("Dialog", Font.BOLD, 15));
        title.setForeground(Theme.accent());
        title.setBorder(new EmptyBorder(16, 0, 6, 0));
        add(title, BorderLayout.NORTH);
 
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setBackground(Theme.dialogBackground());
        center.setBorder(new EmptyBorder(0, 24, 0, 24));
 
        // Preset buttons row
        JLabel presetLabel = new JLabel("quick pick:");
        presetLabel.setFont(Theme.FONT_SMALL);
        presetLabel.setForeground(Theme.textMuted());
        presetLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(presetLabel);
        center.add(Box.createVerticalStrut(6));
 
        JPanel presets = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        presets.setBackground(Theme.dialogBackground());
        presets.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (int min : new int[]{15, 30, 45, 60}) {
            JButton b = presetButton(min + "m");
            final int m = min;
            b.addActionListener(e -> startCountdown(m));
            presets.add(b);
        }
        center.add(presets);
        center.add(Box.createVerticalStrut(14));
 
        // Custom input
        JLabel customLabel = new JLabel("or set custom (minutes):");
        customLabel.setFont(Theme.FONT_SMALL);
        customLabel.setForeground(Theme.textMuted());
        customLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(customLabel);
        center.add(Box.createVerticalStrut(4));
 
        customField = new JTextField("20");
        customField.setFont(Theme.LCD_FONT_BOLD);
        customField.setForeground(Theme.lcdText());
        customField.setBackground(Theme.inputBackground());
        customField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.accentMuted(), 1),
                new EmptyBorder(4, 8, 4, 8)));
        customField.setMaximumSize(new Dimension(120, 32));
        customField.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(customField);
        center.add(Box.createVerticalStrut(10));
 
        startBtn = styledButton("start timer");
        startBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        startBtn.addActionListener(e -> {
            try {
                int mins = Integer.parseInt(customField.getText().trim());
                if (mins < 1 || mins > 360) {
                    JOptionPane.showMessageDialog(this, "Enter 1–360 minutes.", "Invalid", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                startCountdown(mins);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter a valid number.", "Invalid", JOptionPane.WARNING_MESSAGE);
            }
        });
        center.add(startBtn);
        center.add(Box.createVerticalStrut(10));
 
        // Countdown display
        countdownLabel = new JLabel("", SwingConstants.LEFT);
        countdownLabel.setFont(Theme.LCD_TITLE);
        countdownLabel.setForeground(Theme.lcdText());
        countdownLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        center.add(countdownLabel);
 
        add(center, BorderLayout.CENTER);
 
        // Bottom buttons
        cancelBtn = styledButton("cancel timer");
        cancelBtn.setEnabled(sleepTimer.isRunning());
        cancelBtn.addActionListener(e -> {
            sleepTimer.cancel();
            countdownLabel.setText("timer cancelled");
            cancelBtn.setEnabled(false);
            startBtn.setEnabled(true);
        });
 
        JButton closeBtn = styledButton("close");
        closeBtn.addActionListener(e -> dispose());
 
        JPanel south = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        south.setBackground(Theme.dialogBackground());
        south.add(cancelBtn);
        south.add(closeBtn);
        add(south, BorderLayout.SOUTH);
 
        // Update countdown if timer already running
        if (sleepTimer.isRunning()) {
            countdownLabel.setText("timer running...");
        }
    }
 
    private void startCountdown(int minutes) {
        startBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
        countdownLabel.setText(formatTime((long) minutes * 60));
 
        sleepTimer.startTimer(minutes);
 
        // The parent window will receive callbacks via SleepTimer.SleepCallback
        // Countdown label updates via parent passing in the timer that has callbacks wired
        dispose();
    }
 
    private String formatTime(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format("⏱ %02d:%02d remaining", m, s);
    }
 
    private JButton presetButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_BUTTON);
        btn.setBackground(Theme.panelBackground());
        btn.setForeground(Theme.textPrimary());
        btn.setBorderPainted(true);
        btn.setBorder(BorderFactory.createLineBorder(Theme.accentMuted(), 1));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }
 
    private JButton styledButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(Theme.FONT_BUTTON);
        btn.setBackground(Theme.buttonBackground());
        btn.setForeground(Theme.buttonText());
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 28));
        return btn;
    }
}
 