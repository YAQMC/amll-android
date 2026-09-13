package dev.yaqmc.amll.viewconsumer

import android.content.Context
import android.widget.FrameLayout
import dev.yaqmc.amll.ui.AMLLPlayerView

class PublishedViewArtifactSmoke(context: Context) : FrameLayout(context) {
    val lyricsView = AMLLPlayerView(context)

    init {
        addView(
            lyricsView,
            LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
            ),
        )
        lyricsView.setLyricLines(emptyList())
        lyricsView.update(positionMs = 0L, isPlaying = false)
    }
}
