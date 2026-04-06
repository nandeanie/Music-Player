# 🎵 Music Player — iPod Mini-Style Java Desktop App

A feature-rich MP3 music player built in Java with a Swing GUI styled after the classic **iPod mini** aesthetic. Supports playlists, favorites, sleep timer, equalizer, song notes, play statistics, and more.

---

## ✨ Features

### Playback
- Play, pause, resume, and stop MP3 files
- Next / previous track navigation
- Seekable playback slider (frame-accurate)
- Volume control via click wheel drag
- Repeat modes: **Off → Repeat One → Repeat All** (cycles)
- **Shuffle** mode with re-randomized playlist on toggle

### Playlists
- Load `.txt` playlists (one file path per line)
- Load `.m3u` playlist files
- Drag & drop MP3 files onto the window
- Playlist management dialog with `Ctrl+Z` undo for removed songs

### UI / Visual
- iPod mini–inspired body with a rounded pink or white shell
- **LCD screen** with animated visualizer strip
- **Click wheel** — drag to control volume
- Click the LCD screen for **full-screen album art**
- Embedded album art displayed per song
- **Pink ↔ White** theme toggle

### Song Features
- ID3 tag reading (title, artist, duration, album art)
- **Favorites** — right-click a song → ♥ (persisted to `~/.musicplayer_favorites.txt`)
- **Song Notes** — right-click → 📝 (persisted to `~/.musicplayer_notes.txt`)
- **A-B Loop** — press `R` to cycle: Set A → Set B → Clear

### Extras
- **Sleep Timer** — fades volume over 10 seconds then stops playback
- **Play Stats** — tracks how many times each song has been played
- **Lyrics Fetcher** — popup lyrics window
- **Equalizer Dialog** — audio EQ settings
- **System Tray** integration
- **Memory Manager** utility

---

## 📁 Project Structure

```
musicplayer/
├── App.java                        # Entry point — launches the GUI on the EDT
├── model/
│   └── Song.java                   # MP3 model: metadata, album art, frame rate
├── player/
│   ├── MusicPlayer.java            # Core playback engine (play/pause/seek/shuffle/repeat)
│   └── PlayerListener.java         # Interface for UI callbacks
├── ui/
│   ├── MusicPlayerGUI.java         # Main Swing window — iPod body, LCD, click wheel
│   ├── PlaylistDialog.java         # Playlist management popup
│   ├── EqualizerDialog.java        # Equalizer settings dialog
│   └── SleepTimerDialog.java       # Sleep timer configuration dialog
└── util/
    ├── FavoritesManager.java       # Singleton — persist/load favorite songs
    ├── PlayStatsManager.java       # Singleton — track per-song play counts
    ├── SleepTimer.java             # Countdown timer with fade-out callback
    ├── SongNotesManager.java       # Per-song text notes, file-persisted
    ├── LyricsFetcher.java          # Lyrics retrieval utility
    ├── MemoryManager.java          # Memory usage utility
    ├── StatsDialog.java            # UI dialog showing play statistics
    └── Theme.java                  # Color palette — PINK and WHITE modes
```

---

## 🔧 Dependencies

| Library | Purpose |
|--------|---------|
| [mp3agic](https://github.com/mpatric/mp3agic) | MP3 frame counting and duration |
| [jaudiotagger](https://bitbucket.org/ijabz/jaudiotagger) | ID3 tag reading (title, artist, album art) |
| [JLayer / javazoom](http://www.javazoom.net/javalayer/javalayer.html) | MP3 audio decoding and playback (`AdvancedPlayer`) |
| Java Swing | GUI framework |
| Java Sound API (`javax.sound.sampled`) | Volume control via `FloatControl` |

---

## 🚀 Getting Started

### Prerequisites
- **Java 11+** (Java 17 recommended)
- A build tool such as Maven or Gradle, or manual classpath setup

### Running the App

1. Add the required JAR dependencies to your classpath:
   - `mp3agic-x.x.x.jar`
   - `jaudiotagger-x.x.x.jar`
   - `jl1.0.1.jar` (JLayer)

2. Compile all sources:
   ```bash
   javac -cp ".:libs/*" -d out $(find musicplayer -name "*.java")
   ```

3. Run:
   ```bash
   java -cp ".:libs/*:out" com.musicplayer.App
   ```

> On Windows, replace `:` with `;` in the classpath.

---

## 🎮 Controls & Keyboard Shortcuts

| Action | How |
|--------|-----|
| Play / Pause | Click center button on click wheel |
| Next / Previous | Click wheel left/right arrows |
| Volume | Drag the click wheel |
| A-B Loop | Press `R` key |
| Full-screen art | Click the LCD screen |
| Add to Favorites | Right-click song → ♥ |
| Add Note | Right-click song → 📝 |
| Undo playlist removal | `Ctrl+Z` in playlist window |

---

## 💾 Persisted Data

All user data is saved to the home directory (`~`):

| File | Contents |
|------|----------|
| `~/.musicplayer_favorites.txt` | Paths of favorited songs |
| `~/.musicplayer_notes.txt` | Per-song notes |
| `~/.musicplayer_stats.txt` | Play counts (`filePath|||count`) |

---

## 🎨 Theming

Two themes are available, toggled from the menu:

- **Pink** (default) — classic iPod mini rose color
- **White** — clean light mode

All colors are centralized in `Theme.java`. To customize, edit the color values in that file.

---

## ⚙️ Configuration

A few values in the source are marked `// EDITABLE`:

- `MusicPlayer.java` → `DEFAULT_VOLUME` (default: `70`, range `0–100`)
- `Song.java` → `loadMetadata()` — add more ID3 tag fields (e.g., album, year)

---

## 📝 Notes

- Volume control uses the Java Sound API's `FloatControl` on active `SourceDataLine`s. In some environments (depending on the OS audio backend), this may have limited effect when using JLayer's `AdvancedPlayer` directly.
- Lyrics fetching requires a network connection and a configured lyrics API or local source in `LyricsFetcher.java`.
- The system tray icon requires OS tray support; it degrades gracefully if unavailable.

---

## 📄 License

This project is provided as-is for personal and educational use.
