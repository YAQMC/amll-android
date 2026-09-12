# AMLL Android

Android-native Apple Music-like lyric renderer for YAQMC.

> Early native implementation. The goal is to provide an Android-native replacement for the web/DOM lyric rendering path used by AppleMusic-like-lyrics-style players. It does **not** embed a WebView and it has no DOM/PixiJS dependency.

## Goals

- Native Jetpack Compose rendering
- Word-level karaoke timing
- Translation, per-word romanization and ruby annotations
- Duet/right-aligned lines
- Background-vocal grouping and slide-in behavior
- AMLL-like spring focus, line scale, opacity and distance blur transitions
- Android-like soft word-mask leading edge
- Media-time-derived word float and long-word emphasis motion
- Per-grapheme AMLL emphasis stagger, push, lift and glow
- Manual-scroll auto-align suspension and delayed spring return
- AMLL interlude detection and animated focusable dots
- Viewport-relative focus anchoring compatible with AMLL's layout model
- Android 8.0+ (`minSdk 26`), matching YAQMC's Android floor
- API shaped so YAQMC can adapt its existing parsed lyric data directly

## Current replication status

The first seventeen AMLL-parity passes now cover the main timing, annotation, interaction, focus-motion and measured line-layout paths instead of relying on generic Compose defaults:

- active/inactive main-line scale follows AMLL's `1.0 / 0.97` behavior
- background lyrics stay grouped with their primary line rather than becoming independent scroll targets
- background-vocal size/opacity and measured-height slide behavior follow AMLL's grouped presentation model
- main/background ordering follows the first real word timing and supports AMLL's always-postposition option
- word highlighting uses a soft leading-edge mask with the Android-like `1em` fade-width default
- every word keeps AMLL's regular playback-time-derived upward float, including emphasized words
- long-word emphasis uses AMLL's eligibility, duration mapping, two-half easing, final-word amplification and grapheme stagger
- emphasized graphemes reproduce AMLL's horizontal push, vertical lift, scale and duration-derived glow envelope
- Android 8-30 use a native multi-sample glow approximation where API-31-only effects are unavailable
- ruby annotations and per-word romanization participate in merged word layout and ruby character count drives AMLL emphasis stagger anchors
- annotation layout reserves explicit above-baseline space so ruby/romanized text does not distort main-word timing geometry
- word/character motion is reconstructed directly from media time, so seeking is deterministic rather than free-running
- distance-based lyric blur is derived from focus distance; supported Android versions use native blur effects
- lyric gaps become focusable AMLL-style interlude dot items with timed entrance/breathing/brightening/exit motion
- touch scrolling only suspends auto-follow after AMLL's `> 10px` intent threshold; wheel input uses the upstream-style idle debounce
- native Android drag/fling physics remain owned by `LazyColumn`; the five-second auto-align delay starts only after physical scrolling is actually idle
- repeated touch input, tap interruptions and multi-touch re-anchoring restart the same suspension lifecycle without consuming native pointer events
- focus scrolling uses AMLL's interval-adaptive vertical spring policy; seek/interlude motion switches to the slower upstream spring
- playback seek inference ports AMLL's monotonic wall-clock detector, including jitter/drift tolerance and repeated equal-position samples
- automatic focus defaults to AMLL's viewport-relative `Center @ 0.35` alignment instead of a fixed dp offset
- Top / Center / Bottom focus anchors are supported and use the target item's measured height like upstream layout
- leading/trailing list space expands from the real composed viewport size so the first and last lyric items can also reach the configured focus anchor
- lyric-group vertical rhythm now comes from AMLL's measured `0.4em` wrapper padding and `0.3em` main/background gap rather than a fixed global dp gap
- secondary lyric defaults follow AMLL's `0.5em` font, `0.75em` total line-height and 0.3 opacity hierarchy
- songs containing duet lines measure each speaker at 85% content width on the correct side, so wrapping and group height follow AMLL's 15% opposite-speaker inset
- CI builds the library/demo and runs the native grouping, word-motion, annotation, interlude, interaction, spring, seek, focus-geometry and line-layout unit tests

The renderer is still evolving. Remaining fidelity work includes pixel-identical CSS-style text-shadow blur/glow, exact background-vocal occupied-height expansion during its slide spring, responsive wrapper-horizontal-padding parity, end-of-song/bottom-line focus behavior once the host exposes a reliable media duration/end signal, and additional platform-specific performance tuning.

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
data class LyricRuby(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
)

data class LyricWord(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val romanText: String? = null,
    val obscene: Boolean = false,
    val ruby: List<LyricRuby> = emptyList(),
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

The project currently carries AGPL-oriented package metadata and upstream attribution notices while final repository-level licensing details are still being normalized. Keep upstream attribution when porting or adapting AppleMusic-like-lyrics implementation details.