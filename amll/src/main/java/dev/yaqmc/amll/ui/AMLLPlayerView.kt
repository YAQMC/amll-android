package dev.yaqmc.amll.ui

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import dev.yaqmc.amll.model.LyricLine
import dev.yaqmc.amll.model.MaskObsceneWordsMode
import dev.yaqmc.amll.state.AMLLPlayerState

/**
 * Android View host for applications that are not themselves Compose-first.
 *
 * The renderer remains [AMLLPlayer]. This wrapper owns the Compose composition and exposes
 * imperative playback and lyric updates so a traditional Activity, Fragment, Capacitor host,
 * or other View hierarchy can mount AMLL without declaring a host-side `@Composable`.
 *
 * Calls that mutate renderer state should be made from the Android main thread, like normal View
 * mutations.
 */
class AMLLPlayerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AbstractComposeView(context, attrs, defStyleAttr) {
    private val playerState = AMLLPlayerState()

    /** Current renderer style. Assigning a new value recomposes the hosted player. */
    var style: AMLLStyle by mutableStateOf(AMLLStyle())

    /** Invoked when the user taps a lyric line. */
    var onLineClick: ((LyricLine) -> Unit)? by mutableStateOf(null)

    /** Exposes the underlying state for advanced integrations while simple hosts stay imperative. */
    val state: AMLLPlayerState
        get() = playerState

    init {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    fun setLyricLines(lines: List<LyricLine>) {
        playerState.setLyricLines(lines)
    }

    fun update(positionMs: Long, isPlaying: Boolean) {
        playerState.update(positionMs = positionMs, isPlaying = isPlaying)
    }

    fun updatePosition(positionMs: Long) {
        playerState.update(positionMs)
    }

    fun updatePlaybackState(isPlaying: Boolean) {
        playerState.updatePlaybackState(isPlaying)
    }

    fun seekTo(positionMs: Long) {
        playerState.seekTo(positionMs)
    }

    fun setAutoSeekDetectionEnabled(enabled: Boolean) {
        playerState.updateAutoSeekDetectionEnabled(enabled)
    }

    fun setMaskObsceneWordsMode(mode: MaskObsceneWordsMode) {
        playerState.updateMaskObsceneWordsMode(mode)
    }

    fun setMaskObsceneWordChar(char: Char) {
        playerState.updateMaskObsceneWordChar(char)
    }

    @Composable
    override fun Content() {
        AMLLPlayer(
            state = playerState,
            modifier = Modifier.fillMaxSize(),
            style = style,
            onLineClick = onLineClick,
        )
    }
}
