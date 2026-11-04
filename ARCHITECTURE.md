# Architecture

## Boundary

AMLL Android is a rendering library, not a music service or parser. The host owns fetching, parsing, playback and seeking.

```
YAQMC lyric DTO -> adapter -> LyricLine[]
                               |
YAQMC playback clock ----------+--> AMLLPlayerState --> Compose renderer
                                                     |-> focus/scroll animation
                                                     |-> word highlight path
                                                     `-> translation/romanization
```

## Rendering strategy

The current renderer lays out each lyric line natively with Compose. The active lyric uses a Canvas-based karaoke layer:

1. The complete line is shaped once by Compose text layout.
2. The line is painted in the inactive color.
3. A highlight path is generated from per-word timing and glyph bounding boxes.
4. The same shaped text is repainted once through that clip path in the active color.

This avoids DOM spans and avoids one composable per syllable while retaining glyph-level progressive highlighting.

## Playback clock and invalidation

Only the facts that change discretely are read during composition: the active line index, the
active interlude, end-of-song and whether the current frame is a seek frame (each through
`derivedStateOf`). The playback position itself is passed down as a `() -> Long` provider and read
in the draw/layer phase, so steady playback recomposes nothing and redraws only the lines whose
pixels depend on time:

- the active line(s), whose word mask sweeps with the clock;
- lines whose bright and dark mask colors differ (the fade toward or away from the active state).

A settled inactive line paints identical bright and dark colors, so it draws with its line start
time and is not invalidated by the clock. Interlude dots derive their visual state through
`derivedStateOf` inside `graphicsLayer` blocks.

Per-line glyph geometry (word paths, bounds, grapheme ranges, mask extents) is built once per
`(layout, render plan, balanced wrapping, word box widths, annotation reserves)` and held in the
line's `remember` slot, so it is released with the lazy item; each frame only computes the mask
for the current time. Character shadow layouts are measured only for emphasized words.

Automatic seek detection follows the upstream AMLL contract: backward media time is always a seek,
a forward jump is a seek when it outruns the elapsed wall clock beyond the jitter tolerance, and a
repeated equal sample (paused, stalled or coarse host clock) is never a seek; it only rebases the
wall-clock baseline, so a paused host may keep pushing the same position every frame.

## Measuring

- JVM render harness (Robolectric native graphics, per-frame RenderNode recording): deterministic
  fixtures in `perf-fixtures/` x {playing, paused, seek}; reports thread CPU p50/p95/p99,
  allocation and composition/draw counts, and captures screenshots for pixel comparison.

  ```bash
  ./gradlew :amll:testDebugUnitTest --tests "dev.yaqmc.amll.perf.*" -Pamll.perf=true -Pamll.perf.label=local
  ```

  Results land in `amll/build/perf/`. JVM numbers rank changes; they are not device frame times.
- On device: `./gradlew :demo:assembleBenchmark` builds a profileable, R8-optimized, debug-signed
  demo (`dev.yaqmc.amll.demo.benchmark`) that installs beside the debug demo. Start it with a
  fixture and measure with YAQMC's `scripts/android/measure-frames.mjs --package
  dev.yaqmc.amll.demo.benchmark`, or capture a Perfetto trace:

  ```bash
  adb shell am start -n dev.yaqmc.amll.demo.benchmark/dev.yaqmc.amll.demo.MainActivity \
    --es amll.fixture dense-cjk --ez amll.paused false
  ```

## Performance roadmap

- prefetch layouts for active ±3 lines
- optional blur/mesh-gradient background using Android shader APIs on supported versions
- baseline profile and Macrobenchmark module
- record device frame timelines (Perfetto FrameTimeline) for the benchmark fixtures in CI-like runs
