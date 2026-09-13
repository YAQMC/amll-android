# AMLL Android

Android-native Apple Music-like lyric renderer for YAQMC.

> Native Android implementation of the AppleMusic-like-lyrics rendering model. It does **not** embed a WebView and has no DOM/PixiJS dependency. The renderer itself is Jetpack Compose, while `AMLLPlayerView` lets an ordinary Android View/Capacitor host mount it without becoming a Compose-first application.

## Goals

- Android 8.0+ (`minSdk 26`)
- word-level karaoke timing and continuous AMLL-style masks
- translation, per-word romanization and timed ruby annotations
- duet/right-aligned lyrics and grouped background vocals
- AMLL-style focus springs, scale, opacity, blur, interludes and manual-scroll behavior
- deterministic media-time-derived animation across pause/seek/resume
- stable host APIs for both Compose and traditional Android View applications
- packaging suitable for direct YAQMC consumption

## Current status

The first thirty-six implementation/integration passes now cover the major AMLL rendering and Android-host boundaries:

- main-line `1.0 / 0.97` scale, independent background `1.0 / 0.75` scale, `enableScale`, `enableSpring` and the 500ms non-spring fallback
- grouped background vocals, first-word ordering, forced postposition, measured expansion and duet-side geometry
- balanced lyric wrapping, AMLL word chunking, measured wrapper rhythm, responsive `20dp / 1em` horizontal padding and stable weight-600 typography
- continuous bright-to-dark word masks with upstream SOLID/GRADIENT alpha targets and transition timings
- per-word float, long-word emphasis, per-grapheme push/lift/scale and native zero-offset text shadow on Android 8+
- ruby and per-word romanization participating in measured word boxes and sharing one parent word-level mask with the base glyphs
- ruby-segment flex geometry, UTF-16 timing sweep and annotation-aware mask height/width
- emphasized descendants moving underneath a mask that remains in parent-word space, matching upstream DOM mask ownership
- touch/wheel manual-scroll suspension, delayed auto-align, viewport-relative focus anchors, adaptive focus springs and explicit/automatic seek semantics
- interlude dots, end-of-song focus and optional measured `bottomLine`
- `enableBlur`, `hidePassedLines`, `alwaysPostpositionBackground` and other commonly used upstream behavior flags
- upstream-style obscene-word preprocessing through `Disabled`, `FullMask` and `PartialMask` modes while preserving timing/ruby/roman metadata
- `AMLLPlayerView` for BridgeActivity/Fragment/Capacitor/legacy View hosts; the published AAR is smoke-tested from a consumer that does not apply the Compose compiler plugin
- Maven publication tested through isolated consumers, plus credential-gated GitHub Packages publication for cross-repository delivery

Remaining fidelity work is mostly narrower platform differences: browser CSS versus Android/Skia rasterization, residual DOM-versus-Skia font/shaping/subpixel differences, less frequently used upstream configuration, and real-device performance tuning with large YAQMC lyric datasets.

## Compose usage

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

## Traditional Android View / Capacitor usage

A host does not need to apply the Compose compiler plugin just to mount the native renderer:

```kotlin
val lyricsView = AMLLPlayerView(context).apply {
    setLyricLines(lines)
    update(positionMs = currentPositionMs, isPlaying = isPlaying)
    onLineClick = { line -> player.seekTo(line.startTimeMs) }
}

container.addView(
    lyricsView,
    ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    ),
)
```

`AMLLPlayerView` uses `DisposeOnViewTreeLifecycleDestroyed`; mount it under an Activity/Fragment View tree with a normal lifecycle owner. Mutating calls follow ordinary Android View semantics and should be made on the main thread.

## Obscene-word masking

```kotlin
state.updateMaskObsceneWordsMode(MaskObsceneWordsMode.PartialMask)
state.updateMaskObsceneWordChar('*')
```

`LyricWord.obscene` is processed from the original unmasked lyric data, so changing mode or mask character is reversible without re-fetching lyrics. Ruby/roman annotations and timing metadata are preserved.

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

## Dependency and publication

Local development:

```bash
./gradlew :amll:publishReleasePublicationToMavenLocal
```

```kotlin
repositories {
    mavenLocal()
    google()
    mavenCentral()
}

dependencies {
    implementation("dev.yaqmc:amll-android:0.1.0-SNAPSHOT")
}
```

The repository also contains a GitHub Packages publish workflow. A release tag such as `v0.1.0-alpha.1` is published as Maven version `0.1.0-alpha.1`; manual workflow dispatch can provide a version directly. Publishing uses the repository `GITHUB_TOKEN` with `packages: write`.

For a different private repository such as `YAQMC/YAQMC`, package download credentials are still required. CI may use its `GITHUB_TOKEN` only when it has read access to the package; otherwise use a PAT classic with `read:packages`. Do not commit package credentials to the repository.

## YAQMC integration direction

1. Keep YAQMC's existing lyric fetch/parse/provider pipeline.
2. Convert the existing lyric document into `LyricLine` only when lyrics change.
3. Mount `AMLLPlayerView` as a native sibling/overlay of the existing Capacitor WebView.
4. Drive playback position/play state from YAQMC's Android/Rust Core clock instead of pushing per-frame updates through JavaScript.
5. Route lyric-line clicks back to the native seek command.
6. Once the overlay path is stable, decide whether the whole Android full-screen lyric page should move native.

## Build baseline

- compile SDK 36
- demo target SDK 36
- minSdk 26
- AGP 8.13.0
- Kotlin / Compose compiler plugin 2.2.20
- Compose BOM 2026.06.00 (Compose 1.11 generation)
- Gradle 8.14.3
- Java / Kotlin JVM target 21

CI builds/tests the library and demo, publishes the release artifact to Maven Local, then compiles both a Compose consumer and a plain Android View consumer against the published coordinate.

## Documentation

完整中文接入说明见 [`docs/USAGE.zh-CN.md`](docs/USAGE.zh-CN.md)。

## Non-goals for the first milestone

- reimplement every upstream parser
- couple `amll-android` to QQ Music or YAQMC network/provider internals
- introduce a WebView/DOM/PixiJS rendering dependency

## License

The project currently carries AGPL-oriented package metadata and upstream attribution notices while final repository-level licensing details are still being normalized. Keep upstream attribution when porting or adapting AppleMusic-like-lyrics implementation details.
