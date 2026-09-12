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

## Performance roadmap

- cache `TextLayoutResult` by width/style/line identity
- replace coarse 16 ms host updates with frame-clock interpolation from a playback anchor
- prefetch layouts for active ±3 lines
- optional blur/mesh-gradient background using Android shader APIs on supported versions
- baseline profile and Macrobenchmark module
- avoid relayout when only playback time changes
