# 🎵 Music Player

A clean, portfolio-ready Java Swing MP3 player with playlist support, volume control, keyboard shortcuts, and a modern dark theme.

---

## Features

- Play, pause, resume MP3 files
- Seek with the playback slider
- Navigate between songs (Previous / Next)
- Create and load custom playlists (saved as `.txt` files)
- Volume slider (UI ready — see note below)
- Keyboard shortcuts: Space = play/pause, ← / → = prev/next
- Modern dark gray theme (easy to customize)
- Cross-platform: no hardcoded Windows paths

---

## Project Structure

```
MusicPlayer/
├── pom.xml                          # Maven build file
└── src/main/java/com/musicplayer/
    ├── App.java                     # Entry point
    ├── model/
    │   └── Song.java                # Song data + metadata loading
    ├── player/
    │   ├── MusicPlayer.java         # Core playback engine
    │   └── PlayerListener.java      # UI callback interface
    ├── ui/
    │   ├── MusicPlayerGUI.java      # Main window
    │   └── PlaylistDialog.java      # Playlist creator dialog
    └── util/
        └── Theme.java               # Colors and fonts
```

---

## Setup & Running

### Requirements
- Java 21+
- Maven 3.8+

### Build & Run

```bash
cd MusicPlayer
mvn package
java -jar target/music-player-1.0.0.jar
```

Or run directly from your IDE by launching `App.java`.

---

## Customization Guide

### Change the color theme
Edit `src/main/java/com/musicplayer/util/Theme.java`:
```java
public static final Color ACCENT = new Color(10, 132, 255); // change to any color
public static final Color BACKGROUND = new Color(28, 28, 30);
```

### Add custom button icons
Place PNG images in `src/main/resources/` and update the paths in `MusicPlayerGUI.java`:
```java
JButton prevBtn = iconButton("src/main/resources/previous.png", "⏮");
```
If the image isn't found, the fallback emoji text is used automatically.

### Change the default album art
Place your image at `src/main/resources/default_album.jpg` or update this line in `MusicPlayerGUI.java`:
```java
String imagePath = "src/main/resources/default_album.jpg";
```

### Change keyboard shortcuts
Edit `setupKeyboardShortcuts()` in `MusicPlayerGUI.java`:
```java
im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "togglePlay");
```

### Add volume control (advanced)
The volume slider UI is in place. To actually control volume, route audio through `SourceDataLine` and use `FloatControl`:
```java
FloatControl volume = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
volume.setValue(/* dB value */);
```
This requires replacing JLayer's `AdvancedPlayer` with a custom `SourceDataLine` pipeline.

---

## Dependencies

| Library      | Purpose                        |
|-------------|--------------------------------|
| `mp3agic`   | Frame count, duration metadata |
| `jlayer`    | MP3 decoding & playback        |
| `jaudiotagger` | ID3 tag reading (title, artist) |

---

## What I Built / Learned

- Multi-threaded audio playback with thread-safe pause/resume using `Object.wait/notify`
- Decoupled architecture: `MusicPlayer` ↔ `PlayerListener` ↔ `MusicPlayerGUI`
- Swing EDT safety: all UI updates routed through `SwingUtilities.invokeLater`
- Maven build with shade plugin for a single runnable JAR
