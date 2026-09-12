# AMLL Android

Android-native Apple Music-like lyric renderer for YAQMC.

> Early bootstrap. The goal is to provide an Android-native replacement for the web/DOM lyric rendering path used by AppleMusic-like-lyrics-style players. It does **not** embed a WebView and it has no DOM/PixiJS dependency.

## Goals

- Native Jetpack Compose rendering
- Word-level karaoke timing
- Translation and romanization
- Duet/right-aligned lines
- Background-vocal grouping and slide-in behavior
- AMLL-like spring focus, line scale and opacity transitions
- Android-like soft word-mask leading edge
- Media-time-derived word float and long-word emphasis motion
- Per-grapheme AMLL emphasis stagger, push, lift and glow
- Android 8.0+ (`minSdk 26`), matching YAQMC's Android floor
- API shaped so YAQMC can adapt its existing parsed lyric data directly

## Current replication status

The first three AMLL-parity passes mirror several upstream behaviors instead of using generic Compose defaults:

- active main line scale: `1.0`
- inactive main line scale: `0.97`
- background lyric size: approximately `0.7em`
- background lyric opacity: approximately `0.4`
- background vocals are grouped with the previous primary line rather than becoming a separate scroll target
- background vocals spring in from an approximately `-80dp` vertical offset
- focus scrolling uses a spring when the target item is already visible
- word highlighting uses a soft three-band leading edge; the default fade width is tuned for the Android-like `1em` AMLL setting
- every word keeps AMLL's regular ease-out upward drift of approximately `0.05em`; background-vocal words use twice the vertical distance
- long-word emphasis follows AMLL's duration/length eligibility rule and its two-half cubic-bezier pulse
- emphasized text is split into grapheme clusters and each cluster receives AMLL's staggered delay, horizontal push, vertical lift and scale pulse
- character glow strength/radius follows AMLL's duration-derived formula; Android 8-30 use a small native multi-sample halo instead of API-31-only `RenderEffect`
- word and character transforms are derived directly from playback time, so seeking reconstructs the same visual state deterministically
- CI builds the library/demo and runs native word-motion/grouping unit tests

This is still an early renderer. Pixel-identical text-shadow blur, interlude dots, manual-scroll auto-align suspension, ruby/per-word romanization, distance blur, and exact line-layout parity remain future work.

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
