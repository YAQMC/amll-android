# AMLL Android

Android-native Apple Music-like lyric renderer for YAQMC.

> Early bootstrap. The goal is to provide an Android-native replacement for the web/DOM lyric rendering path used by AppleMusic-like-lyrics-style players. It does **not** embed a WebView and it has no DOM/PixiJS dependency.

## Goals

- Native Jetpack Compose rendering
- Word-level karaoke timing
- Translation and romanization
- Duet/right-aligned lines
- Background-vocal line model
- Animated focus, scale, alpha and automatic scrolling
- Android 8.0+ (`minSdk 26`), matching YAQMC's Android floor
- API shaped so YAQMC can adapt its existing parsed lyric data directly

## Non-goals for the first milestone

- Reimplement every parser from upstream AMLL
- Couple the library to QQ Music or YAQMC playback internals
- Use WebView, DOM, CSS or PixiJS

## Usage

```kotlin
val state = remember { AMLLPlayerState() }

LaunchedEffect(songId) {
    state.setLyricLines(lines)
}

LaunchedEffect(playbackPositionMs) {
    state.update(playbackPositionMs)
}

AMLLPlayer(
    state = state,
    modifier = Modifier.fillMaxSize(),
    onLineClick = { line -> player.seekTo(line.startTimeMs) },
)
```

## Model

```kotlin
data class LyricWord(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val romanText: String? = null,
)

data class LyricLine(
    val words: List<LyricWord>,
    val translatedLyric: String = "",
    val romanLyric: String = "",
    val startTimeMs: Long,
    val endTimeMs: Long,
    val isBackground: Boolean = false,
    val isDuet: Boolean = false,
)
```

## YAQMC integration plan

1. Keep YAQMC's existing lyric fetch/parse pipeline.
2. Add a tiny adapter from YAQMC's lyric DTO to `LyricLine`.
3. Mount this Compose UI in the Android host rather than rendering lyrics in Capacitor/WebView.
4. Feed the native renderer from the same playback position source used by Android MediaSession/native audio.
5. Once stable, optionally move the whole Android full-screen lyric page to native Compose.

## Build baseline

- compile/target SDK 37
- AGP 9.4.0
- Kotlin/Compose compiler plugin 2.3.21
- Compose BOM 2026.08.00
- minSdk 26

## License

The bootstrap is intended to be released under GNU AGPL-3.0-only to stay compatible with the upstream AppleMusic-like-lyrics project's license while the implementation is intentionally AMLL-compatible. Keep upstream attribution when porting or adapting upstream implementation details.
