package com.musicplayer.ui;
 
import com.musicplayer.util.Theme;
 
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
 
/**
 * 10-band equalizer dialog with presets.
 *
 * NOTE: The sliders here are UI-only for now. Actual EQ processing requires
 * routing audio through a DSP pipeline (e.g., BasicPlayer + AudioInputStream
 * with IIR filters). This dialog is wired up and ready — just connect the
 * getBands() output to your DSP layer when you add it.
 *
 * EDITABLE:
 *   - BAND_LABELS → rename frequency bands
 *   - PRESETS     → add or change presets (values are -12 to +12 dB)
 */
public class EqualizerDialog extends JDialog {
 
    // ── Band labels (Hz) ──────────────────────────────────────────────────────
    // EDITABLE: rename bands if needed
    private static final String[] BAND_LABELS = {
        "32", "64", "125", "250", "500", "1K", "2K", "4K", "8K", "16K"
    };
 
    // ── Presets: name → 10 values in dB (-12 to +12) ─────────────────────────
    // EDITABLE: add your own presets here
    private static final Map<String, int[]> PRESETS = new LinkedHashMap<>();
    static {
        PRESETS.put("Flat",       new int[]{ 0,  0,  0,  0,  0,  0,  0,  0,  0,  0});
        PRESETS.put("Bass Boost", new int[]{10,  8,  6,  4,  2,  0,  0,  0,  0,  0});
        PRESETS.put("Treble",     new int[]{ 0,  0,  0,  0,  0,  2,  4,  6,  8, 10});
        PRESETS.put("Pop",        new int[]{-1,  3,  5,  5,  3, -1, -2, -2, -1, -1});
        PRESETS.put("Rock",       new int[]{ 5,  4,  3,  1, -1, -1,  2,  4,  5,  5});
        PRESETS.put("Jazz",       new int[]{ 4,  3,  1,  2, -1, -1,  1,  2,  3,  4});
        PRESETS.put("Classical",  new int[]{ 5,  4,  3,  2,  0,  0, -1,  0,  2,  3});
        PRESETS.put("Electronic", new int[]{ 7,  5,  0, -2,  3,  1,  5,  5,  6,  7});
        PRESETS.put("Vocal",      new int[]{-2, -2,  0,  3,  5,  5,  3,  1,  0, -1});
        PRESETS.put("Loudness",   new int[]{ 8,  5,  0, -2, -3, -3,  0,  3,  6,  8});
    }
 
    private final JSlider[] sliders = new JSlider[10];
 
    public EqualizerDialog(JFrame parent) {
        super(parent, "Equalizer", false); // non-modal so player keeps running
        setSize(600, 380);
        setResizable(false);
        setLocationRelativeTo(parent);
        getContentPane().setBackground(Theme.BACKGROUND);
        setLayout(new BorderLayout(10, 10));
 
        add(buildPresetBar(),  BorderLayout.NORTH);
        add(buildSliderPanel(), BorderLayout.CENTER);
        add(buildResetBtn(),   BorderLayout.SOUTH);
    }
 
    // ─── Preset row ───────────────────────────────────────────────────────────
 
    private JPanel buildPresetBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        panel.setBackground(Theme.SURFACE);
        panel.setBorder(new EmptyBorder(6, 10, 6, 10));
 
        JLabel lbl = new JLabel("Preset:");
        lbl.setForeground(Theme.TEXT_MUTED);
        lbl.setFont(Theme.FONT_SMALL);
        panel.add(lbl);
 
        String[] presetNames = PRESETS.keySet().toArray(new String[0]);
        JComboBox<String> combo = new JComboBox<>(presetNames);
        combo.setBackground(Theme.SURFACE2);
        combo.setForeground(Theme.TEXT_PRIMARY);
        combo.setFont(Theme.FONT_SMALL);
        combo.setFocusable(false);
        combo.addActionListener(e -> applyPreset((String) combo.getSelectedItem()));
        panel.add(combo);
 
        return panel;
    }
 
    // ─── 10 sliders ───────────────────────────────────────────────────────────
 
    private JPanel buildSliderPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 10, 8, 0));
        panel.setBackground(Theme.BACKGROUND);
        panel.setBorder(new EmptyBorder(10, 16, 10, 16));
 
        for (int i = 0; i < 10; i++) {
            JPanel col = new JPanel(new BorderLayout(0, 4));
            col.setBackground(Theme.BACKGROUND);
 
            // Value label at top
            JLabel valueLabel = new JLabel("0", SwingConstants.CENTER);
            valueLabel.setFont(Theme.FONT_SMALL);
            valueLabel.setForeground(Theme.ACCENT);
 
            // Vertical slider: -12 to +12 dB
            JSlider slider = new JSlider(JSlider.VERTICAL, -12, 12, 0);
            slider.setBackground(Theme.BACKGROUND);
            slider.setForeground(Theme.ACCENT);
            slider.setMajorTickSpacing(6);
            slider.setPaintTicks(true);
            slider.setSnapToTicks(false);
 
            final int index = i;
            slider.addChangeListener(e -> {
                int val = slider.getValue();
                valueLabel.setText((val > 0 ? "+" : "") + val);
                // EDITABLE: hook into DSP here → onBandChanged(index, val)
            });
 
            // Frequency label at bottom
            JLabel freqLabel = new JLabel(BAND_LABELS[i], SwingConstants.CENTER);
            freqLabel.setFont(Theme.FONT_SMALL);
            freqLabel.setForeground(Theme.TEXT_MUTED);
 
            col.add(valueLabel, BorderLayout.NORTH);
            col.add(slider,     BorderLayout.CENTER);
            col.add(freqLabel,  BorderLayout.SOUTH);
 
            sliders[i] = slider;
            panel.add(col);
        }
 
        return panel;
    }
 
    // ─── Reset button ─────────────────────────────────────────────────────────
 
    private JPanel buildResetBtn() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panel.setBackground(Theme.BACKGROUND);
 
        JButton reset = new JButton("Reset to Flat");
        reset.setFont(Theme.FONT_SMALL);
        reset.setBackground(Theme.SURFACE2);
        reset.setForeground(Theme.TEXT_PRIMARY);
        reset.setBorderPainted(false);
        reset.setFocusPainted(false);
        reset.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        reset.addActionListener(e -> applyPreset("Flat"));
        panel.add(reset);
 
        return panel;
    }
 
    // ─── Helpers ──────────────────────────────────────────────────────────────
 
    private void applyPreset(String name) {
        int[] values = PRESETS.get(name);
        if (values == null) return;
        for (int i = 0; i < sliders.length; i++) {
            sliders[i].setValue(values[i]);
        }
    }
 
    /** Returns current band values (dB) — use this to feed into your DSP layer. */
    public int[] getBands() {
        int[] bands = new int[10];
        for (int i = 0; i < sliders.length; i++) {
            bands[i] = sliders[i].getValue();
        }
        return bands;
    }
}
 