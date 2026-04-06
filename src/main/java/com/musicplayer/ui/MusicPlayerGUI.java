package com.musicplayer.ui;
 
import com.musicplayer.model.Song;
import com.musicplayer.player.MusicPlayer;
import com.musicplayer.player.PlayerListener;
import com.musicplayer.util.*;
 
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
 
/**
 * MODIFIED FILE — complete iPod mini UI rewrite
 *
 * NEW vs ORIGINAL:
 *  - Full iPod mini visual (pink rounded body, LCD screen, click wheel)
 *  - Interactive click wheel: mouse drag = volume control
 *  - Animated visualizer strip on LCD (top of screen, like photo)
 *  - Memory photo per song (replaces album art, flashes 3s on song start)
 *  - Favorites: right-click → ♥, saved to ~/.musicplayer_favorites.txt
 *  - Song Notes: right-click → 📝, saved to ~/.musicplayer_notes.txt
 *  - A-B Loop: [R] key cycles Set-A → Set-B → Clear
 *  - Sleep Timer: menu → fades volume and stops
 *  - Drag & drop MP3s onto window
 *  - Full-screen album art: click the LCD
 *  - Play count tracking → Stats screen
 *  - Pink ↔ White theme toggle
 *  - Playlist Ctrl+Z undo removal
 *  - M3U + .txt playlist load
 */
public class MusicPlayerGUI extends JFrame implements PlayerListener {
 
    private final MusicPlayer player;
    private final SleepTimer sleepTimer;
 
    // A-B Loop
    private int abLoopState = 0; // 0=off,1=A,2=B+looping
    private int abFrameA = 0, abFrameB = 0;
 
    // UI panels
    private IpodBodyPanel bodyPanel;
    private LcdPanel lcdPanel;
    private ClickWheelPanel wheelPanel;
 
    // Playlist popup
    private JDialog playlistWindow;
    private JList<Song> playlistList;
    private DefaultListModel<Song> playlistModel;
    private Song lastRemovedSong;
    private int lastRemovedIndex = -1;
 
    // Lyrics popup
    private JDialog lyricsWindow;
    private JTextArea lyricsArea;
    private boolean lyricsVisible = false;
 
    private TrayIcon trayIcon;
 
    public MusicPlayerGUI() {
        super("Music Player");
        setSize(268, 575);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        getContentPane().setBackground(Theme.iPodBody());
        setLayout(new BorderLayout());
 
        player = new MusicPlayer(this);
        sleepTimer = new SleepTimer(buildSleepCallback());
 
        setJMenuBar(buildMenuBar());
        bodyPanel = new IpodBodyPanel();
        add(bodyPanel, BorderLayout.CENTER);
 
        setupDragAndDrop();
        setupKeyboardShortcuts();
        setupSystemTray();
        setupWindowListeners();
    }
 
    // =========================================================================
    //  iPod Body Panel
    // =========================================================================
    class IpodBodyPanel extends JPanel {
        IpodBodyPanel() {
            setLayout(null);
            setOpaque(false);
 
            lcdPanel = new LcdPanel();
            lcdPanel.setBounds(30, 28, 208, 182);
            add(lcdPanel);
 
            wheelPanel = new ClickWheelPanel();
            wheelPanel.setBounds(18, 236, 230, 230);
            add(wheelPanel);
 
            JLabel menuLbl = new JLabel("MENU", SwingConstants.CENTER);
            menuLbl.setFont(new Font("Dialog", Font.BOLD, 10));
            menuLbl.setForeground(Theme.wheelText());
            menuLbl.setBounds(102, 228, 64, 16);
            add(menuLbl);
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), arc = 40;
            // Main body gradient
            GradientPaint grad = new GradientPaint(0, 0, Theme.iPodBodyHighlight(), w, h, Theme.iPodBodyShadow());
            g2.setPaint(grad);
            g2.fill(new RoundRectangle2D.Float(3, 3, w - 6, h - 6, arc, arc));
            // Border
            g2.setColor(Theme.iPodBodyShadow());
            g2.setStroke(new BasicStroke(1.8f));
            g2.draw(new RoundRectangle2D.Float(3, 3, w - 6, h - 6, arc, arc));
            // Shine
            g2.setPaint(new GradientPaint(0, 0, new Color(255,255,255,90), w/2f, h/3f, new Color(255,255,255,0)));
            g2.fill(new RoundRectangle2D.Float(3, 3, w - 6, h / 2.5f, arc, arc));
        }
    }
 
    // =========================================================================
    //  LCD Panel
    // =========================================================================
    class LcdPanel extends JPanel {
        float[] barH = new float[20];
        float[] barT = new float[20];
        Timer vizTimer;
 
        Image currentArt = null;
        Image memFlash = null;
        long memFlashUntil = 0;
 
        String titleText = "Music Player";
        String artistText = "drop a song or load one";
        String timeText = "0:00";
        String durText = "-0:00";
        int progressVal = 0, progressMax = 100;
        int trackNum = 0, trackTotal = 0;
        String abLabel = "";
        String sleepLabel = "";
 
        LcdPanel() {
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("click for full-screen art");
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) { if (currentArt != null) showFullScreenArt(); }
            });
            startViz();
        }
 
        void startViz() {
            vizTimer = new Timer(true);
            vizTimer.scheduleAtFixedRate(new TimerTask() {
                final Random rng = new Random();
                public void run() {
                    boolean playing = player.getCurrentSong() != null;
                    for (int i = 0; i < barT.length; i++)
                        barT[i] = playing ? 2 + rng.nextFloat() * 12f : 1.5f;
                    for (int i = 0; i < barH.length; i++)
                        barH[i] += (barT[i] - barH[i]) * 0.32f;
                    SwingUtilities.invokeLater(() -> repaint());
                }
            }, 0, 75);
        }
 
        void setSong(Song song, boolean hasMemory, Image memPhoto) {
            titleText = song.getTitle();
            artistText = song.getArtist();
            timeText = "0:00"; durText = "-0:00";
            progressVal = 0;
            if (hasMemory && memPhoto != null) {
                memFlash = memPhoto;
                memFlashUntil = System.currentTimeMillis() + 3000;
                currentArt = memPhoto;
            } else {
                memFlash = null;
                currentArt = song.getAlbumArt();
            }
            repaint();
        }
 
        void updateProgress(int val, int max, String time, String dur) {
            progressVal = val; progressMax = max; timeText = time; durText = dur;
            if (memFlash != null && System.currentTimeMillis() > memFlashUntil) {
                Song cur = player.getCurrentSong();
                currentArt = (cur != null) ? cur.getAlbumArt() : null;
                memFlash = null;
            }
            repaint();
        }
 
        void setTrackInfo(int n, int t) { trackNum = n; trackTotal = t; repaint(); }
        void setAbLabel(String s)       { abLabel = s; repaint(); }
        void setSleepLabel(String s)    { sleepLabel = s; repaint(); }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
 
            // LCD bezel
            g2.setColor(new Color(75, 88, 108));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, 8, 8));
 
            // Screen background
            int bx = 3, by = 3, bw = w - 6, bh = h - 6;
            g2.setColor(Theme.lcdBackground());
            g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, 5, 5));
 
            // ── Visualizer strip (top, like the real iPod photo) ──────────────
            int vizH = 20;
            g2.setColor(new Color(175, 195, 218));
            g2.fillRect(bx, by, bw, vizH);
            int bWidth = bw / barH.length - 1;
            for (int i = 0; i < barH.length; i++) {
                int bhi = (int) Math.min(barH[i], vizH - 2);
                int bi = bx + i * (bWidth + 1);
                int byi = by + vizH - bhi - 1;
                // gradient bar color
                float ratio = (float) i / barH.length;
                Color barColor = new Color(
                        (int)(30 + ratio * 80),
                        (int)(75 + ratio * 30),
                        (int)(160 - ratio * 40));
                g2.setColor(barColor);
                g2.fillRect(bi, byi, bWidth, bhi);
            }
            g2.setColor(Theme.lcdBorder());
            g2.drawLine(bx, by + vizH, bx + bw, by + vizH);
 
            int cy = by + vizH + 2; // content start Y
 
            // ── Status row ────────────────────────────────────────────────────
            g2.setFont(Theme.LCD_FONT);
            g2.setColor(Theme.lcdTextMuted());
            if (trackTotal > 0)
                g2.drawString(trackNum + " av " + trackTotal, bx + 4, cy + 10);
            if (!abLabel.isEmpty()) {
                g2.setColor(new Color(200, 50, 80));
                g2.setFont(Theme.LCD_FONT_BOLD);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(abLabel, bx + bw / 2 - fm.stringWidth(abLabel) / 2, cy + 10);
                g2.setFont(Theme.LCD_FONT);
            }
            if (!sleepLabel.isEmpty()) {
                g2.setColor(Theme.lcdTextMuted());
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(sleepLabel, bx + bw - fm.stringWidth(sleepLabel) - 4, cy + 10);
            }
 
            // ── Album art (or memory photo) ───────────────────────────────────
            int artY = cy + 14;
            int artSz = 64;
            int artX = bx + (bw - artSz) / 2;
            if (currentArt != null) {
                Shape clip = new RoundRectangle2D.Float(artX, artY, artSz, artSz, 6, 6);
                g2.setClip(clip);
                g2.drawImage(currentArt, artX, artY, artSz, artSz, null);
                g2.setClip(null);
                g2.setColor(Theme.lcdBorder());
                g2.setStroke(new BasicStroke(1f));
                g2.draw(clip);
            } else {
                g2.setColor(Theme.panelBackground());
                g2.fillRoundRect(artX, artY, artSz, artSz, 6, 6);
                g2.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 26));
                g2.setColor(Theme.lcdTextMuted());
                g2.drawString("♫", artX + 20, artY + 42);
            }
 
            // ── Title & artist ────────────────────────────────────────────────
            int titleY = artY + artSz + 11;
            g2.setFont(Theme.LCD_FONT_BOLD);
            g2.setColor(Theme.lcdText());
            g2.drawString(trunc(titleText, 24), bx + 4, titleY);
 
            g2.setFont(Theme.LCD_FONT);
            g2.setColor(Theme.lcdTextMuted());
            g2.drawString(trunc(artistText, 28), bx + 4, titleY + 12);
 
            // ── Progress bar ──────────────────────────────────────────────────
            int pbY = titleY + 20;
            int pbW = bw - 8;
            g2.setColor(new Color(148, 165, 192));
            g2.fillRoundRect(bx + 4, pbY, pbW, 4, 4, 4);
            if (progressMax > 0) {
                int filled = Math.max(4, Math.min((int)((float)progressVal/progressMax * pbW), pbW));
                g2.setColor(new Color(50, 90, 170));
                g2.fillRoundRect(bx + 4, pbY, filled, 4, 4, 4);
                g2.setColor(Theme.lcdText());
                g2.fillOval(bx + 4 + filled - 3, pbY - 2, 7, 7);
            }
 
            // ── Time display ──────────────────────────────────────────────────
            g2.setFont(Theme.LCD_FONT);
            g2.setColor(Theme.lcdTextMuted());
            g2.drawString(timeText, bx + 4, pbY + 14);
            FontMetrics fm2 = g2.getFontMetrics();
            g2.drawString(durText, bx + bw - fm2.stringWidth(durText) - 4, pbY + 14);
        }
 
        private String trunc(String s, int max) {
            if (s == null) return "";
            return s.length() > max ? s.substring(0, max - 1) + "…" : s;
        }
    }
 
    // =========================================================================
    //  Click Wheel Panel
    // =========================================================================
    class ClickWheelPanel extends JPanel {
        double lastAngle = 0;
        boolean dragging = false;
 
        ClickWheelPanel() {
            setOpaque(false);
            MouseAdapter ma = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    dragging = true;
                    lastAngle = angle(e);
                }
                public void mouseReleased(MouseEvent e) { dragging = false; }
                public void mouseDragged(MouseEvent e) {
                    if (!dragging) return;
                    double a = angle(e);
                    double d = a - lastAngle;
                    if (d > Math.PI) d -= 2 * Math.PI;
                    if (d < -Math.PI) d += 2 * Math.PI;
                    lastAngle = a;
                    int dv = (int)(d * 28);
                    if (dv != 0) {
                        int nv = Math.max(0, Math.min(100, player.getVolume() + dv));
                        player.setVolume(nv);
                        lcdPanel.setSleepLabel("vol:" + nv);
                        lcdPanel.repaint();
                    }
                }
                public void mouseClicked(MouseEvent e) {
                    int cx = getWidth()/2, cy = getHeight()/2;
                    int dx = e.getX()-cx, dy = e.getY()-cy;
                    double dist = Math.sqrt(dx*dx + dy*dy);
                    int outerR = Math.min(getWidth(),getHeight())/2;
                    int innerR = outerR/3;
                    if (dist < innerR) { togglePlayPause(); return; }
                    if (dist < outerR - 8) {
                        double a = Math.toDegrees(Math.atan2(dy, dx));
                        if (a >= -45 && a < 45) player.nextSong();
                        else if (a < -135 || a >= 135) player.prevSong();
                        else if (a >= 45 && a < 135) togglePlayPause();
                    }
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
        }
 
        double angle(MouseEvent e) {
            return Math.atan2(e.getY() - getHeight()/2, e.getX() - getWidth()/2);
        }
 
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth()/2, cy = getHeight()/2;
            int outerR = Math.min(getWidth(),getHeight())/2 - 4;
            int innerR = outerR / 3;
 
            // Outer ring
            RadialGradientPaint rg = new RadialGradientPaint(
                    new Point(cx - outerR/4, cy - outerR/4), outerR,
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{Theme.wheelInner(), Theme.wheelOuter(), Theme.wheelInner()});
            g2.setPaint(rg);
            g2.fillOval(cx-outerR, cy-outerR, outerR*2, outerR*2);
            g2.setColor(Theme.iPodBodyShadow());
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(cx-outerR, cy-outerR, outerR*2, outerR*2);
 
            // Center button
            g2.setColor(Theme.wheelCenter());
            g2.fillOval(cx-innerR, cy-innerR, innerR*2, innerR*2);
            g2.setColor(Theme.iPodBodyShadow());
            g2.setStroke(new BasicStroke(1f));
            g2.drawOval(cx-innerR, cy-innerR, innerR*2, innerR*2);
 
            // Button labels
            g2.setFont(new Font("Dialog", Font.PLAIN, 11));
            g2.setColor(Theme.wheelText());
            FontMetrics fm = g2.getFontMetrics();
            String sTxt = "▶ ⏸";
            g2.drawString(sTxt, cx - fm.stringWidth(sTxt)/2, cy + outerR - 13);
            g2.drawString("⏮", cx - outerR + 8, cy + fm.getAscent()/2);
            String nxt = "⏭";
            g2.drawString(nxt, cx + outerR - fm.stringWidth(nxt) - 8, cy + fm.getAscent()/2);
        }
    }
 
    // =========================================================================
    //  Menu bar
    // =========================================================================
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        bar.setBackground(Theme.panelBackground());
 
        JMenu songs = mkMenu("Songs");
        songs.add(mkItem("Load Song", e -> openSong()));
        bar.add(songs);
 
        JMenu pl = mkMenu("Playlist");
        pl.add(mkItem("Create Playlist", e -> new PlaylistDialog(this).setVisible(true)));
        pl.add(mkItem("Load Playlist (.txt / .m3u)", e -> openPlaylist()));
        pl.add(mkItem("Show Playlist", e -> showPlaylistWindow()));
        bar.add(pl);
 
        JMenu tools = mkMenu("Tools");
        tools.add(mkItem("Equalizer", e -> new EqualizerDialog(this).setVisible(true)));
        tools.add(mkItem("Toggle Lyrics  [L]", e -> toggleLyrics()));
        tools.add(mkItem("Sleep Timer ⏰", e -> new SleepTimerDialog(this, sleepTimer).setVisible(true)));
        tools.add(mkItem("Stats ✨", e -> new StatsDialog(this).setVisible(true)));
        tools.add(mkItem("Toggle Theme 🌓", e -> {
            Theme.toggle();
            SwingUtilities.invokeLater(() -> {
                getContentPane().setBackground(Theme.iPodBody());
                repaint();
                if (playlistWindow != null) SwingUtilities.updateComponentTreeUI(playlistWindow);
            });
        }));
        bar.add(tools);
 
        return bar;
    }
 
    // =========================================================================
    //  Playlist window
    // =========================================================================
    private void showPlaylistWindow() {
        if (playlistWindow == null) buildPlaylistWindow();
        refreshPlaylistModel();
        playlistWindow.setVisible(true);
    }
 
    private void buildPlaylistWindow() {
        playlistWindow = new JDialog(this, "Playlist", false);
        playlistWindow.setSize(390, 480);
        playlistWindow.setLocationRelativeTo(this);
        playlistWindow.getContentPane().setBackground(Theme.dialogBackground());
        playlistWindow.setLayout(new BorderLayout());
 
        playlistModel = new DefaultListModel<>();
        playlistList  = new JList<>(playlistModel);
        playlistList.setBackground(Theme.lcdBackground());
        playlistList.setForeground(Theme.lcdText());
        playlistList.setFont(Theme.LCD_FONT);
        playlistList.setSelectionBackground(Theme.lcdHighlight());
        playlistList.setSelectionForeground(Theme.lcdHighlightText());
        playlistList.setCellRenderer(new PlaylistCellRenderer());
 
        playlistList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Song s = playlistList.getSelectedValue();
                    if (s != null) { player.loadSong(s); onSongChanged(s); }
                }
            }
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int i = playlistList.locationToIndex(e.getPoint());
                    if (i >= 0) {
                        playlistList.setSelectedIndex(i);
                        showSongContextMenu(playlistList.getSelectedValue(), e);
                    }
                }
            }
        });
 
        playlistList.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        playlistList.getActionMap().put("undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { undoLastRemoval(); }
        });
 
        playlistWindow.add(new JScrollPane(playlistList), BorderLayout.CENTER);
 
        JLabel hint = new JLabel("  right-click for options  •  double-click to play  •  Ctrl+Z to undo");
        hint.setFont(Theme.FONT_TINY);
        hint.setForeground(Theme.textMuted());
        hint.setBorder(new EmptyBorder(4, 4, 4, 4));
        playlistWindow.add(hint, BorderLayout.SOUTH);
    }
 
    private void refreshPlaylistModel() {
        if (playlistModel == null) return;
        playlistModel.clear();
        List<Song> pl = player.getPlaylist();
        if (pl != null) pl.forEach(playlistModel::addElement);
    }
 
    private void showSongContextMenu(Song song, MouseEvent e) {
        if (song == null) return;
        JPopupMenu menu = new JPopupMenu();
 
        boolean isFav = FavoritesManager.getInstance().isFavorite(song.getFilePath());
        JMenuItem favItem = new JMenuItem(isFav ? "♥ Remove from Favorites" : "♡ Add to Favorites");
        favItem.setFont(Theme.FONT_SMALL);
        favItem.addActionListener(ev -> { FavoritesManager.getInstance().toggle(song.getFilePath()); playlistList.repaint(); });
        menu.add(favItem);
 
        boolean hasNote = SongNotesManager.getInstance().hasNote(song.getFilePath());
        JMenuItem noteItem = new JMenuItem(hasNote ? "📝 Edit Note" : "📝 Add Note");
        noteItem.setFont(Theme.FONT_SMALL);
        noteItem.addActionListener(ev -> showNoteDialog(song));
        menu.add(noteItem);
 
        JMenuItem memItem = new JMenuItem("📷 Set Memory Photo");
        memItem.setFont(Theme.FONT_SMALL);
        memItem.addActionListener(ev -> pickMemoryPhoto(song));
        menu.add(memItem);
 
        if (MemoryManager.getInstance().hasMemory(song.getFilePath())) {
            JMenuItem clrMem = new JMenuItem("🗑 Clear Memory Photo");
            clrMem.setFont(Theme.FONT_SMALL);
            clrMem.addActionListener(ev -> {
                MemoryManager.getInstance().removeMemory(song.getFilePath());
                if (player.getCurrentSong() != null &&
                        player.getCurrentSong().getFilePath().equals(song.getFilePath()))
                    lcdPanel.currentArt = player.getCurrentSong().getAlbumArt();
            });
            menu.add(clrMem);
        }
 
        menu.addSeparator();
        JMenuItem rm = new JMenuItem("Remove from Playlist");
        rm.setFont(Theme.FONT_SMALL);
        rm.addActionListener(ev -> {
            int idx = playlistModel.indexOf(song);
            if (idx >= 0) {
                lastRemovedSong = song; lastRemovedIndex = idx;
                playlistModel.remove(idx);
                List<Song> pl = player.getPlaylist();
                if (pl != null) pl.remove(song);
            }
        });
        menu.add(rm);
 
        menu.show(e.getComponent(), e.getX(), e.getY());
    }
 
    private void undoLastRemoval() {
        if (lastRemovedSong == null) return;
        List<Song> pl = player.getPlaylist();
        if (pl != null) {
            int at = Math.min(lastRemovedIndex, pl.size());
            pl.add(at, lastRemovedSong);
            playlistModel.add(at, lastRemovedSong);
            lastRemovedSong = null; lastRemovedIndex = -1;
        }
    }
 
    private void showNoteDialog(Song song) {
        String existing = SongNotesManager.getInstance().getNote(song.getFilePath());
        JTextArea area = new JTextArea(existing, 5, 28);
        area.setFont(Theme.LCD_FONT); area.setLineWrap(true); area.setWrapStyleWord(true);
        int res = JOptionPane.showConfirmDialog(this, new JScrollPane(area),
                "📝 Note for: " + song.getTitle(), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            SongNotesManager.getInstance().setNote(song.getFilePath(), area.getText());
            playlistList.repaint();
        }
    }
 
    private void pickMemoryPhoto(Song song) {
        JFileChooser fc = new JFileChooser(System.getProperty("user.home"));
        fc.setFileFilter(new FileNameExtensionFilter("Images", "jpg","jpeg","png","gif","bmp"));
        fc.setDialogTitle("Memory photo for: " + song.getTitle());
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
            MemoryManager.getInstance().setPhoto(song.getFilePath(), fc.getSelectedFile().getAbsolutePath());
            Song cur = player.getCurrentSong();
            if (cur != null && cur.getFilePath().equals(song.getFilePath())) {
                try {
                    Image img = ImageIO.read(fc.getSelectedFile());
                    if (img != null) lcdPanel.currentArt = img;
                    lcdPanel.repaint();
                } catch (Exception ignored) {}
            }
        }
    }
 
    // =========================================================================
    //  Playlist cell renderer
    // =========================================================================
    class PlaylistCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int idx, boolean sel, boolean focus) {
            JLabel l = (JLabel) super.getListCellRendererComponent(list, value, idx, sel, focus);
            if (value instanceof Song s) {
                boolean fav  = FavoritesManager.getInstance().isFavorite(s.getFilePath());
                boolean note = SongNotesManager.getInstance().hasNote(s.getFilePath());
                boolean mem  = MemoryManager.getInstance().hasMemory(s.getFilePath());
                StringBuilder sb = new StringBuilder();
                if (fav)  sb.append("♥ ");
                if (note) sb.append("📝 ");
                if (mem)  sb.append("📷 ");
                sb.append(s.getTitle()).append("  ").append(s.getDuration());
                l.setText(sb.toString());
                l.setFont(Theme.LCD_FONT);
                l.setBorder(new EmptyBorder(3, 6, 3, 6));
                if (!sel) {
                    l.setBackground(Theme.lcdBackground());
                    l.setForeground(fav ? Theme.accent() : Theme.lcdText());
                }
            }
            return l;
        }
    }
 
    // =========================================================================
    //  Lyrics popup
    // =========================================================================
    private void toggleLyrics() {
        if (lyricsWindow == null) {
            lyricsWindow = new JDialog(this, "Lyrics", false);
            lyricsWindow.setSize(340, 420);
            lyricsWindow.setLocationRelativeTo(this);
            lyricsWindow.getContentPane().setBackground(Theme.lcdBackground());
            lyricsWindow.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) { lyricsVisible = false; }
            });
            lyricsArea = new JTextArea("load a song and press L");
            lyricsArea.setFont(Theme.LCD_FONT); lyricsArea.setForeground(Theme.lcdText());
            lyricsArea.setBackground(Theme.lcdBackground()); lyricsArea.setEditable(false);
            lyricsArea.setLineWrap(true); lyricsArea.setWrapStyleWord(true);
            lyricsArea.setBorder(new EmptyBorder(10,12,10,12));
            lyricsWindow.add(new JScrollPane(lyricsArea));
        }
        lyricsVisible = !lyricsVisible;
        lyricsWindow.setVisible(lyricsVisible);
        if (lyricsVisible) fetchAndShowLyrics();
    }
 
    private void fetchAndShowLyrics() {
        Song song = player.getCurrentSong();
        if (song == null) { if (lyricsArea != null) lyricsArea.setText("no song playing"); return; }
        lyricsArea.setText("fetching lyrics for " + song.getTitle() + "...");
        new Thread(() -> {
            String lyrics = LyricsFetcher.fetchLyrics(song.getArtist(), song.getTitle());
            SwingUtilities.invokeLater(() -> { if (lyricsArea != null) lyricsArea.setText(lyrics); });
        }).start();
    }
 
    // =========================================================================
    //  Full-screen art
    // =========================================================================
    private void showFullScreenArt() {
        Image art = lcdPanel.currentArt;
        if (art == null) return;
        JDialog d = new JDialog(this, "art", true);
        d.setUndecorated(true); d.setSize(420,420); d.setLocationRelativeTo(null);
        JLabel lbl = new JLabel() {
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.drawImage(art, 0, 0, getWidth(), getHeight(), null);
            }
        };
        lbl.setToolTipText("click to close");
        lbl.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { d.dispose(); } });
        d.add(lbl); d.setVisible(true);
    }
 
    // =========================================================================
    //  File openers
    // =========================================================================
    private void openSong() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.home"));
        fc.setFileFilter(new FileNameExtensionFilter("MP3 Files", "mp3"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
            Song song = new Song(fc.getSelectedFile().getPath());
            player.loadSong(song); onSongChanged(song);
        }
    }
 
    private void openPlaylist() {
        JFileChooser fc = new JFileChooser(System.getProperty("user.home"));
        fc.setFileFilter(new FileNameExtensionFilter("Playlist Files (txt, m3u)", "txt","m3u","m3u8"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION && fc.getSelectedFile() != null) {
            File f = fc.getSelectedFile();
            player.stopSong();
            String name = f.getName().toLowerCase();
            if (name.endsWith(".m3u") || name.endsWith(".m3u8")) player.loadM3uPlaylist(f);
            else player.loadPlaylist(f);
            refreshPlaylistModel();
        }
    }
 
    // =========================================================================
    //  Sleep timer callback
    // =========================================================================
    private SleepTimer.SleepCallback buildSleepCallback() {
        return new SleepTimer.SleepCallback() {
            public void onTick(long s) {
                long m = s/60, sec = s%60;
                lcdPanel.setSleepLabel(String.format("💤%d:%02d", m, sec));
            }
            public void onFadeStart() {
                int startVol = player.getVolume();
                new Thread(() -> {
                    for (int i = 10; i >= 0; i--) {
                        final int v = (int)(startVol * (i/10.0));
                        SwingUtilities.invokeLater(() -> player.setVolume(v));
                        try { Thread.sleep(1000); } catch (Exception ignored) {}
                    }
                }).start();
            }
            public void onFinish() {
                player.stopSong(); lcdPanel.setSleepLabel(""); player.setVolume(70);
            }
        };
    }
 
    // =========================================================================
    //  Drag & Drop
    // =========================================================================
    private void setupDragAndDrop() {
        new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File f = files.get(0);
                        if (f.getName().toLowerCase().endsWith(".mp3")) {
                            Song s = new Song(f.getPath());
                            player.loadSong(s); onSongChanged(s);
                        }
                    }
                } catch (Exception ignored) {}
            }
        });
    }
 
    // =========================================================================
    //  Keyboard shortcuts
    // =========================================================================
    private void setupKeyboardShortcuts() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();
 
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "play");
        am.put("play", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { togglePlayPause(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "prev");
        am.put("prev", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { player.prevSong(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "next");
        am.put("next", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { player.nextSong(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_L, 0), "lyrics");
        am.put("lyrics", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { toggleLyrics(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0), "abloop");
        am.put("abloop", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { cycleAbLoop(); }
        });
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "undo");
        am.put("undo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { undoLastRemoval(); }
        });
    }
 
    private void togglePlayPause() {
        if (player.getCurrentSong() == null) return;
        player.playCurrent();
    }
 
    private void cycleAbLoop() {
        if (player.getCurrentSong() == null) return;
        abLoopState = (abLoopState + 1) % 3;
        switch (abLoopState) {
            case 1 -> { abFrameA = lcdPanel.progressVal; lcdPanel.setAbLabel("A-"); }
            case 2 -> {
                abFrameB = lcdPanel.progressVal;
                if (abFrameB <= abFrameA) { abLoopState = 0; lcdPanel.setAbLabel(""); break; }
                lcdPanel.setAbLabel("A-B");
                new Thread(() -> {
                    while (abLoopState == 2) {
                        try { Thread.sleep(200); } catch (Exception ignored) {}
                        if (lcdPanel.progressVal >= abFrameB) {
                            Song s = player.getCurrentSong();
                            if (s != null) {
                                player.setCurrentFrame(abFrameA);
                                player.setCurrentTimeMs((int)(abFrameA /
                                        (2.08 * s.getFrameRatePerMillisecond())));
                                player.stopSong();
                                player.playCurrent();
                            }
                        }
                    }
                }, "ABLoop").start();
            }
            case 0 -> lcdPanel.setAbLabel("");
        }
    }
 
    // =========================================================================
    //  System tray + window listeners
    // =========================================================================
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) return;
        Image img = Toolkit.getDefaultToolkit().createImage(new byte[0]);
        PopupMenu popup = new PopupMenu();
        MenuItem pp = new MenuItem("Play / Pause"); pp.addActionListener(e -> togglePlayPause());
        MenuItem nxt = new MenuItem("Next"); nxt.addActionListener(e -> player.nextSong());
        MenuItem show = new MenuItem("Show"); show.addActionListener(e -> { setVisible(true); setState(Frame.NORMAL); });
        MenuItem exit = new MenuItem("Exit"); exit.addActionListener(e -> System.exit(0));
        popup.add(pp); popup.add(nxt); popup.addSeparator(); popup.add(show); popup.add(exit);
        trayIcon = new TrayIcon(img, "Music Player", popup);
        trayIcon.setImageAutoSize(true);
        trayIcon.addActionListener(e -> { setVisible(true); setState(Frame.NORMAL); });
        try { SystemTray.getSystemTray().add(trayIcon); } catch (Exception ignored) {}
    }
 
    private void setupWindowListeners() {
        addWindowListener(new WindowAdapter() {
            public void windowIconified(WindowEvent e) { setVisible(false); }
            public void windowClosing(WindowEvent e) {
                setVisible(false);
                if (trayIcon != null)
                    trayIcon.displayMessage("Music Player", "Still running in tray.", TrayIcon.MessageType.INFO);
            }
        });
    }
 
    // =========================================================================
    //  Helpers
    // =========================================================================
    private JMenu mkMenu(String name) {
        JMenu m = new JMenu(name);
        m.setFont(Theme.FONT_BUTTON);
        m.setForeground(Theme.textPrimary());
        return m;
    }
    private JMenuItem mkItem(String name, ActionListener a) {
        JMenuItem i = new JMenuItem(name);
        i.setFont(Theme.FONT_SMALL);
        i.addActionListener(a);
        return i;
    }
 
    // =========================================================================
    //  PlayerListener callbacks
    // =========================================================================
    @Override
    public void onSongChanged(Song song) {
        SwingUtilities.invokeLater(() -> {
            PlayStatsManager.getInstance().increment(song.getFilePath());
            String photoPath = MemoryManager.getInstance().getPhotoPath(song.getFilePath());
            Image memPhoto = null;
            if (photoPath != null) {
                try { memPhoto = ImageIO.read(new File(photoPath)); } catch (Exception ignored) {}
            }
            lcdPanel.setSong(song, memPhoto != null, memPhoto);
            List<Song> pl = player.getPlaylist();
            if (pl != null) lcdPanel.setTrackInfo(pl.indexOf(song) + 1, pl.size());
            if (lyricsVisible) fetchAndShowLyrics();
            if (trayIcon != null)
                trayIcon.displayMessage("Now Playing", song.getTitle() + " — " + song.getArtist(),
                        TrayIcon.MessageType.INFO);
        });
    }
 
    @Override
    public void onSliderUpdate(int frame) {
        SwingUtilities.invokeLater(() -> {
            Song song = player.getCurrentSong();
            if (song == null || song.getMp3File() == null) return;
            int maxF = song.getMp3File().getFrameCount();
            long totalMs = song.getMp3File().getLengthInMilliseconds();
            long elSec = (maxF > 0) ? (long)((double)frame/maxF * totalMs/1000) : 0;
            long remSec = Math.max(0, totalMs/1000 - elSec);
            lcdPanel.updateProgress(frame, maxF,
                    String.format("%d:%02d", elSec/60, elSec%60),
                    String.format("-%d:%02d", remSec/60, remSec%60));
        });
    }
 
    @Override public void onPlaybackStarted() { SwingUtilities.invokeLater(() -> lcdPanel.repaint()); }
    @Override public void onPlaybackStopped() { SwingUtilities.invokeLater(() -> lcdPanel.repaint()); }
}
 