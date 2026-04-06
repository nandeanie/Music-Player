package com.musicplayer.util;
 
import java.awt.*;
 
/**
 * MODIFIED FILE (was: basic dark Spotify theme)
 * Now supports two modes: PINK (default, iPod mini aesthetic) and WHITE/LIGHT.
 * All colors and fonts reference Theme static methods — call Theme.toggle() to switch.
 */
public class Theme {
 
    public enum Mode { PINK, WHITE }
 
    private static Mode currentMode = Mode.PINK;
 
    public static Mode getMode() { return currentMode; }
    public static void toggle() {
        currentMode = (currentMode == Mode.PINK) ? Mode.WHITE : Mode.PINK;
    }
    public static boolean isPink() { return currentMode == Mode.PINK; }
 
    // iPod Body
    public static Color iPodBody() {
        return currentMode == Mode.PINK ? new Color(220, 160, 170) : new Color(240, 240, 245);
    }
    public static Color iPodBodyHighlight() {
        return currentMode == Mode.PINK ? new Color(240, 185, 195) : new Color(255, 255, 255);
    }
    public static Color iPodBodyShadow() {
        return currentMode == Mode.PINK ? new Color(180, 110, 125) : new Color(180, 180, 190);
    }
 
    // LCD Screen - always the blue-white classic iPod LCD look
    public static Color lcdBackground()    { return new Color(200, 215, 235); }
    public static Color lcdBorder()        { return new Color(120, 135, 155); }
    public static Color lcdText()          { return new Color(20, 30, 55); }
    public static Color lcdTextMuted()     { return new Color(70, 85, 115); }
    public static Color lcdHighlight()     { return new Color(50, 90, 170); }
    public static Color lcdHighlightText() { return Color.WHITE; }
 
    // Click Wheel
    public static Color wheelOuter() {
        return currentMode == Mode.PINK ? new Color(200, 145, 155) : new Color(210, 210, 218);
    }
    public static Color wheelInner() {
        return currentMode == Mode.PINK ? new Color(230, 175, 185) : new Color(235, 235, 240);
    }
    public static Color wheelCenter() {
        return currentMode == Mode.PINK ? new Color(245, 200, 208) : new Color(248, 248, 252);
    }
    public static Color wheelText() {
        return currentMode == Mode.PINK ? new Color(120, 60, 75) : new Color(100, 100, 115);
    }
 
    // Accent
    public static Color accent()      { return new Color(255, 80, 120); }
    public static Color accentMuted() { return new Color(255, 150, 170); }
 
    // Dialogs / panels
    public static Color dialogBackground() {
        return currentMode == Mode.PINK ? new Color(245, 220, 225) : new Color(250, 250, 255);
    }
    public static Color panelBackground() {
        return currentMode == Mode.PINK ? new Color(235, 200, 208) : new Color(240, 240, 248);
    }
    public static Color inputBackground() {
        return currentMode == Mode.PINK ? new Color(255, 235, 238) : new Color(255, 255, 255);
    }
    public static Color textPrimary() {
        return currentMode == Mode.PINK ? new Color(80, 30, 45) : new Color(30, 30, 50);
    }
    public static Color textMuted() {
        return currentMode == Mode.PINK ? new Color(160, 100, 115) : new Color(120, 120, 140);
    }
    public static Color buttonBackground() {
        return currentMode == Mode.PINK ? new Color(200, 130, 145) : new Color(180, 180, 200);
    }
    public static Color buttonText() { return Color.WHITE; }
 
    // Fonts
    public static final Font LCD_FONT      = new Font("Monospaced", Font.PLAIN, 11);
    public static final Font LCD_FONT_BOLD = new Font("Monospaced", Font.BOLD,  12);
    public static final Font LCD_TITLE     = new Font("Monospaced", Font.BOLD,  13);
    public static final Font FONT_TITLE    = new Font("Dialog", Font.BOLD,  13);
    public static final Font FONT_SMALL    = new Font("Dialog", Font.PLAIN, 11);
    public static final Font FONT_TINY     = new Font("Dialog", Font.PLAIN, 10);
    public static final Font FONT_BUTTON   = new Font("Dialog", Font.BOLD,  11);
    public static final Font FONT_EMOJI    = new Font("Segoe UI Emoji", Font.PLAIN, 14);
 
    // Legacy static fields kept for EqualizerDialog backward compat
    public static final Color BACKGROUND   = new Color(200, 215, 235);
    public static final Color SURFACE      = new Color(185, 200, 220);
    public static final Color SURFACE2     = new Color(170, 185, 205);
    public static final Color ACCENT       = new Color(255, 80, 120);
    public static final Color ACCENT_HOVER = new Color(255, 110, 145);
    public static final Color TEXT_PRIMARY = new Color(20, 30, 55);
    public static final Color TEXT_MUTED   = new Color(70, 85, 115);
    public static final Color TEXT_DARK    = new Color(40, 55, 90);
    public static final Font  FONT_ARTIST  = new Font("Dialog", Font.PLAIN, 11);
    public static final Font  FONT_MENU    = new Font("Dialog", Font.BOLD,  11);
 
    private Theme() {}
}