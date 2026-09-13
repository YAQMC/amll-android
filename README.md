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

The first twenty-nine AMLL-parity passes now cover the main timing, annotation, interaction, focus-motion, measured layout, background-vocal geometry, line-transform, group-opacity, mask, responsive-wrapper, playback-control and end-of-song behavior instead of relying on generic Compose defaults:

- active/inactive main-line scale follows AMLL's `1.0 / 0.97` behavior using the upstream physical scale spring
- `enableScale` mirrors upstream `setEnableScale()` and disables only the main-line 97% treatment; background-vocal 75% inactive scale remains independent
- `enableSpring` mirrors upstream `setEnableSpring()`; disabling physical springs falls back to AMLL's 500ms CSS `ease` transform transition rather than snapping transforms
- background lyrics stay grouped with their primary line rather than becoming independent scroll targets
- background-vocal size/opacity, first-word ordering and always-postposition behavior follow AMLL's grouped presentation model
- background-vocal wrapper motion is driven by the same dynamic vertical spring policy as AMLL's `bgSlideY`, including `+80 / -80 -> 0`, measured-height translation and `0.8 -> 1.0` wrapper scale
- background-first vocals unfold their occupied measured height with the slide transform; post-positioned vocals keep AMLL's in-flow/out-of-flow visibility semantics
- background lyric lines have their own independent `1.0 / 0.75` transform instead of inheriting the main line's `0.97` scale
- word highlighting uses AMLL's continuous word-level bright-to-dark gradient with the Android-like `1em` fade-width default
- gradient geometry uses measured word height for fade width and keeps AMLL's half-fade lead-in/tail around each timed word
- AMLL SOLID/GRADIENT mask endpoints use the upstream alpha targets (`0.2`, `1.0`, `0.4`) and mode-specific `450ms / 300ms ease-out` transitions
- dynamic group opacity follows upstream targets: highlighted groups use `0.85`, ordinary dynamic rows stay at `1.0`, with the DOM-style `0.4s ease` transition instead of distance fading
- `hidePassedLines` follows AMLL's playing-only passed-boundary behavior and uses the same near-zero `1e-4` target; paused lyrics are restored
- every word keeps AMLL's regular playback-time-derived upward float, including emphasized words
- long-word emphasis uses AMLL's eligibility, duration mapping, two-half easing, final-word amplification and grapheme stagger
- emphasized graphemes reproduce AMLL's horizontal push, vertical lift, scale and duration-derived glow envelope
- Android 8-30 use a native multi-sample glow approximation where API-31-only effects are unavailable
- ruby annotations and per-word romanization participate in merged word layout and ruby character count drives AMLL emphasis stagger anchors
- annotation layout reserves explicit above-baseline space so ruby/romanized text does not distort main-word timing geometry
- word/character motion is reconstructed directly from media time, so seeking is deterministic rather than free-running
- ordinary playback-clock `update(...)` samples are distinct from explicit host `seekTo(...)` calls, so explicit seeks always use seek-motion semantics
- automatic seek inference can be enabled/disabled on `AMLLPlayerState`; changing the flag resets the detector baseline while explicit seeks remain authoritative
- playback seek inference ports AMLL's monotonic wall-clock detector, including jitter/drift tolerance and repeated equal-position samples while automatic detection is enabled
- distance-based lyric blur is derived from focus distance; supported Android versions use native blur effects
- lyric gaps become focusable AMLL-style interlude dot items with timed entrance/breathing/brightening/exit motion
- touch scrolling only suspends auto-follow after AMLL's `> 10px` intent threshold; wheel input uses the upstream-style idle debounce
- native Android drag/fling physics remain owned by `LazyColumn`; the five-second auto-align delay starts only after physical scrolling is actually idle
- repeated touch input, tap interruptions and multi-touch re-anchoring restart the same suspension lifecycle without consuming native pointer events
- focus scrolling uses AMLL's interval-adaptive vertical spring policy; seek/interlude motion switches to the slower upstream spring, or the shared 500ms transform fallback when springs are disabled
- end-of-song is derived from the maximum main-line `endTime` across all lyric groups, matching upstream without requiring media duration/end callbacks from the host
- end-of-song clears active lyric highlighting and switches focus motion to AMLL's medium end spring
- an optional measured `bottomLine` Compose slot stays at the real end of the lyric list; at end-of-song it becomes the focus target, otherwise the final lyric group is focused
- the bottom line follows AMLL's `0.20 -> 0.85` focus opacity hierarchy, approximately `0.7em` typography, inherited lyric weight and distance-blur behavior
- automatic focus defaults to AMLL's viewport-relative `Center @ 0.35` alignment instead of a fixed dp offset
- Top / Center / Bottom focus anchors are supported and use the target item's measured height like upstream layout
- leading/trailing list space expands from the real composed viewport size so the first, last and optional bottom-line items can reach the configured focus anchor
- lyric-group vertical rhythm comes from AMLL's measured `0.4em` wrapper padding and `0.3em` main/background gap rather than a fixed global dp gap
- horizontal wrapper padding is responsive like upstream: `20dp` at <=500dp composed width, otherwise `1em`; hosts may explicitly override it
- secondary lyric defaults follow AMLL's `0.5em` font, `0.75em` total line-height and 0.3 opacity hierarchy
- songs containing duet lines measure each speaker at 85% content width on the correct side, so wrapping and group height follow AMLL's 15% opposite-speaker inset
- lyric typography uses the react-full default weight 600 consistently across main, secondary and background content; active-state changes no longer alter glyph metrics or wrapping
- CI builds the library/demo and runs native grouping, word-motion, annotation, interlude, interaction, spring, seek, focus-geometry, end-of-song, line-layout, background-motion, line-scale, opacity, continuous-mask, responsive-padding, behavior-flag, transform-policy and typography unit tests

The renderer is still evolving. Remaining fidelity work includes pixel-identical CSS-style text-shadow blur/glow, deeper ruby/roman annotation-mask parity, additional upstream configuration parity where useful, and platform-specific performance tuning.

## Non-goals for the first milestone

- Reimplement every parser from upstream AMLL
- Couple the library to QQ Music or YAQMC playback internals
- Use WebView, DOM, CSS or PixiJS

## Usage

完整中文接入文档见 [`docs/USAGE.zh-CN.md`](docs/USAGE.zh-CN.md)，包括 Gradle 引入、YAQMC DTO adapter、播放状态同步、背景人声/对唱、曲末底栏、样式配置和完整 Compose 示例。

```kotlin
val state = remember { AMLLPlayerState() }

LaunchedEffect(songId) {
    state.setLyricLines(lines)
}

LaunchedEffect(playbackPositionMs, isPlaying) {
    state.update(
        positionMs = playbackPositionMs,
        isPlaying = isPlaying,
    )
}

AMLLPlayer(
    state = state,
    modifier = Modifier.fillMaxSize(),
    onLineClick = { line ->
        player.seekTo(line.startTimeMs)
        state.seekTo(line.startTimeMs)
    },
    bottomLine = {
        Text("Lyrics by …")
    },
)
```

The renderer derives end-of-song from lyric-group timing, so the host does not need to provide a separate media duration/end signal for bottom-line focus.

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
